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

package org.mariotaku.twidere.loader

import android.content.Context

import org.mariotaku.microblog.library.MicroBlog
import org.mariotaku.microblog.library.MicroBlogException
import org.mariotaku.microblog.library.twitter.model.Paging
import org.mariotaku.microblog.library.twitter.model.User
import org.mariotaku.twidere.model.ParcelableAccount
import org.mariotaku.twidere.model.ParcelableCredentials
import org.mariotaku.twidere.model.ParcelableUser
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.model.util.ParcelableAccountUtils

open class UserSearchLoader(
        context: Context,
        accountKey: UserKey,
        val query: String,
        val page: Int,
        data: List<ParcelableUser>?,
        fromUser: Boolean
) : TwitterAPIUsersLoader(context, accountKey, data, fromUser) {

    @Throws(MicroBlogException::class)
    public override fun getUsers(twitter: MicroBlog, credentials: ParcelableCredentials): List<User> {
        val paging = Paging()
        paging.page(page)
        when (ParcelableAccountUtils.getAccountType(credentials)) {
            ParcelableAccount.Type.FANFOU -> {
                return twitter.searchFanfouUsers(query, paging)
            }
        }
        return twitter.searchUsers(query, paging)
    }

}
