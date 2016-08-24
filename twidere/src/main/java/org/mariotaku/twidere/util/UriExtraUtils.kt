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

import android.net.Uri

import org.mariotaku.twidere.TwidereConstants.QUERY_PARAM_EXTRA

/**
 * Created by mariotaku on 15/10/20.
 */
object UriExtraUtils {

    fun addExtra(builder: Uri.Builder, key: String, value: Any) {
        builder.appendQueryParameter(QUERY_PARAM_EXTRA, key + "=" + value.toString())
    }

    fun getExtra(uri: Uri, key: String): String {
        return getExtra(uri.getQueryParameters(QUERY_PARAM_EXTRA), key)
    }

    fun getExtra(extras: List<String>, key: String): String? {
        for (extra in extras) {
            val prefix = key + "="
            val i = extra.indexOf(prefix)
            if (i == 0) {
                return extra.substring(prefix.length)
            }
        }
        return null
    }

}
