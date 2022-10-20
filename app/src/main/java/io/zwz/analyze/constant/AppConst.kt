package io.zwz.analyze.constant

import com.script.javascript.RhinoScriptEngine
object AppConst {
    const val DEBUG = true
    const val UA_NAME = "User-Agent"
    const val userAgent =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36"
    val SCRIPT_ENGINE: RhinoScriptEngine by lazy {
        RhinoScriptEngine()
    }

}