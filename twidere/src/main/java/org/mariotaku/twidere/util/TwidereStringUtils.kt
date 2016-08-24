/*
 * Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
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

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Spannable
import android.text.Spanned
import android.text.style.ReplacementSpan

/**
 * Created by mariotaku on 14/12/23.
 */
object TwidereStringUtils {

    fun regionMatchesIgnoreCase(string: String, thisStart: Int,
                                match: String, start: Int,
                                length: Int): Boolean {
        return string.substring(thisStart, thisStart + length).equals(match.substring(start, start + length), ignoreCase = true)
    }

    @JvmOverloads fun startsWithIgnoreCase(string: String, prefix: String,
                                           start: Int = 0): Boolean {
        if (prefix.length > string.length) return false
        return regionMatchesIgnoreCase(string, start, prefix, 0, prefix.length)
    }

    /**
     * Fix to https://github.com/TwidereProject/Twidere-Android/issues/449
     * @param string
     */
    fun fixSHY(string: Spannable) {
        for (i in 0 until string.length) {
            if (string[i] == '\u00ad') {
                string.setSpan(ZeroWidthSpan, i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private object ZeroWidthSpan : ReplacementSpan() {

        override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt): Int {
            return 0
        }

        override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {

        }
    }
}
