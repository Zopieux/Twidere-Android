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
import android.text.TextUtils

import org.mariotaku.twidere.model.ParcelableStatus
import org.mariotaku.twidere.model.ParcelableUser
import org.mariotaku.twidere.model.UserKey

import org.mariotaku.twidere.TwidereConstants.AUTHORITY_STATUS
import org.mariotaku.twidere.TwidereConstants.AUTHORITY_USER
import org.mariotaku.twidere.TwidereConstants.QUERY_PARAM_ACCOUNT_KEY
import org.mariotaku.twidere.TwidereConstants.QUERY_PARAM_SCREEN_NAME
import org.mariotaku.twidere.TwidereConstants.QUERY_PARAM_STATUS_ID
import org.mariotaku.twidere.TwidereConstants.QUERY_PARAM_USER_KEY
import org.mariotaku.twidere.TwidereConstants.SCHEME_HTTP
import org.mariotaku.twidere.TwidereConstants.SCHEME_HTTPS
import org.mariotaku.twidere.TwidereConstants.SCHEME_TWIDERE
import org.mariotaku.twidere.TwidereConstants.USER_TYPE_FANFOU_COM

/**
 * Created by mariotaku on 15/3/14.
 */
object LinkCreator {

    private val AUTHORITY_TWITTER = "twitter.com"
    private val AUTHORITY_FANFOU = "fanfou.com"

    fun getTwidereStatusLink(accountKey: UserKey?, statusId: String): Uri {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_STATUS)
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        }
        builder.appendQueryParameter(QUERY_PARAM_STATUS_ID, statusId)
        return builder.build()
    }

    fun getTwidereUserLink(accountKey: UserKey?, userKey: UserKey?, screenName: String?): Uri {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_USER)
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        }
        if (userKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_USER_KEY, userKey.toString())
        }
        if (screenName != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName)
        }
        return builder.build()
    }

    fun getTwitterUserListLink(userScreenName: String, listName: String): Uri {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_HTTPS)
        builder.authority(AUTHORITY_TWITTER)
        builder.appendPath(userScreenName)
        builder.appendPath(listName)
        return builder.build()
    }

    fun getStatusWebLink(status: ParcelableStatus): Uri {
        if (status.extras != null && !TextUtils.isEmpty(status.extras.external_url)) {
            return Uri.parse(status.extras.external_url)
        }
        if (USER_TYPE_FANFOU_COM == status.account_key.host) {
            return getFanfouStatusLink(status.id)
        }
        return getTwitterStatusLink(status.user_screen_name, status.id)
    }

    fun getQuotedStatusWebLink(status: ParcelableStatus): Uri {
        if (status.extras != null) {
            if (!TextUtils.isEmpty(status.extras.quoted_external_url)) {
                return Uri.parse(status.extras.quoted_external_url)
            } else if (!TextUtils.isEmpty(status.extras.external_url)) {
                return Uri.parse(status.extras.external_url)
            }
        }
        if (USER_TYPE_FANFOU_COM == status.account_key.host) {
            return getFanfouStatusLink(status.quoted_id)
        }
        return getTwitterStatusLink(status.quoted_user_screen_name, status.quoted_id)
    }

    fun getUserWebLink(user: ParcelableUser): Uri {
        if (user.extras != null && user.extras.statusnet_profile_url != null) {
            return Uri.parse(user.extras.statusnet_profile_url)
        }
        if (USER_TYPE_FANFOU_COM == user.key.host) {
            return getFanfouUserLink(user.key.id)
        }
        return getTwitterUserLink(user.screen_name)
    }

    internal fun getTwitterStatusLink(screenName: String, statusId: String): Uri {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_HTTPS)
        builder.authority(AUTHORITY_TWITTER)
        builder.appendPath(screenName)
        builder.appendPath("status")
        builder.appendPath(statusId)
        return builder.build()
    }

    internal fun getTwitterUserLink(screenName: String): Uri {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_HTTPS)
        builder.authority(AUTHORITY_TWITTER)
        builder.appendPath(screenName)
        return builder.build()
    }

    internal fun getFanfouStatusLink(id: String): Uri {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_HTTP)
        builder.authority(AUTHORITY_FANFOU)
        builder.appendPath("statuses")
        builder.appendPath(id)
        return builder.build()
    }

    internal fun getFanfouUserLink(id: String): Uri {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_HTTP)
        builder.authority(AUTHORITY_FANFOU)
        builder.appendPath(id)
        return builder.build()
    }
}
