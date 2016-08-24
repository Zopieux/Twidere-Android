package org.mariotaku.twidere.model.util

import android.text.TextUtils
import org.mariotaku.microblog.library.twitter.model.UserList
import org.mariotaku.twidere.model.ParcelableUserList
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.util.TwitterContentUtils

/**
 * Created by mariotaku on 16/3/5.
 */
object ParcelableUserListUtils {

    @JvmOverloads fun from(list: UserList, accountKey: UserKey, position: Long = 0, isFollowing: Boolean = false): ParcelableUserList {
        val obj = ParcelableUserList()
        val user = list.user
        obj.position = position
        obj.account_key = accountKey
        obj.id = list.id
        obj.is_public = UserList.Mode.PUBLIC == list.mode
        obj.is_following = isFollowing
        obj.name = list.name
        obj.description = list.description
        obj.user_key = UserKeyUtils.fromUser(user, null)
        obj.user_name = user.name
        obj.user_screen_name = user.screenName
        obj.user_profile_image_url = TwitterContentUtils.getProfileImageUrl(user)
        obj.members_count = list.memberCount
        obj.subscribers_count = list.subscriberCount
        return obj
    }

    fun fromUserLists(userLists: Array<UserList>?, accountKey: UserKey): Array<ParcelableUserList>? {
        if (userLists == null) return null
        return Array(userLists.size) {
            from(userLists[it], accountKey)
        }
    }

    fun check(userList: ParcelableUserList, accountKey: UserKey, listId: String?,
              userKey: UserKey?, screenName: String?, listName: String?): Boolean {
        if (userList.account_key != accountKey) return false
        if (listId != null) {
            return TextUtils.equals(listId, userList.id)
        } else if (listName != null) {
            if (!TextUtils.equals(listName, userList.name)) return false
            if (userKey != null) {
                return userKey == userList.user_key
            } else if (screenName != null) {
                return TextUtils.equals(screenName, userList.user_screen_name)
            }
        }
        return false
    }
}
