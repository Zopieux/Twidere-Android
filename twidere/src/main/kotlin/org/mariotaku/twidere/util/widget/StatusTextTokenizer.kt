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

package org.mariotaku.twidere.util.widget

import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.widget.MultiAutoCompleteTextView

/**
 * Created by mariotaku on 15/5/14.
 */
class StatusTextTokenizer : MultiAutoCompleteTextView.Tokenizer {

    override fun findTokenStart(text: CharSequence, cursor: Int): Int {
        // Search backward to find start symbol
        var i = cursor - 1
        val len = text.length
        while (i >= 0 && i < len && !isStartSymbol(text[i])) {
            i--
        }
        if (i < 0) return cursor
        return i
    }

    override fun findTokenEnd(text: CharSequence, cursor: Int): Int {
        var i = cursor - 1
        val len = text.length
        // Search backward to find start symbol
        while (i >= 0 && i < len && isStartSymbol(text[i])) {
            i--
        }
        // Search forward to find space
        while (i < len && !isSpace(text[i])) {
            i++
        }
        if (i < 0) return cursor
        return i
    }

    override fun terminateToken(text: CharSequence): CharSequence {
        // We already have spaces at the end, so just ignore
        if (text is Spanned) {
            val sp = SpannableString("$text ")
            TextUtils.copySpansFrom(text, 0, text.length,
                    Any::class.java, sp, 0)
            return sp
        } else {
            return "$text "
        }
    }

    private fun isSpace(c: Char): Boolean {
        return Character.isSpaceChar(c) || Character.isWhitespace(c)
    }

    private fun isStartSymbol(c: Char): Boolean {
        when (c) {
            '\uff20', '@', '\uff03', '#' -> return true
        }
        return false
    }
}
