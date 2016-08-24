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

import android.text.TextUtils

/**
 * Common tool to check strings, objects etc.
 * Created by mariotaku on 15/11/22.
 */
object CheckUtils {

    fun checkRange(text: CharSequence?, start: Int, end: Int): Boolean {
        if (text == null) return false

        if (end < start) {
            return false
        }

        val len = text.length

        if (start > len || end > len) {
            return false
        }

        if (start < 0 || end < 0) {
            return false
        }
        return true
    }

    fun isValidLocale(locale: String): Boolean {
        return !TextUtils.isEmpty(locale) && "und" != locale
    }
}
