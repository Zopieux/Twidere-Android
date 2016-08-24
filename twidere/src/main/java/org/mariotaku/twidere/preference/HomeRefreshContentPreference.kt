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

import android.content.Context
import android.util.AttributeSet

import org.mariotaku.twidere.Constants
import org.mariotaku.twidere.R

import org.mariotaku.twidere.constant.SharedPreferenceConstants.KEY_HOME_REFRESH_DIRECT_MESSAGES
import org.mariotaku.twidere.constant.SharedPreferenceConstants.KEY_HOME_REFRESH_MENTIONS
import org.mariotaku.twidere.constant.SharedPreferenceConstants.KEY_HOME_REFRESH_SAVED_SEARCHES
import org.mariotaku.twidere.constant.SharedPreferenceConstants.KEY_HOME_REFRESH_TRENDS

class HomeRefreshContentPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet = null, defStyle: Int = R.attr.preferenceStyle) : MultiSelectListPreference(context, attrs, defStyle), Constants {

    protected override val defaults: BooleanArray
        get() = booleanArrayOf(true, true, true, true)

    protected override val keys: Array<String>
        get() = arrayOf(KEY_HOME_REFRESH_MENTIONS, KEY_HOME_REFRESH_DIRECT_MESSAGES, KEY_HOME_REFRESH_TRENDS, KEY_HOME_REFRESH_SAVED_SEARCHES)

    protected override val names: Array<String>
        get() = context.resources.getStringArray(R.array.entries_home_refresh_content)

}
