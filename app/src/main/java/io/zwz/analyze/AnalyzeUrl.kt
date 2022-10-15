package io.zwz.analyze

import android.annotation.SuppressLint
import android.text.TextUtils.isEmpty
import com.google.gson.reflect.TypeToken
import io.zwz.analyze.constant.AppConst.JS_ENGINE
import io.zwz.analyze.constant.AppConst.UA_NAME
import io.zwz.analyze.constant.AppConst.userAgent
import io.zwz.analyze.constant.AppPattern.EXP_PATTERN
import io.zwz.analyze.constant.AppPattern.JS_PATTERN
import io.zwz.analyze.help.RequestMethod
import io.zwz.analyze.utils.*
import java.lang.reflect.Type
import java.net.URLEncoder
import java.util.*
import java.util.regex.Pattern
import javax.script.SimpleBindings


/**
 * Created by GKF on 2018/1/24.
 * 搜索URL规则解析
 */
class AnalyzeUrl(
        private var baseUrl: String?,
        private var ruleUrl: String,
        private val key: String?,
        private val page: Int?,
        headerMap: Map<String, String>?) {
    private var requestUrl: String? = null
    private var queryStr: String? = null
    private val queryMap: MutableMap<String, String> = HashMap()
    private val headerMap: MutableMap<String, String> = HashMap()
    private var requestMethod: RequestMethod? = RequestMethod.DEFAULT
    private var urlNoQuery: String = ""
    private lateinit var urlHasQuery: String
    private var type: String? = null
    var useWebView: Boolean = false
        private set
    var host: String = ""
        private set
    var path: String = ""
        private set
    var url: String = ""
        private set
    var charset: String? = null
        private set
    var body: String? = null
        private set
    var postData: ByteArray? = null
        private set

    constructor(baseUrl: String?, urlRule: String) : this(baseUrl, urlRule, null) {}
    constructor(baseUrl: String?, ruleUrl: String, headerMap: Map<String, String>?) : this(baseUrl, ruleUrl, null, headerMap) {}
    constructor(baseUrl: String?, ruleUrl: String, page: Int?, headerMap: Map<String, String>?) : this(baseUrl, ruleUrl, null, page, headerMap) {}

    init {
        if (!isEmpty(baseUrl)) {
            baseUrl = AnalyzeGlobal.PATTERN_HEADER.matcher(baseUrl).replaceAll("")
        }

        //解析Header
        ruleUrl = analyzeHeader(ruleUrl, headerMap)
        //替换关键字
        if (!isEmpty(key)) {
            ruleUrl = ruleUrl.replace("searchKey", key!!)
        }
        //分离编码规则
        ruleUrl = splitCharCode(ruleUrl)

        //判断是否有下一页
        if (page != null && page > 1 && withoutPaging(ruleUrl)) {
            throw Exception("no next page")
        }

        //设置页数
        ruleUrl = replaceKeyPageJs()
        ruleUrl = analyzePage(ruleUrl, page)

        //js
        ruleUrl = analyzeJs()

        initUrl()

        //分离post参数
        var ruleUrlS = ruleUrl.split("@".toRegex()).toTypedArray()
        if (ruleUrlS.size > 1) {
            requestMethod = RequestMethod.POST
        } else {
            //分离get参数
            ruleUrlS = ruleUrlS[0].split("\\?".toRegex()).toTypedArray()
            if (ruleUrlS.size > 1) {
                requestMethod = RequestMethod.GET
            }
        }
        generateUrlPath(ruleUrlS[0])
        if (requestMethod != RequestMethod.DEFAULT) {
            analyzeQuery(ruleUrlS[1].also { queryStr = it })
            postData = generatePostData()
        }
    }

    /**
     * 处理URL
     */
    private fun initUrl() {
        var urlArray = ruleUrl.split(splitUrlRegex, 2)
        url = NetworkUtils.getAbsoluteURL(baseUrl, urlArray[0])
        urlHasQuery = urlArray[0]
        if (urlArray.size > 1) {
            val option = GSON.fromJsonObject<UrlOption>(urlArray[1])
            option?.let { _ ->
                option.method?.let {
                    if (it.equals("POST", true)) requestMethod = RequestMethod.POST
                }
                option.type?.let { type = it }
                option.headers?.let { headers ->
                    if (headers is Map<*, *>) {
                        headers.forEach { entry ->
                            headerMap[entry.key.toString()] = entry.value.toString()
                        }
                    } else if (headers is String) {
                        GSON.fromJsonObject<Map<String, String>>(headers)
                                ?.let { headerMap.putAll(it) }
                    }
                }
                option.charset?.let { charset = it }
                option.body?.let {
                    body = if (it is String) it else GSON.toJson(it)
                }
                option.webView?.let {
                    if (it.toString().isNotEmpty()) {
                        useWebView = true
                    }
                }
                option.js?.let {
                    evalJS(it, page, key, null)
                }
            }
        }
        headerMap[UA_NAME] ?: let {
            headerMap[UA_NAME] = userAgent
        }
        urlNoQuery = url as String
        when (requestMethod) {
            RequestMethod.GET -> {
                if (!useWebView) {
                    urlArray = url!!.split("?")
                    url = urlArray[0]
                    if (urlArray.size > 1) {
                        analyzeFields(urlArray[1])
                    }
                }
            }
            RequestMethod.POST -> {
                body?.let {
                    if (!it.isJson()) {
                        analyzeFields(it)
                    }
                }
            }
        }
    }

    /**
     * 解析QueryMap
     */
    private fun analyzeFields(fieldsTxt: String) {
        queryStr = fieldsTxt
        val queryS = fieldsTxt.splitNotBlank("&")
        for (query in queryS) {
            val queryM = query.splitNotBlank("=")
            val value = if (queryM.size > 1) queryM[1] else ""
            if (isEmpty(charset)) {
                if (NetworkUtils.hasUrlEncoded(value)) {
                    queryMap[queryM[0]] = value
                } else {
                    queryMap[queryM[0]] = URLEncoder.encode(value, "UTF-8")
                }
            } else if (charset == "escape") {
                queryMap[queryM[0]] = EncoderUtils.escape(value)
            } else {
                queryMap[queryM[0]] = URLEncoder.encode(value, charset)
            }
        }
    }

    /**
     * 替换关键字,页数,JS
     */
    private fun replaceKeyPageJs(): String {
        page?.let {
            val matcher = pagePattern.matcher(ruleUrl)
            while (matcher.find()) {
                val pages = matcher.group(1)!!.split(",")
                ruleUrl = if (page <= pages.size) {
                    ruleUrl.replace(matcher.group(), pages[page - 1].trim { it <= ' ' })
                } else {
                    ruleUrl.replace(matcher.group(), pages.last().trim { it <= ' ' })
                }
            }
        }
        //js
        if (ruleUrl.contains("{{") && ruleUrl.contains("}}")) {
            var jsEval: Any
            val sb = StringBuffer()
            val bindings = SimpleBindings()
            bindings["java"] = this
            bindings["baseUrl"] = baseUrl
            bindings["searchPage"] = page
            bindings["page"] = page
            bindings["key"] = key
            bindings["searchKey"] = key
            val expMatcher = EXP_PATTERN.matcher(ruleUrl)
            while (expMatcher.find()) {
                jsEval = expMatcher.group(1)?.let {
                    JS_ENGINE.eval(it, bindings)
                } ?: ""
                if (jsEval is String) {
                    expMatcher.appendReplacement(sb, jsEval)
                } else if (jsEval is Double && jsEval % 1.0 == 0.0) {
                    expMatcher.appendReplacement(sb, String.format("%.0f", jsEval))
                } else {
                    expMatcher.appendReplacement(sb, jsEval.toString())
                }
            }
            expMatcher.appendTail(sb)
            ruleUrl = sb.toString()
        }
        return ruleUrl
    }

    /**
     * 没有分页规则
     */
    private fun withoutPaging(ruleUrl: String): Boolean {
        return (!ruleUrl.contains("searchPage")
                && !AnalyzeGlobal.PATTERN_PAGE.matcher(ruleUrl).find())
    }

    /**
     * 解析Header
     */
    private fun analyzeHeader(ruleUrl: String, headerMapF: Map<String, String>?): String {
        var ruleUrl = ruleUrl
        if (headerMapF != null) {
            headerMap.putAll(headerMapF)
        }
        val matcher = AnalyzeGlobal.PATTERN_HEADER.matcher(ruleUrl)
        if (matcher.find()) {
            var find = matcher.group(0)
            ruleUrl = ruleUrl.replace(find, "")
            find = find.substring(8)
            try {
                headerMap.putAll(GSON.fromJson(find, AnalyzeGlobal.MAP_TYPE))
            } catch (ignore: Exception) {
            }
        }
        return ruleUrl
    }

    /**
     * 解析页数
     */
    @Throws(Exception::class)
    private fun analyzePage(ruleUrl: String, searchPage: Int?): String {
        var ruleUrl = ruleUrl
        if (searchPage == null) return ruleUrl
        val matcher = AnalyzeGlobal.PATTERN_PAGE.matcher(ruleUrl)
        if (matcher.find()) {
            val pages = matcher.group().substring(1, matcher.group().length - 1).split(",".toRegex()).toTypedArray()
            ruleUrl = if (searchPage <= pages.size) {
                ruleUrl.replace(matcher.group(), pages[searchPage - 1].trim { it <= ' ' })
            } else {
                val page = pages[pages.size - 1].trim { it <= ' ' }
                if (withoutPaging(page)) {
                    throw Exception("no next page")
                }
                ruleUrl.replace(matcher.group(), page)
            }
        }
        return ruleUrl.replace("searchPage-1", (searchPage - 1).toString())
                .replace("searchPage+1", (searchPage + 1).toString())
                .replace("searchPage", searchPage.toString())
    }

    /**
     * 替换js
     */
    @SuppressLint("DefaultLocale")
    private fun analyzeJs(): String {
        val ruleList = arrayListOf<String>()
        var start = 0
        var tmp: String
        val jsMatcher = JS_PATTERN.matcher(ruleUrl)
        while (jsMatcher.find()) {
            if (jsMatcher.start() > start) {
                tmp =
                        ruleUrl.substring(start, jsMatcher.start()).replace("\n", "").trim { it <= ' ' }
                if (isEmpty(tmp)) {
                    ruleList.add(tmp)
                }
            }
            ruleList.add(jsMatcher.group())
            start = jsMatcher.end()
        }
        if (ruleUrl.length > start) {
            tmp = ruleUrl.substring(start).replace("\n", "").trim { it <= ' ' }
            if (isEmpty(tmp)) {
                ruleList.add(tmp)
            }
        }
        for (rule in ruleList) {
            var ruleStr = rule
            when {
                ruleStr.startsWith("<js>") -> {
                    ruleStr = ruleStr.substring(4, ruleStr.lastIndexOf("<"))
                    ruleUrl = evalJS(ruleStr, page, key, ruleUrl) as String
                }
                ruleStr.startsWith("@js", true) -> {
                    ruleStr = ruleStr.substring(4)
                    ruleUrl = evalJS(ruleStr, page, key, ruleUrl) as String
                }
                else -> ruleUrl = ruleStr.replace("@result", ruleUrl)
            }
        }
        return ruleUrl
    }

    /**
     * 执行JS
     */
    private fun evalJS(jsStr: String, page: Int?, key: String?, result: Any? = null): Any? {
        val bindings = SimpleBindings()
        bindings["java"] = this
        bindings["page"] = page
        bindings["key"] = key
        bindings["result"] = result
        bindings["baseUrl"] = baseUrl
        return JS_ENGINE.eval(jsStr, bindings)
    }

    /**
     * 解析编码规则
     */
    private fun splitCharCode(rule: String): String {
        val ruleUrlS = rule.split("\\|".toRegex()).toTypedArray()
        if (ruleUrlS.size > 1) {
            if (!isEmpty(ruleUrlS[1])) {
                val qtS = ruleUrlS[1].split("&".toRegex()).toTypedArray()
                for (qt in qtS) {
                    val gz = qt.split("=".toRegex()).toTypedArray()
                    if (gz[0] == "char") {
                        charset = gz[1]
                    }
                }
            }
        }
        return ruleUrlS[0]
    }

    /**
     * QueryMap
     */
    @Throws(Exception::class)
    private fun analyzeQuery(allQuery: String) {
        val queryS = allQuery.split("&".toRegex()).toTypedArray()
        for (query in queryS) {
            val queryM = query.split("=".toRegex()).toTypedArray()
            val value = if (queryM.size > 1) queryM[1] else ""
            if (isEmpty(charset)) {
                if (UrlEncoderUtils.hasUrlEncoded(value)) {
                    queryMap[queryM[0]] = value
                } else {
                    queryMap[queryM[0]] = URLEncoder.encode(value, "UTF-8")
                }
            } else if (charset == "escape") {
                StringUtils.escape(value)?.let {
                    queryMap[queryM[0]] = it;
                }
            } else {
                queryMap[queryM[0]] = URLEncoder.encode(value, charset)
            }
        }
    }

    /**
     * PostData
     */
    private fun generatePostData(): ByteArray? {
        if (queryMap.isNotEmpty()) {
            val builder = StringBuilder()
            val keys: Set<String> = queryMap.keys
            for (key in keys) {
                builder.append(String.format("%s=%s&", key, queryMap[key]))
            }
            builder.deleteCharAt(builder.lastIndexOf("&"))
            return builder.toString().toByteArray()
        }
        return null
    }

    private fun generateUrlPath(ruleUrl: String) {

        url = NetworkUtils.getAbsoluteURL(baseUrl, ruleUrl)
        NetworkUtils.getBaseUrl(url)?.let {
            host = it;
        }
        path = url.substring(host.length)
    }

    fun setRequestUrl(requestUrl: String?) {
        this.requestUrl = requestUrl
    }

    fun getRequestUrl(): String? {
        return if (requestUrl == null) {
            baseUrl
        } else requestUrl
    }

    val queryUrl: String
        get() = if (StringUtils.isBlank(queryStr)) {
            url
        } else String.format("%s?%s", url, queryStr)

    fun getQueryMap(): Map<String, String> {
        return queryMap
    }

    fun getQueryMapStr(): String {
        return GSON.toJson(queryMap)
    }

    fun getHeaderMap(): Map<String, String> {
        return headerMap
    }

    fun getHeaderMapStr(): String {
        return GSON.toJson(headerMap)
    }

    fun getRequestMethod(): RequestMethod {
        return requestMethod ?: RequestMethod.DEFAULT
    }

    override fun toString(): String {
        return "AnalyzeUrl{" +
                "requestUrl='" + requestUrl + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", url='" + url + '\'' +
                ", host='" + host + '\'' +
                ", urlPath='" + path + '\'' +
                ", queryStr='" + queryStr + '\'' +
                ", postData=" + Arrays.toString(postData) +
                ", encoding='" + charset + '\'' +
                ", queryMap=" + queryMap +
                ", headerMap=" + headerMap +
                ", requestMethod=" + requestMethod +
                '}'
    }

    companion object {
        val splitUrlRegex = Regex(",\\s*(?=\\{)")
        private val pagePattern = Pattern.compile("<(.*?)>")
    }

    data class UrlOption(
            val method: String?,
            val charset: String?,
            val webView: Any?,
            val headers: Any?,
            val body: Any?,
            val type: String?,
            val js: String?
    )

    object AnalyzeGlobal {
        val PATTERN_HEADER: Pattern = Pattern.compile("@header:\\{.+?\\}", Pattern.CASE_INSENSITIVE)
        val PATTERN_PAGE: Pattern = Pattern.compile("\\{.*?\\}")
        val PATTERN_SPACE_END: Pattern = Pattern.compile("[\\s|\\u3000]+$")
        val MAP_TYPE: Type = object : TypeToken<Map<String?, String?>?>() {}.type
        const val EMPTY = ""
        const val RULE_REVERSE = "-"
        const val RULE_IN_WHOLE = "@whole:"
        const val RULE_JSON_TRAIT = "$."
        const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.142 Safari/537.36"
    }
}


