/*
 * 				Twidere - Twitter client for Android
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

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri

import org.mariotaku.twidere.provider.TwidereDataStore
import org.mariotaku.twidere.provider.TwidereDataStore.UnreadCounts

object UnreadCountUtils {

    fun getUnreadCount(context: Context?, position: Int): Int {
        if (context == null || position < 0) return 0
        val resolver = context.contentResolver
        val builder = TwidereDataStore.UnreadCounts.CONTENT_URI.buildUpon()
        builder.appendPath(ParseUtils.parseString(position))
        val uri = builder.build()
        val c = resolver.query(uri, arrayOf(UnreadCounts.COUNT), null, null, null) ?: return 0
        try {
            if (c.count == 0) return 0
            c.moveToFirst()
            return c.getInt(c.getColumnIndex(UnreadCounts.COUNT))
        } finally {
            c.close()
        }
    }

    fun getUnreadCount(context: Context?, type: String?): Int {
        if (context == null || type == null) return 0
        val resolver = context.contentResolver
        val builder = TwidereDataStore.UnreadCounts.ByType.CONTENT_URI.buildUpon()
        builder.appendPath(type)
        val uri = builder.build()
        val c = resolver.query(uri, arrayOf(UnreadCounts.COUNT), null, null, null) ?: return 0
        try {
            if (c.count == 0) return 0
            c.moveToFirst()
            return c.getInt(c.getColumnIndex(UnreadCounts.COUNT))
        } finally {
            c.close()
        }
    }
}
