package io.zwz.analyze.entities

import io.zwz.analyze.RuleDataInterface
import io.zwz.analyze.utils.splitNotBlank


interface BaseBook : RuleDataInterface {
    var name: String
    var author: String
    var bookUrl: String
    var kind: String?
    var wordCount: String?

    var infoHtml: String?
    var tocHtml: String?

    fun getKindList(): List<String> {
        val kindList = arrayListOf<String>()
        wordCount?.let {
            if (it.isNotBlank()) kindList.add(it)
        }
        kind?.let {
            val kinds = it.splitNotBlank(",", "\n")
            kindList.addAll(kinds)
        }
        return kindList
    }
}