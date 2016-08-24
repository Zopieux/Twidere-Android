package org.mariotaku.ktextension

import java.text.NumberFormat
import java.util.*

/**
 * Created by mariotaku on 16/7/30.
 */

fun String.toLong(def: Long): Long {
    try {
        return toLong()
    } catch (e: NumberFormatException) {
        return def
    }
}

fun Number.toLocalizedString(locale: Locale): String {
    val nf = NumberFormat.getInstance(locale)
    return nf.format(this)
}