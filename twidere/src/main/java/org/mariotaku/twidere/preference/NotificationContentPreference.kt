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

import org.mariotaku.twidere.constant.SharedPreferenceConstants.DEFAULT_DIRECT_MESSAGES_NOTIFICATION
import org.mariotaku.twidere.constant.SharedPreferenceConstants.DEFAULT_HOME_TIMELINE_NOTIFICATION
import org.mariotaku.twidere.constant.SharedPreferenceConstants.DEFAULT_MENTIONS_NOTIFICATION
import org.mariotaku.twidere.constant.SharedPreferenceConstants.KEY_DIRECT_MESSAGES_NOTIFICATION
import org.mariotaku.twidere.constant.SharedPreferenceConstants.KEY_HOME_TIMELINE_NOTIFICATION
import org.mariotaku.twidere.constant.SharedPreferenceConstants.KEY_MENTIONS_NOTIFICATION

class NotificationContentPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet = null, defStyle: Int = R.attr.preferenceStyle) : MultiSelectListPreference(context, attrs, defStyle), Constants {

    protected override val defaults: BooleanArray
        get() = booleanArrayOf(DEFAULT_HOME_TIMELINE_NOTIFICATION, DEFAULT_MENTIONS_NOTIFICATION, DEFAULT_DIRECT_MESSAGES_NOTIFICATION)

    protected override val keys: Array<String>
        get() = arrayOf(KEY_HOME_TIMELINE_NOTIFICATION, KEY_MENTIONS_NOTIFICATION, KEY_DIRECT_MESSAGES_NOTIFICATION)

    protected override val names: Array<String>
        get() = context.resources.getStringArray(R.array.entries_notification_content)

}
