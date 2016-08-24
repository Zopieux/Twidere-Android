package org.mariotaku.twidere.task

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri

import org.mariotaku.microblog.library.MicroBlog
import org.mariotaku.microblog.library.MicroBlogException
import org.mariotaku.microblog.library.twitter.model.User
import org.mariotaku.sqliteqb.library.Expression
import org.mariotaku.twidere.R
import org.mariotaku.twidere.model.ParcelableCredentials
import org.mariotaku.twidere.model.ParcelableUser
import org.mariotaku.twidere.model.message.FriendshipTaskEvent
import org.mariotaku.twidere.provider.TwidereDataStore
import org.mariotaku.twidere.provider.TwidereDataStore.Activities
import org.mariotaku.twidere.provider.TwidereDataStore.CachedRelationships
import org.mariotaku.twidere.provider.TwidereDataStore.Statuses
import org.mariotaku.twidere.util.Utils

import org.mariotaku.twidere.constant.SharedPreferenceConstants.KEY_NAME_FIRST

/**
 * Created by mariotaku on 16/3/11.
 */
class CreateUserMuteTask(context: Context) : AbsFriendshipOperationTask(context, FriendshipTaskEvent.Action.MUTE) {

    @Throws(MicroBlogException::class)
    override fun perform(twitter: MicroBlog, credentials: ParcelableCredentials,
                         args: AbsFriendshipOperationTask.Arguments): User {
        return twitter.createMute(args.userKey.id)
    }

    override fun succeededWorker(twitter: MicroBlog,
                                 credentials: ParcelableCredentials,
                                 args: AbsFriendshipOperationTask.Arguments, user: ParcelableUser) {
        val resolver = context.contentResolver
        Utils.setLastSeen(context, args.userKey, -1)
        for (uri in TwidereDataStore.STATUSES_URIS) {
            val where = Expression.and(
                    Expression.equalsArgs(Statuses.ACCOUNT_KEY),
                    Expression.equalsArgs(Statuses.USER_KEY))
            val whereArgs = arrayOf(args.accountKey.toString(), args.userKey.toString())
            resolver.delete(uri, where.sql, whereArgs)
        }
        if (!user.is_following) {
            for (uri in TwidereDataStore.ACTIVITIES_URIS) {
                val where = Expression.and(
                        Expression.equalsArgs(Activities.ACCOUNT_KEY),
                        Expression.equalsArgs(Activities.STATUS_USER_KEY))
                val whereArgs = arrayOf(args.accountKey.toString(), args.userKey.toString())
                resolver.delete(uri, where.sql, whereArgs)
            }
        }
        // I bet you don't want to see this user in your auto complete list.
        val values = ContentValues()
        values.put(CachedRelationships.ACCOUNT_KEY, args.accountKey.toString())
        values.put(CachedRelationships.USER_KEY, args.userKey.toString())
        values.put(CachedRelationships.MUTING, true)
        resolver.insert(CachedRelationships.CONTENT_URI, values)
    }

    override fun showSucceededMessage(params: AbsFriendshipOperationTask.Arguments, user: ParcelableUser) {
        val nameFirst = preferences.getBoolean(KEY_NAME_FIRST)
        val message = context.getString(R.string.muted_user, manager.getDisplayName(user,
                nameFirst))
        Utils.showInfoMessage(context, message, false)

    }

    override fun showErrorMessage(params: AbsFriendshipOperationTask.Arguments, exception: Exception?) {
        Utils.showErrorMessage(context, R.string.action_muting, exception, true)
    }
}
