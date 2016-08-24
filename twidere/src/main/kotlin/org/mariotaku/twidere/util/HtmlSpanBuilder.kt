/*
 *                 Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.twidere.util

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.URLSpan

import org.apache.commons.lang3.StringUtils
import org.attoparser.ParseException
import org.attoparser.config.ParseConfiguration
import org.attoparser.simple.AbstractSimpleMarkupHandler
import org.attoparser.simple.SimpleMarkupParser

import java.util.ArrayList
import java.util.Locale

/**
 * Created by mariotaku on 15/11/4.
 */
object HtmlSpanBuilder {

    private val PARSER = SimpleMarkupParser(ParseConfiguration.htmlConfiguration())

    @Throws(HtmlParseException::class)
    fun fromHtml(html: String): Spannable {
        val handler = HtmlSpanHandler()
        try {
            PARSER.parse(html, handler)
        } catch (e: ParseException) {
            throw HtmlParseException(e)
        }

        return handler.text
    }

    fun fromHtml(html: String, fallback: CharSequence): CharSequence {
        try {
            return fromHtml(html)
        } catch (e: HtmlParseException) {
            return fallback
        }

    }

    private fun applyTag(sb: SpannableStringBuilder, start: Int, end: Int, info: TagInfo) {
        if (info.name.equals("br", ignoreCase = true)) {
            sb.append('\n')
        } else {
            val span = createSpan(info) ?: return
            sb.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun createSpan(info: TagInfo): Any? {
        when (info.name.toLowerCase(Locale.US)) {
            "a" -> {
                return URLSpan(info.getAttribute("href"))
            }
            "b", "strong" -> {
                return StyleSpan(Typeface.BOLD)
            }
            "em", "cite", "dfn", "i" -> {
                return StyleSpan(Typeface.ITALIC)
            }
        }
        return null
    }

    private fun lastIndexOfTag(info: List<TagInfo>, name: String): Int {
        for (i in info.indices.reversed()) {
            if (StringUtils.equals(info[i].name, name)) {
                return i
            }
        }
        return -1
    }

    class HtmlParseException : RuntimeException {
        constructor() : super() {
        }

        constructor(detailMessage: String) : super(detailMessage) {
        }

        constructor(detailMessage: String, throwable: Throwable) : super(detailMessage, throwable) {
        }

        constructor(throwable: Throwable) : super(throwable) {
        }
    }

    internal class TagInfo(val start: Int, val name: String, val attributes: Map<String, String>) {

        fun getAttribute(attr: String): String {
            return attributes[attr]!!
        }
    }

    internal class HtmlSpanHandler : AbstractSimpleMarkupHandler() {
        private val sb: SpannableStringBuilder
        var tagInfo: MutableList<TagInfo>

        init {
            sb = SpannableStringBuilder()
            tagInfo = ArrayList<TagInfo>()
        }

        override fun handleText(buffer: CharArray?, offset: Int, len: Int, line: Int, col: Int) {
            sb.append(HtmlEscapeHelper.unescape(String(buffer!!, offset, len)))
        }

        override fun handleCloseElement(elementName: String?, line: Int, col: Int) {
            val lastIndex = lastIndexOfTag(tagInfo, elementName)
            if (lastIndex != -1) {
                val info = tagInfo[lastIndex]
                applyTag(sb, info.start, sb.length, info)
                tagInfo.removeAt(lastIndex)
            }
        }

        override fun handleOpenElement(elementName: String?, attributes: Map<String, String>?, line: Int, col: Int) {
            tagInfo.add(TagInfo(sb.length, elementName, attributes))
        }

        val text: Spannable
            get() = sb
    }
}
