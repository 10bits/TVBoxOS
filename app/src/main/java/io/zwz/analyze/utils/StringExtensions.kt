package io.zwz.analyze.utils

val removeHtmlRegex = "</?(?:div|p|br|hr|h\\d|article|dd|dl)[^>]*>".toRegex()
val imgRegex = "<img[^>]*>".toRegex()
val notImgHtmlRegex = "</?(?!img)\\w+[^>]*>".toRegex()

fun String?.safeTrim() = if (this.isNullOrBlank()) null else this.trim()

fun String?.isContentPath(): Boolean = this?.startsWith("content://") == true

fun isEmpty(str: CharSequence?): Boolean {
    return str == null || str.isEmpty()
}

fun join(delimiter: CharSequence, tokens: Array<Any?>): String? {
    val length = tokens.size
    if (length == 0) {
        return ""
    }
    val sb = StringBuilder()
    sb.append(tokens[0])
    for (i in 1 until length) {
        sb.append(delimiter)
        sb.append(tokens[i])
    }
    return sb.toString()
}

fun join(delimiter: CharSequence, tokens: Iterable<*>): String? {
    val it = tokens.iterator()
    if (!it.hasNext()) {
        return ""
    }
    val sb = java.lang.StringBuilder()
    sb.append(it.next())
    while (it.hasNext()) {
        sb.append(delimiter)
        sb.append(it.next())
    }
    return sb.toString()
}

fun String?.isAbsUrl() =
    this?.let {
        it.startsWith("http://", true)
                || it.startsWith("https://", true)
    } ?: false

fun String?.isJson(): Boolean =
    this?.run {
        val str = this.trim()
        when {
            str.startsWith("{") && str.endsWith("}") -> true
            str.startsWith("[") && str.endsWith("]") -> true
            else -> false
        }
    } ?: false

fun String?.isJsonObject(): Boolean =
    this?.run {
        val str = this.trim()
        str.startsWith("{") && str.endsWith("}")
    } ?: false

fun String?.isJsonArray(): Boolean =
    this?.run {
        val str = this.trim()
        str.startsWith("[") && str.endsWith("]")
    } ?: false

fun String?.htmlFormat(): String {
    this ?: return ""
    return this
        .replace(imgRegex, "\n$0\n")
        .replace(removeHtmlRegex, "\n")
        .replace(notImgHtmlRegex, "")
        .replace("\\s*\\n+\\s*".toRegex(), "\n　　")
        .replace("^[\\n\\s]+".toRegex(), "　　")
        .replace("[\\n\\s]+$".toRegex(), "")
}

fun String.splitNotBlank(vararg delimiter: String): Array<String> = run {
    this.split(*delimiter).map { it.trim() }.filterNot { it.isBlank() }.toTypedArray()
}

fun String.splitNotBlank(regex: Regex, limit: Int = 0): Array<String> = run {
    this.split(regex, limit).map { it.trim() }.filterNot { it.isBlank() }.toTypedArray()
}

/**
 * 将字符串拆分为单个字符,包含emoji
 */
fun String.toStringArray(): Array<String> {
    var codePointIndex = 0
    return try {
        Array(codePointCount(0, length)) {
            val start = codePointIndex
            codePointIndex = offsetByCodePoints(start, 1)
            substring(start, codePointIndex)
        }
    } catch (e: Exception) {
        split("").toTypedArray()
    }
}

