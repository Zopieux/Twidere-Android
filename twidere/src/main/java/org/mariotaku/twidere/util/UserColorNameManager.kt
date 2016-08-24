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
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Color
import android.net.Uri
import android.support.annotation.WorkerThread

import org.mariotaku.microblog.library.twitter.model.User
import org.mariotaku.sqliteqb.library.Expression
import org.mariotaku.twidere.TwidereConstants
import org.mariotaku.twidere.model.ParcelableStatus
import org.mariotaku.twidere.model.ParcelableUser
import org.mariotaku.twidere.model.ParcelableUserList
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.model.util.UserKeyUtils
import org.mariotaku.twidere.provider.TwidereDataStore.Activities
import org.mariotaku.twidere.provider.TwidereDataStore.Statuses

import android.text.TextUtils.isEmpty
import org.mariotaku.twidere.TwidereConstants.USER_COLOR_PREFERENCES_NAME
import org.mariotaku.twidere.TwidereConstants.USER_NICKNAME_PREFERENCES_NAME

class UserColorNameManager(private val mContext: Context) {

    private val mColorPreferences: SharedPreferences
    private val mNicknamePreferences: SharedPreferences

    init {
        mColorPreferences = mContext.getSharedPreferences(USER_COLOR_PREFERENCES_NAME, Context.MODE_PRIVATE)
        mNicknamePreferences = mContext.getSharedPreferences(USER_NICKNAME_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    fun registerColorChangedListener(listener: UserColorChangedListener) {

        mColorPreferences.registerOnSharedPreferenceChangeListener(OnColorPreferenceChangeListener(listener))
    }

    fun registerNicknameChangedListener(listener: UserNicknameChangedListener) {

        mNicknamePreferences.registerOnSharedPreferenceChangeListener(OnNickPreferenceChangeListener(listener))
    }

    fun clearUserColor(userKey: UserKey) {
        val editor = mColorPreferences.edit()
        val userKeyString = userKey.toString()
        updateColor(userKeyString, 0)
        editor.remove(userKeyString)
        editor.apply()
    }

    fun setUserColor(userKey: UserKey, color: Int) {
        val editor = mColorPreferences.edit()
        val userKeyString = userKey.toString()
        updateColor(userKeyString, color)
        editor.putInt(userKeyString, color)
        editor.apply()
    }

    fun setUserNickname(userKey: UserKey, nickname: String) {
        val editor = mNicknamePreferences.edit()
        val userKeyString = userKey.toString()
        updateNickname(userKeyString, null)
        editor.putString(userKeyString, nickname)
        editor.apply()
    }

    fun clearUserNickname(userKey: UserKey) {
        val editor = mNicknamePreferences.edit()
        val userKeyString = userKey.toString()
        updateNickname(userKeyString, null)
        editor.remove(userKeyString)
        editor.apply()
    }

    private fun updateColor(userKey: String, color: Int) {
        val cr = mContext.contentResolver
        val cv = ContentValues()
        updateColumn(cr, Statuses.CONTENT_URI, userKey, Statuses.USER_COLOR, Statuses.USER_KEY,
                color, cv)
        updateColumn(cr, Statuses.CONTENT_URI, userKey, Statuses.QUOTED_USER_COLOR,
                Statuses.QUOTED_USER_KEY, color, cv)
        updateColumn(cr, Statuses.CONTENT_URI, userKey, Statuses.RETWEET_USER_COLOR,
                Statuses.RETWEETED_BY_USER_KEY, color, cv)

        updateColumn(cr, Activities.AboutMe.CONTENT_URI, userKey, Activities.STATUS_USER_COLOR,
                Activities.STATUS_USER_KEY, color, cv)
        updateColumn(cr, Activities.AboutMe.CONTENT_URI, userKey, Activities.STATUS_RETWEET_USER_COLOR,
                Activities.STATUS_RETWEETED_BY_USER_KEY, color, cv)
        updateColumn(cr, Activities.AboutMe.CONTENT_URI, userKey, Activities.STATUS_QUOTED_USER_COLOR,
                Activities.STATUS_QUOTED_USER_KEY, color, cv)
    }

    private fun updateNickname(userKey: String, nickname: String?) {
        val cr = mContext.contentResolver
        val cv = ContentValues()
        updateColumn(cr, Statuses.CONTENT_URI, userKey, Statuses.USER_NICKNAME, Statuses.USER_KEY,
                nickname, cv)
        updateColumn(cr, Statuses.CONTENT_URI, userKey, Statuses.QUOTED_USER_NICKNAME,
                Statuses.QUOTED_USER_KEY, nickname, cv)
        updateColumn(cr, Statuses.CONTENT_URI, userKey, Statuses.RETWEET_USER_NICKNAME,
                Statuses.RETWEETED_BY_USER_KEY, nickname, cv)

        updateColumn(cr, Activities.AboutMe.CONTENT_URI, userKey, Activities.STATUS_USER_NICKNAME,
                Activities.STATUS_USER_KEY, nickname, cv)
        updateColumn(cr, Activities.AboutMe.CONTENT_URI, userKey, Activities.STATUS_RETWEET_USER_NICKNAME,
                Activities.STATUS_RETWEETED_BY_USER_KEY, nickname, cv)
        updateColumn(cr, Activities.AboutMe.CONTENT_URI, userKey, Activities.STATUS_QUOTED_USER_NICKNAME,
                Activities.STATUS_QUOTED_USER_KEY, nickname, cv)
    }

    @WorkerThread
    fun getDisplayName(user: ParcelableUser, nameFirst: Boolean): String {
        return getDisplayName(user.key, user.name, user.screen_name, nameFirst)
    }

    @WorkerThread
    fun getDisplayName(user: User, nameFirst: Boolean): String {
        return getDisplayName(UserKeyUtils.fromUser(user, null), user.name,
                user.screenName, nameFirst)
    }

    @WorkerThread
    fun getDisplayName(user: ParcelableUserList, nameFirst: Boolean): String {
        return getDisplayName(user.user_key, user.user_name, user.user_screen_name, nameFirst)
    }

    @WorkerThread
    fun getDisplayName(status: ParcelableStatus, nameFirst: Boolean): String {
        return getDisplayName(status.user_key, status.user_name, status.user_screen_name, nameFirst)
    }

    @WorkerThread
    fun getDisplayName(userId: UserKey, name: String,
                       screenName: String, nameFirst: Boolean): String {
        return getDisplayName(userId.toString(), name, screenName, nameFirst)
    }

    @WorkerThread
    fun getDisplayName(userId: String, name: String,
                       screenName: String, nameFirst: Boolean): String {
        val nick = getUserNicknameInternal(userId)
        return decideDisplayName(nick, name, screenName, nameFirst)
    }

    @WorkerThread
    fun getUserColor(userId: UserKey): Int {
        return getUserColor(userId.toString())
    }

    @WorkerThread
    fun getUserColor(userId: String): Int {
        return mColorPreferences.getInt(userId, Color.TRANSPARENT)
    }

    @WorkerThread
    fun getUserNickname(userKey: UserKey): String {
        val userKeyString = userKey.toString()
        if (mNicknamePreferences.contains(userKey.id)) {
            val nick = mNicknamePreferences.getString(userKey.id, null)
            val editor = mNicknamePreferences.edit()
            editor.remove(userKey.id)
            editor.putString(userKeyString, nick)
            editor.apply()
            return nick
        }
        return mNicknamePreferences.getString(userKeyString, null)
    }

    @WorkerThread
    fun getUserNickname(userId: UserKey, name: String): String {
        val nick = getUserNickname(userId)
        return decideNickname(nick, name)
    }

    @WorkerThread
    fun getUserNickname(userId: String, name: String): String {
        val nick = getUserNicknameInternal(userId)
        return decideNickname(nick, name)
    }

    val nameEntries: Set<Entry<String, *>>
        get() = mNicknamePreferences.all.entries

    @WorkerThread
    private fun getUserNicknameInternal(userId: String): String {
        return mNicknamePreferences.getString(userId, null)
    }

    interface UserColorChangedListener {
        fun onUserColorChanged(userId: UserKey, color: Int)
    }

    interface UserNicknameChangedListener {
        fun onUserNicknameChanged(userId: UserKey, nick: String)
    }

    private class OnColorPreferenceChangeListener internal constructor(private val mListener: UserColorChangedListener?) : OnSharedPreferenceChangeListener {

        override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String) {
            val userId = UserKey.valueOf(key)
            if (mListener != null && userId != null) {
                mListener.onUserColorChanged(userId, preferences.getInt(key, 0))
            }
        }

    }

    private class OnNickPreferenceChangeListener internal constructor(private val mListener: UserNicknameChangedListener?) : OnSharedPreferenceChangeListener {

        override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String) {
            val userId = UserKey.valueOf(key)
            if (mListener != null && userId != null) {
                mListener.onUserNicknameChanged(userId, preferences.getString(key, null))
            }
        }

    }

    companion object {

        fun decideDisplayName(nickname: String, name: String,
                              screenName: String, nameFirst: Boolean): String {
            if (!isEmpty(nickname)) return nickname
            return if (nameFirst && !isEmpty(name)) name else "@" + screenName
        }

        fun decideNickname(nick: String, name: String): String {
            return if (isEmpty(nick)) name else nick
        }

        private fun updateColumn(cr: ContentResolver, uri: Uri, userKey: String, valueColumn: String,
                                 whereColumn: String, value: Int, temp: ContentValues) {
            temp.clear()
            temp.put(valueColumn, value)
            cr.update(uri, temp, Expression.equalsArgs(whereColumn).sql,
                    arrayOf(userKey))
        }


        private fun updateColumn(cr: ContentResolver, uri: Uri, userKey: String, valueColumn: String,
                                 whereColumn: String, value: String, temp: ContentValues) {
            temp.clear()
            temp.put(valueColumn, value)
            cr.update(uri, temp, Expression.equalsArgs(whereColumn).sql,
                    arrayOf(userKey))
        }
    }
}
