package org.mariotaku.twidere.model.util

import android.content.Context
import android.text.TextUtils
import org.mariotaku.microblog.library.twitter.model.User
import org.mariotaku.twidere.TwidereConstants.USER_TYPE_FANFOU_COM
import org.mariotaku.twidere.TwidereConstants.USER_TYPE_TWITTER_COM
import org.mariotaku.twidere.extension.isFanfouUser
import org.mariotaku.twidere.model.ParcelableUser
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.provider.TwidereDataStore.Accounts
import org.mariotaku.twidere.util.DataStoreUtils
import org.mariotaku.twidere.util.UriUtils
import java.util.*

/**
 * Created by mariotaku on 16/3/7.
 */
object UserKeyUtils {

    fun findById(context: Context, id: String): UserKey? {
        val projection = arrayOf(Accounts.ACCOUNT_KEY)
        val cur = DataStoreUtils.findAccountCursorsById(context, projection, id) ?: return null
        try {
            if (cur.moveToFirst()) return UserKey.valueOf(cur.getString(0))
        } finally {
            cur.close()
        }
        return null
    }

    fun findByIds(context: Context, vararg id: String): Array<UserKey> {
        val projection = arrayOf(Accounts.ACCOUNT_KEY)
        val cur = DataStoreUtils.findAccountCursorsById(context, projection, *id) ?: return emptyArray()
        try {
            val accountKeys = ArrayList<UserKey>()
            cur.moveToFirst()
            while (!cur.isAfterLast) {
                accountKeys.add(UserKey.valueOf(cur.getString(0)))
                cur.moveToNext()
            }
            return accountKeys.toTypedArray()
        } finally {
            cur.close()
        }
    }

    fun fromUser(user: User, userHost: String? = null): UserKey {
        return UserKey(user.id, userHost ?: getUserHost(user))
    }

    fun getUserHost(user: User): String {
        if (user.isFanfouUser) return USER_TYPE_FANFOU_COM
        return getUserHost(user.statusnetProfileUrl, USER_TYPE_TWITTER_COM)
    }

    fun getUserHost(user: ParcelableUser): String {
        if (isFanfouUser(user)) return USER_TYPE_FANFOU_COM
        if (user.extras == null) return USER_TYPE_TWITTER_COM

        return getUserHost(user.extras.statusnet_profile_url, USER_TYPE_TWITTER_COM)
    }


    fun isFanfouUser(user: ParcelableUser): Boolean {
        return user.key.host == USER_TYPE_FANFOU_COM
    }

    fun getUserHost(uri: String?, def: String?): String {
        val fixedDef = def ?: USER_TYPE_TWITTER_COM
        if (uri == null) return fixedDef
        val authority = UriUtils.getAuthority(uri) ?: return fixedDef
        return authority.replace("[^\\w\\d\\.]".toRegex(), "-")
    }

    fun isSameHost(accountKey: UserKey, userKey: UserKey): Boolean {
        return isSameHost(accountKey.host, userKey.host)
    }

    fun isSameHost(a: String?, b: String?): Boolean {
        if (TextUtils.isEmpty(a) || TextUtils.isEmpty(b)) return true
        return TextUtils.equals(a, b)
    }
}
