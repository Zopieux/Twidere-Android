package org.mariotaku.twidere.task

import android.content.Context

import org.mariotaku.microblog.library.MicroBlog
import org.mariotaku.microblog.library.MicroBlogException
import org.mariotaku.microblog.library.twitter.model.User
import org.mariotaku.twidere.Constants
import org.mariotaku.twidere.R
import org.mariotaku.twidere.model.ParcelableAccount
import org.mariotaku.twidere.model.ParcelableCredentials
import org.mariotaku.twidere.model.ParcelableUser
import org.mariotaku.twidere.model.message.FriendshipTaskEvent
import org.mariotaku.twidere.model.util.ParcelableAccountUtils
import org.mariotaku.twidere.util.Utils

import org.mariotaku.twidere.constant.SharedPreferenceConstants.KEY_NAME_FIRST

/**
 * Created by mariotaku on 16/3/11.
 */
class AcceptFriendshipTask(context: Context) : AbsFriendshipOperationTask(context, FriendshipTaskEvent.Action.ACCEPT), Constants {

    @Throws(MicroBlogException::class)
    override fun perform(twitter: MicroBlog, credentials: ParcelableCredentials, args: AbsFriendshipOperationTask.Arguments): User {
        when (ParcelableAccountUtils.getAccountType(credentials)) {
            ParcelableAccount.Type.FANFOU -> {
                return twitter.acceptFanfouFriendship(args.userKey.id)
            }
        }
        return twitter.acceptFriendship(args.userKey.id)
    }

    override fun succeededWorker(twitter: MicroBlog, credentials: ParcelableCredentials, args: AbsFriendshipOperationTask.Arguments, user: ParcelableUser) {
        Utils.setLastSeen(context, user.key, System.currentTimeMillis())
    }

    override fun showErrorMessage(params: AbsFriendshipOperationTask.Arguments, exception: Exception?) {
        Utils.showErrorMessage(context, R.string.action_accepting_follow_request, exception, false)
    }

    override fun showSucceededMessage(params: AbsFriendshipOperationTask.Arguments, user: ParcelableUser) {
        val nameFirst = preferences.getBoolean(KEY_NAME_FIRST)
        val message = context.getString(R.string.accepted_users_follow_request,
                manager.getDisplayName(user, nameFirst))
        Utils.showOkMessage(context, message, false)
    }

}
