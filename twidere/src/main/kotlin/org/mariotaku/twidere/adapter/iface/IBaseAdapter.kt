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

package org.mariotaku.twidere.adapter.iface

import android.content.Context
import android.widget.ListAdapter
import org.mariotaku.twidere.Constants

import org.mariotaku.twidere.util.MediaLoaderWrapper
import org.mariotaku.twidere.util.Utils

interface IBaseAdapter : ListAdapter {

    val mediaLoader: MediaLoaderWrapper

    val linkHighlightOption: Int

    var textSize: Float

    var isDisplayNameFirst: Boolean

    var isProfileImageDisplayed: Boolean

    var isShowAccountColor: Boolean

    fun notifyDataSetChanged()

    fun setLinkHighlightOption(option: String)

    fun config(context: Context) {
        val pref = context.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        isProfileImageDisplayed = pref.getBoolean(Constants.KEY_DISPLAY_PROFILE_IMAGE, true)
        isDisplayNameFirst = pref.getBoolean(Constants.KEY_NAME_FIRST, true)
        setLinkHighlightOption(pref.getString(Constants.KEY_LINK_HIGHLIGHT_OPTION, Constants.VALUE_LINK_HIGHLIGHT_OPTION_NONE)!!)
        textSize = pref.getInt(Constants.KEY_TEXT_SIZE, Utils.getDefaultTextSize(context)).toFloat()
        notifyDataSetChanged()
    }
}