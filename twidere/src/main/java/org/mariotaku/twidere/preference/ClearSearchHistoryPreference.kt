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

package org.mariotaku.twidere.preference

import android.content.ContentResolver
import android.content.Context
import android.provider.SearchRecentSuggestions
import android.util.AttributeSet

import org.mariotaku.twidere.R
import org.mariotaku.twidere.provider.RecentSearchProvider
import org.mariotaku.twidere.provider.TwidereDataStore.SearchHistory

class ClearSearchHistoryPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet = null, defStyle: Int = R.attr.preferenceStyle) : AsyncTaskPreference(context, attrs, defStyle) {

    override fun doInBackground() {
        val context = context ?: return
        val suggestions = SearchRecentSuggestions(context,
                RecentSearchProvider.AUTHORITY, RecentSearchProvider.MODE)
        suggestions.clearHistory()
        val cr = context.contentResolver
        cr.delete(SearchHistory.CONTENT_URI, null, null)
    }

}
