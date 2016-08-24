/*
 * Twidere - Twitter client for Android
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

import android.net.Uri

/**
 * Created by mariotaku on 15/3/23.
 */
object UriUtils {

    fun appendQueryParameters(uri: Uri, key: String, value: Long): Uri {
        return appendQueryParameters(uri, key, ParseUtils.parseString(value))
    }

    fun appendQueryParameters(uri: Uri, key: String, value: String): Uri {
        val builder = uri.buildUpon()
        builder.appendQueryParameter(key, value)
        return builder.build()
    }

    fun appendQueryParameters(uri: Uri, key: String, value: Boolean): Uri {
        return appendQueryParameters(uri, key, ParseUtils.parseString(value))
    }

    fun getAuthority(link: String): String? {
        val start = link.indexOf("://")
        if (start < 0) return null
        var end = link.indexOf('/', start + 3)
        if (end < 0) {
            end = link.length
        }
        return link.substring(start + 3, end)
    }


    fun getAuthorityRange(link: String): IntArray? {
        val start = link.indexOf("://")
        if (start < 0) return null
        var end = link.indexOf('/', start + 3)
        if (end < 0) {
            end = link.length
        }
        return intArrayOf(start + 3, end)
    }

    fun getPath(link: String): String? {
        var start = link.indexOf("://")
        if (start < 0) return null
        start = link.indexOf('/', start + 3)
        if (start < 0) {
            return ""
        }
        var end = link.indexOf('?', start)
        if (end < 0) {
            end = link.indexOf('#', start)
            if (end < 0) {
                end = link.length
            }
        }
        return link.substring(start, end)
    }
}
