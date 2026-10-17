package com.huijui.sunext.domain.text

fun interface TextTransliterator {
    fun transliterate(value: String): String
}
