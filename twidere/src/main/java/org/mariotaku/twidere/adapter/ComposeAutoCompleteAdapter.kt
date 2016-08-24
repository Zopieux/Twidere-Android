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

package org.mariotaku.twidere.adapter

import android.content.Context
import android.database.Cursor
import android.graphics.PorterDuff.Mode
import android.support.v4.widget.SimpleCursorAdapter
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import org.apache.commons.lang3.StringUtils
import org.mariotaku.twidere.Constants
import org.mariotaku.twidere.R
import org.mariotaku.twidere.TwidereConstants.*
import org.mariotaku.twidere.constant.SharedPreferenceConstants.KEY_DISPLAY_PROFILE_IMAGE
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.provider.TwidereDataStore.Suggestions
import org.mariotaku.twidere.util.MediaLoaderWrapper
import org.mariotaku.twidere.util.SharedPreferencesWrapper
import org.mariotaku.twidere.util.UserColorNameManager
import org.mariotaku.twidere.util.dagger.GeneralComponentHelper
import javax.inject.Inject

class ComposeAutoCompleteAdapter(context: Context) : SimpleCursorAdapter(context, R.layout.list_item_auto_complete, null, ComposeAutoCompleteAdapter.FROM, ComposeAutoCompleteAdapter.TO, 0), Constants {

    @Inject
    lateinit var mProfileImageLoader: MediaLoaderWrapper
    @Inject
    lateinit var mPreferences: SharedPreferencesWrapper
    @Inject
    lateinit var mUserColorNameManager: UserColorNameManager

    private val mDisplayProfileImage: Boolean

    private var mTypeIdx: Int = 0
    private var mIconIdx: Int = 0
    private var mTitleIdx: Int = 0
    private var mSummaryIdx: Int = 0
    private var mExtraIdIdx: Int = 0
    private var mValueIdx: Int = 0
    private var mAccountKey: UserKey? = null
    private var mToken: Char = ' '

    init {
        GeneralComponentHelper.build(context).inject(this)
        mDisplayProfileImage = mPreferences.getBoolean(KEY_DISPLAY_PROFILE_IMAGE, true)
    }

    override fun bindView(view: View, context: Context?, cursor: Cursor) {
        val text1 = view.findViewById(android.R.id.text1) as TextView
        val text2 = view.findViewById(android.R.id.text2) as TextView
        val icon = view.findViewById(android.R.id.icon) as ImageView

        // Clear images in order to prevent images in recycled view shown.
        icon.setImageDrawable(null)

        if (Suggestions.AutoComplete.TYPE_USERS == cursor.getString(mTypeIdx)) {
            text1.text = mUserColorNameManager.getUserNickname(cursor.getString(mExtraIdIdx),
                    cursor.getString(mTitleIdx))
            text2.text = String.format("@%s", cursor.getString(mSummaryIdx))
            if (mDisplayProfileImage) {
                val profileImageUrl = cursor.getString(mIconIdx)
                mProfileImageLoader.displayProfileImage(icon, profileImageUrl)
            } else {
                mProfileImageLoader.cancelDisplayTask(icon)
            }

            icon.clearColorFilter()
        } else {
            text1.text = String.format("#%s", cursor.getString(mTitleIdx))
            text2.setText(R.string.hashtag)

            icon.setImageResource(R.drawable.ic_action_hashtag)
            icon.setColorFilter(text1.currentTextColor, Mode.SRC_ATOP)
        }
        icon.visibility = if (mDisplayProfileImage) View.VISIBLE else View.GONE
        super.bindView(view, context, cursor)
    }

    fun closeCursor() {
        val cursor = swapCursor(null) ?: return
        if (!cursor.isClosed) {
            cursor.close()
        }
    }

    override fun convertToString(cursor: Cursor?): CharSequence {
        when (StringUtils.defaultIfEmpty(cursor!!.getString(mTypeIdx), "")) {
            Suggestions.AutoComplete.TYPE_HASHTAGS -> {
                return '#' + cursor.getString(mValueIdx)
            }
            Suggestions.AutoComplete.TYPE_USERS -> {
                return '@' + cursor.getString(mValueIdx)
            }
        }
        return cursor.getString(mValueIdx)
    }

    override fun runQueryOnBackgroundThread(constraint: CharSequence): Cursor? {
        if (TextUtils.isEmpty(constraint)) return null
        val token = constraint[0]
        if (getNormalizedSymbol(token) == getNormalizedSymbol(mToken)) {
            val filter = filterQueryProvider
            if (filter != null) return filter.runQuery(constraint)
        }
        mToken = token
        val builder = Suggestions.AutoComplete.CONTENT_URI.buildUpon()
        builder.appendQueryParameter(QUERY_PARAM_QUERY, constraint.subSequence(1, constraint.length).toString())
        when (getNormalizedSymbol(token)) {
            '#' -> {
                builder.appendQueryParameter(QUERY_PARAM_TYPE, Suggestions.AutoComplete.TYPE_HASHTAGS)
            }
            '@' -> {
                builder.appendQueryParameter(QUERY_PARAM_TYPE, Suggestions.AutoComplete.TYPE_USERS)
            }
            else -> {
                return null
            }
        }
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, mAccountKey.toString())
        return mContext.contentResolver.query(builder.build(), Suggestions.AutoComplete.COLUMNS,
                null, null, null)
    }


    fun setAccountKey(accountKey: UserKey) {
        mAccountKey = accountKey
    }

    override fun swapCursor(cursor: Cursor?): Cursor? {
        if (cursor != null) {
            mTypeIdx = cursor.getColumnIndex(Suggestions.AutoComplete.TYPE)
            mTitleIdx = cursor.getColumnIndex(Suggestions.AutoComplete.TITLE)
            mSummaryIdx = cursor.getColumnIndex(Suggestions.AutoComplete.SUMMARY)
            mExtraIdIdx = cursor.getColumnIndex(Suggestions.AutoComplete.EXTRA_ID)
            mIconIdx = cursor.getColumnIndex(Suggestions.AutoComplete.ICON)
            mValueIdx = cursor.getColumnIndex(Suggestions.AutoComplete.VALUE)
        }
        return super.swapCursor(cursor)
    }

    companion object {

        private val FROM = arrayOfNulls<String>(0)
        private val TO = IntArray(0)


        private fun getNormalizedSymbol(character: Char): Char {
            when (character) {
                '\uff20', '@' -> return '@'
                '\uff03', '#' -> return '#'
            }
            return '\u0000'
        }
    }

}
