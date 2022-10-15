package io.zwz.analyze.help

import android.annotation.SuppressLint
import android.content.Context
import android.net.http.SslError
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.webkit.*
import java.lang.ref.WeakReference
import java.util.*

class AjaxWebView {
    private class AjaxHandler(private val mCallback: AjaxWebView.Callback) : Handler(Looper.getMainLooper()) {
        private var mWebView: WebView? = null
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_AJAX_START -> {
                    val params = msg.obj as AjaxParams
                    mWebView = createAjaxWebView(params, this)
                }
                MSG_SNIFF_START -> {
                    val params = msg.obj as AjaxParams
                    mWebView = createAjaxWebView(params, this)
                }
                MSG_SUCCESS -> {
                    mCallback.onResult(msg.obj as String)
                    destroyWebView()
                }
                MSG_ERROR -> {
                    mCallback.onError(msg.obj as Throwable)
                    destroyWebView()
                }
            }
        }

        private fun destroyWebView() {
            if (mWebView != null) {
                mWebView!!.clearCache(true)
                mWebView!!.destroy()
                mWebView = null
            }
        }

        companion object {
            const val MSG_AJAX_START = 0
            const val MSG_SNIFF_START = 1
            const val MSG_SUCCESS = 2
            const val MSG_ERROR = 3
        }
    }

    fun ajax(params: AjaxParams?, callback: Callback) {
        AjaxHandler(callback).obtainMessage(AjaxHandler.MSG_AJAX_START, params)
                .sendToTarget()
    }

    fun sniff(params: AjaxParams?, callback: Callback) {
        AjaxHandler(callback).obtainMessage(AjaxHandler.MSG_SNIFF_START, params)
                .sendToTarget()
    }

    private class JavaInjectMethod internal constructor(private val handler: Handler) {
        @JavascriptInterface
        fun processHTML(html: String?) {
            handler.obtainMessage(AjaxHandler.MSG_SUCCESS, html)
                    .sendToTarget()
        }
    }

    class AjaxParams(val context: Context) {
        private var requestMethod: RequestMethod? = null
        var url: String? = null
        var postData: ByteArray? = null
        var headerMap: Map<String, String>? = null
        private var audioSuffix: String? = null
        var javaScript: String? = null
        var audioSuffixList: List<String>? = null
            get() {
                if (field == null) {
                    field = if (isSniff) {
                        val suffixArray = audioSuffix!!.split("\\|\\|".toRegex()).toTypedArray()
                        suffixArray.asList()
                    } else {
                        emptyList()
                    }
                }
                return field
            }

        fun requestMethod(method: RequestMethod): AjaxParams {
            requestMethod = method
            return this
        }

        fun url(url: String?): AjaxParams {
            this.url = url
            return this
        }

        fun postData(postData: ByteArray): AjaxParams {
            this.postData = postData
            return this
        }

        fun headerMap(headerMap: Map<String, String>?): AjaxParams {
            this.headerMap = headerMap
            return this
        }

        fun suffix(suffix: String?): AjaxParams {
            audioSuffix = suffix
            return this
        }

        fun javaScript(javaScript: String?): AjaxParams {
            this.javaScript = javaScript
            return this
        }

        fun getRequestMethod(): RequestMethod {
            return if (requestMethod == null) RequestMethod.DEFAULT else requestMethod!!
        }

        val userAgent: String?
            get() = if (headerMap != null) {
                headerMap!!["User-Agent"]
            } else null
        val isSniff: Boolean
            get() = !TextUtils.isEmpty(audioSuffix)

        fun hasJavaScript(): Boolean {
            return !TextUtils.isEmpty(javaScript)
        }

        fun clearJavaScript() {
            javaScript = null
        }
    }

    private class HtmlWebViewClient(private val handler: Handler) : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            evaluateJavascript(view)
        }

        override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                handler.obtainMessage(AjaxHandler.MSG_ERROR, Exception(description))
                        .sendToTarget()
            }
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                handler.obtainMessage(AjaxHandler.MSG_ERROR, Exception(error.description.toString()))
                        .sendToTarget()
            }
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            handler.proceed()
        }

        private fun evaluateJavascript(webView: WebView) {
            val runnable = ScriptRunnable(webView, OUTER_HTML)
            handler.postDelayed(runnable, 1000L)
        }

        companion object {
            private const val OUTER_HTML = "window.OUTHTML.processHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');"
        }
    }

    private class SnifferWebClient(private val params: AjaxParams, private val handler: Handler) : WebViewClient() {
        override fun onLoadResource(view: WebView, url: String) {
            val suffixList: List<String>? = params.audioSuffixList
            if (suffixList != null) {
                for (suffix in suffixList) {
                    if (!TextUtils.isEmpty(suffix) && url.contains(suffix)) {
                        handler.obtainMessage(AjaxHandler.MSG_SUCCESS, url)
                                .sendToTarget()
                        return
                    }
                }
            }
            for (suffix in DEFAULT_AUDIO_SUFFIXES) {
                if (url.endsWith(suffix)) {
                    handler.obtainMessage(AjaxHandler.MSG_SUCCESS, url)
                            .sendToTarget()
                    break
                }
            }
        }

        override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                handler.obtainMessage(AjaxHandler.MSG_ERROR, Exception(description))
                        .sendToTarget()
            }
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                handler.obtainMessage(AjaxHandler.MSG_ERROR, Exception(error.description.toString()))
                        .sendToTarget()
            }
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            handler.proceed()
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (params.hasJavaScript()) {
                evaluateJavascript(view, params.javaScript)
                params.clearJavaScript()
            }
        }

        private fun evaluateJavascript(webView: WebView, javaScript: String?) {
            val runnable = ScriptRunnable(webView, javaScript)
            handler.postDelayed(runnable, 1000L)
        }
    }

    private class ScriptRunnable(webView: WebView, javaScript: String?) : Runnable {
        private val mJavaScript: String? = javaScript
        private val mWebView: WeakReference<WebView> = WeakReference(webView)
        override fun run() {
            val webView = mWebView.get()
            webView?.loadUrl("javascript:$mJavaScript")
        }

    }

    abstract class Callback {
        abstract fun onResult(result: String?)
        abstract fun onError(error: Throwable?)
    }

    companion object {
        var DEFAULT_AUDIO_SUFFIXES = arrayOf(".mp3", ".m4a", ".wav", ".wma", ".vqf")

        @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
        private fun createAjaxWebView(params: AjaxParams, handler: Handler): WebView {
            val webView = WebView(params.context.applicationContext)
            val settings = webView.settings
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.blockNetworkImage = true
            settings.userAgentString = params.userAgent
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                settings.mediaPlaybackRequiresUserGesture = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
            if (params.isSniff) {
                webView.webViewClient = SnifferWebClient(params, handler)
            } else {
                webView.webViewClient = HtmlWebViewClient(handler)
                webView.addJavascriptInterface(JavaInjectMethod(handler), "OUTHTML")
            }
            when (params.getRequestMethod()) {
                RequestMethod.POST -> params.postData?.let { webView.postUrl(params.url!!, it) }
                RequestMethod.GET, RequestMethod.DEFAULT -> webView.loadUrl(params.url!!, params.headerMap!!)
            }
            return webView
        }
    }
}