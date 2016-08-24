package org.mariotaku.twidere.task

import android.content.ContentResolver
import android.content.Context

import org.mariotaku.microblog.library.MicroBlog
import org.mariotaku.microblog.library.MicroBlogException
import org.mariotaku.microblog.library.twitter.model.User
import org.mariotaku.sqliteqb.library.Expression
import org.mariotaku.twidere.R
import org.mariotaku.twidere.model.ParcelableAccount
import org.mariotaku.twidere.model.ParcelableCredentials
import org.mariotaku.twidere.model.ParcelableUser
import org.mariotaku.twidere.model.message.FriendshipTaskEvent
import org.mariotaku.twidere.model.util.ParcelableAccountUtils
import org.mariotaku.twidere.provider.TwidereDataStore
import org.mariotaku.twidere.util.Utils

import org.mariotaku.twidere.constant.SharedPreferenceConstants.KEY_NAME_FIRST

/**
 * Created by mariotaku on 16/3/11.
 */
class DestroyFriendshipTask(context: Context) : AbsFriendshipOperationTask(context, FriendshipTaskEvent.Action.UNFOLLOW) {

    @Throws(MicroBlogException::class)
    override fun perform(twitter: MicroBlog, credentials: ParcelableCredentials, args: AbsFriendshipOperationTask.Arguments): User {
        when (ParcelableAccountUtils.getAccountType(credentials)) {
            ParcelableAccount.Type.FANFOU -> {
                return twitter.destroyFanfouFriendship(args.userKey.id)
            }
        }
        return twitter.destroyFriendship(args.userKey.id)
    }

    override fun succeededWorker(twitter: MicroBlog, credentials: ParcelableCredentials, args: AbsFriendshipOperationTask.Arguments, user: ParcelableUser) {
        user.is_following = false
        Utils.setLastSeen(context, user.key, -1)
        val where = Expression.and(Expression.equalsArgs(TwidereDataStore.Statuses.ACCOUNT_KEY),
                Expression.or(Expression.equalsArgs(TwidereDataStore.Statuses.USER_KEY),
                        Expression.equalsArgs(TwidereDataStore.Statuses.RETWEETED_BY_USER_KEY)))
        val whereArgs = arrayOf(args.userKey.toString(), args.userKey.toString(), args.userKey.toString())
        val resolver = context.contentResolver
        resolver.delete(TwidereDataStore.Statuses.CONTENT_URI, where.sql, whereArgs)
    }

    override fun showErrorMessage(params: AbsFriendshipOperationTask.Arguments, exception: Exception?) {
        Utils.showErrorMessage(context, R.string.action_unfollowing, exception, false)
    }

    override fun showSucceededMessage(params: AbsFriendshipOperationTask.Arguments, user: ParcelableUser) {
        val nameFirst = preferences.getBoolean(KEY_NAME_FIRST)
        val message = context.getString(R.string.unfollowed_user,
                manager.getDisplayName(user, nameFirst))
        Utils.showInfoMessage(context, message, false)
    }

}
