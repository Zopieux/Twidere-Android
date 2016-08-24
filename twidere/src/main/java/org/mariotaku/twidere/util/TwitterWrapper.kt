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

import android.content.Context
import android.net.Uri
import android.support.v4.util.SimpleArrayMap
import android.text.TextUtils
import android.util.Log
import android.webkit.MimeTypeMap
import org.mariotaku.microblog.library.MicroBlog
import org.mariotaku.microblog.library.MicroBlogException
import org.mariotaku.microblog.library.twitter.model.DirectMessage
import org.mariotaku.microblog.library.twitter.model.Paging
import org.mariotaku.microblog.library.twitter.model.Status
import org.mariotaku.microblog.library.twitter.model.User
import org.mariotaku.restfu.http.ContentType
import org.mariotaku.restfu.http.mime.FileBody
import org.mariotaku.twidere.TwidereConstants.LOGTAG
import org.mariotaku.twidere.model.ListResponse
import org.mariotaku.twidere.model.ParcelableAccount
import org.mariotaku.twidere.model.SingleResponse
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.provider.TwidereDataStore.Notifications
import org.mariotaku.twidere.provider.TwidereDataStore.UnreadCounts
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

open class TwitterWrapper {


    class MessageListResponse internal constructor(accountKey: UserKey, maxId: String?, sinceId: String?,
                                                   list: List<DirectMessage>?, exception: Exception?) : TwitterListResponse<DirectMessage>(accountKey, maxId, sinceId, list, exception) {

        constructor(accountKey: UserKey, exception: Exception) : this(accountKey, null, null, null, exception) {
        }

        constructor(accountKey: UserKey, list: List<DirectMessage>) : this(accountKey, null, null, list, null) {
        }

        constructor(accountKey: UserKey, maxId: String, sinceId: String,
                    list: List<DirectMessage>) : this(accountKey, maxId, sinceId, list, null) {
        }

    }

    class StatusListResponse internal constructor(accountKey: UserKey, maxId: String?, sinceId: String?, list: List<Status>?,
                                                  val truncated: Boolean, exception: Exception?) : TwitterListResponse<Status>(accountKey, maxId, sinceId, list, exception) {

        constructor(accountKey: UserKey, exception: Exception) : this(accountKey, null, null, null, false, exception) {
        }

        constructor(accountKey: UserKey, list: List<Status>) : this(accountKey, null, null, list, false, null) {
        }

        constructor(accountKey: UserKey, maxId: String, sinceId: String,
                    list: List<Status>, truncated: Boolean) : this(accountKey, maxId, sinceId, list, truncated, null) {
        }

    }

    open class TwitterListResponse<Data> internal constructor(val accountKey: UserKey, val maxId: String?, val sinceId: String?,
                                                              list: List<Data>?, exception: Exception?) : ListResponse<Data>(list, exception) {

        constructor(accountKey: UserKey,
                    exception: Exception) : this(accountKey, null, null, null, exception) {
        }

        constructor(accountKey: UserKey, maxId: String,
                    sinceId: String, list: List<Data>) : this(accountKey, maxId, sinceId, list, null) {
        }

    }

    companion object {

        fun clearNotification(context: Context, notificationType: Int, accountKey: UserKey?): Int {
            val builder = Notifications.CONTENT_URI.buildUpon()
            builder.appendPath(notificationType.toString())
            if (accountKey != null) {
                builder.appendPath(accountKey.toString())
            }
            return context.contentResolver.delete(builder.build(), null, null)
        }

        fun clearUnreadCount(context: Context?, position: Int): Int {
            if (context == null || position < 0) return 0
            val uri = UnreadCounts.CONTENT_URI.buildUpon().appendPath(position.toString()).build()
            return context.contentResolver.delete(uri, null, null)
        }

        fun deleteProfileBannerImage(context: Context,
                                     accountKey: UserKey): SingleResponse<Boolean> {
            val twitter = MicroBlogAPIFactory.getInstance(context, accountKey, false) ?: return SingleResponse.getInstance(false)
            try {
                twitter.removeProfileBannerImage()
                return SingleResponse.getInstance(true)
            } catch (e: MicroBlogException) {
                return SingleResponse.getInstance(false, e)
            }

        }

        fun removeUnreadCounts(context: Context?, position: Int, account_id: Long,
                               vararg statusIds: Long): Int {
            if (context == null || position < 0 || statusIds.size == 0)
                return 0
            var result = 0
            val builder = UnreadCounts.CONTENT_URI.buildUpon()
            builder.appendPath(position.toString())
            builder.appendPath(account_id.toString())
            builder.appendPath(statusIds.joinToString(","))
            result += context.contentResolver.delete(builder.build(), null, null)
            return result
        }

        fun removeUnreadCounts(context: Context?, position: Int,
                               counts: SimpleArrayMap<UserKey, out Set<String>>?): Int {
            if (context == null || position < 0 || counts == null) return 0
            var result = 0
            var i = 0
            val j = counts.size()
            while (i < j) {
                val key = counts.keyAt(i)
                val value = counts.valueAt(i)
                val builder = UnreadCounts.CONTENT_URI.buildUpon()
                builder.appendPath(position.toString())
                builder.appendPath(key.toString())
                builder.appendPath(value.joinToString(","))
                result += context.contentResolver.delete(builder.build(), null, null)
                i++
            }
            return result
        }

        @Throws(MicroBlogException::class)
        fun showUser(twitter: MicroBlog, id: String?, screenName: String?,
                     accountType: String?): User {
            if (id != null) {
                if (ParcelableAccount.Type.FANFOU == accountType) {
                    return twitter.showFanfouUser(id)
                }
                return twitter.showUser(id)
            } else if (screenName != null) {
                if (ParcelableAccount.Type.FANFOU == accountType) {
                    return twitter.showFanfouUser(screenName)
                }
                return twitter.showUserByScreenName(screenName)
            }
            throw MicroBlogException("Invalid user id or screen name")
        }

        @Throws(MicroBlogException::class)
        fun showUserAlternative(twitter: MicroBlog, id: String?,
                                screenName: String?): User {
            val searchScreenName: String
            if (screenName != null) {
                searchScreenName = screenName
            } else if (id != null) {
                searchScreenName = twitter.showFriendship(id).targetUserScreenName
            } else
                throw IllegalArgumentException()
            val paging = Paging()
            paging.count(1)
            for (user in twitter.searchUsers(searchScreenName, paging)) {
                if (TextUtils.equals(user.id, id) || searchScreenName.equals(user.screenName, ignoreCase = true))
                    return user
            }
            if (id != null) {
                val timeline = twitter.getUserTimeline(id, paging)
                for (status in timeline) {
                    val user = status.user
                    if (TextUtils.equals(user.id, id)) return user
                }
            } else {
                val timeline = twitter.getUserTimelineByScreenName(screenName, paging)
                for (status in timeline) {
                    val user = status.user
                    if (searchScreenName.equals(user.screenName, ignoreCase = true))
                        return user
                }
            }
            throw MicroBlogException("can't find user")
        }

        @Throws(MicroBlogException::class)
        fun tryShowUser(twitter: MicroBlog, id: String?, screenName: String?,
                        accountType: String?): User {
            try {
                return showUser(twitter, id, screenName, accountType)
            } catch (e: MicroBlogException) {
                // Twitter specific error for private API calling through proxy
                if (e.statusCode == 200) {
                    return showUserAlternative(twitter, id, screenName)
                }
                throw e
            }

        }

        @Throws(IOException::class, MicroBlogException::class)
        fun updateProfileBannerImage(context: Context, twitter: MicroBlog,
                                     imageUri: Uri, deleteImage: Boolean) {
            var fileBody: FileBody? = null
            try {
                fileBody = getFileBody(context, imageUri)
                twitter.updateProfileBannerImage(fileBody)
            } finally {
                Utils.closeSilently(fileBody)
                if (deleteImage && "file" == imageUri.scheme) {
                    val file = File(imageUri.path)
                    if (!file.delete()) {
                        Log.w(LOGTAG, String.format("Unable to delete %s", file))
                    }
                }
            }
        }

        @Throws(IOException::class, MicroBlogException::class)
        fun updateProfileBackgroundImage(context: Context,
                                         twitter: MicroBlog,
                                         imageUri: Uri,
                                         tile: Boolean,
                                         deleteImage: Boolean) {
            var fileBody: FileBody? = null
            try {
                fileBody = getFileBody(context, imageUri)
                twitter.updateProfileBackgroundImage(fileBody, tile)
            } finally {
                Utils.closeSilently(fileBody)
                if (deleteImage && "file" == imageUri.scheme) {
                    val file = File(imageUri.path)
                    if (!file.delete()) {
                        Log.w(LOGTAG, String.format("Unable to delete %s", file))
                    }
                }
            }
        }

        @Throws(IOException::class, MicroBlogException::class)
        fun updateProfileImage(context: Context, twitter: MicroBlog,
                               imageUri: Uri, deleteImage: Boolean): User {
            var fileBody: FileBody? = null
            try {
                fileBody = getFileBody(context, imageUri)
                return twitter.updateProfileImage(fileBody)
            } finally {
                Utils.closeSilently(fileBody)
                if (deleteImage && "file" == imageUri.scheme) {
                    val file = File(imageUri.path)
                    if (!file.delete()) {
                        Log.w(LOGTAG, String.format("Unable to delete %s", file))
                    }
                }
            }
        }

        @Throws(IOException::class)
        private fun getFileBody(context: Context, imageUri: Uri): FileBody {
            val cr = context.contentResolver
            var type = cr.getType(imageUri)
            if (type == null) {
                type = Utils.getImageMimeType(cr, imageUri)
            }
            val contentType: ContentType?
            val extension: String?
            if (type != null) {
                contentType = ContentType.parse(type)
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
            } else {
                contentType = null
                extension = null
            }
            val `is` = cr.openInputStream(imageUri) ?: throw FileNotFoundException(imageUri.toString())

            val fileName: String
            if (extension != null) {
                fileName = "image." + extension
            } else {
                fileName = "image"
            }
            return FileBody(`is`, fileName, `is`.available().toLong(), contentType)
        }
    }
}
