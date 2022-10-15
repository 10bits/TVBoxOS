package io.zwz.analyze.constant

import javax.script.ScriptEngine
import javax.script.ScriptEngineManager


object AppConst {
    const val DEBUG = true
    const val UA_NAME = "User-Agent"
    const val userAgent =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36"

    val JS_ENGINE: ScriptEngine by lazy {
        ScriptEngineManager().getEngineByName("rhino")
    }
}