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
import org.mariotaku.microblog.library.twitter.model.PageableResponseList
import org.mariotaku.microblog.library.twitter.model.Paging
import org.mariotaku.microblog.library.twitter.model.UserList
import org.mariotaku.twidere.model.ParcelableUserList
import org.mariotaku.twidere.model.UserKey

class UserListMembershipsLoader(
        context: Context,
        accountKey: UserKey,
        private val userKey: UserKey?,
        private val screenName: String?,
        cursor: Long,
        data: List<ParcelableUserList>?
) : BaseUserListsLoader(context, accountKey, cursor, data) {

    @Throws(MicroBlogException::class)
    override fun getUserLists(twitter: MicroBlog): PageableResponseList<UserList> {
        val paging = Paging()
        paging.cursor(cursor)
        if (userKey != null) {
            return twitter.getUserListMemberships(userKey.id, paging)
        } else if (screenName != null) {
            return twitter.getUserListMembershipsByScreenName(screenName, paging)
        }
        throw MicroBlogException()
    }

}
