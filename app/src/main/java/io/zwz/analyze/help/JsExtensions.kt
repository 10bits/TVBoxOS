package io.zwz.analyze.help

import io.zwz.analyze.AnalyzeUrl
import io.zwz.analyze.SimpleModel
import io.zwz.analyze.utils.NetworkUtils

interface JsExtensions {
    fun ajax(urlStr: String): String {
        val result = try {
            val analyzeUrl = AnalyzeUrl(NetworkUtils.getBaseUrl(urlStr), urlStr!!)
            SimpleModel.getResponse(analyzeUrl).blockingFirst().body()
        } catch (e: Exception) {
            e.localizedMessage
        }
        if (result != null) {
            return result.toString()
        }
        return ""
    }
}