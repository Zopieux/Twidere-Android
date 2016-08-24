package org.mariotaku.twidere.task

import android.content.Context

import com.squareup.otto.Bus

import org.mariotaku.abstask.library.AbstractTask
import org.mariotaku.microblog.library.MicroBlog
import org.mariotaku.microblog.library.MicroBlogException
import org.mariotaku.microblog.library.twitter.model.User
import org.mariotaku.twidere.model.ParcelableCredentials
import org.mariotaku.twidere.model.ParcelableUser
import org.mariotaku.twidere.model.SingleResponse
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.model.message.FriendshipTaskEvent
import org.mariotaku.twidere.model.util.ParcelableCredentialsUtils
import org.mariotaku.twidere.model.util.ParcelableUserUtils
import org.mariotaku.twidere.util.AsyncTwitterWrapper
import org.mariotaku.twidere.util.MicroBlogAPIFactory
import org.mariotaku.twidere.util.SharedPreferencesWrapper
import org.mariotaku.twidere.util.UserColorNameManager
import org.mariotaku.twidere.util.dagger.GeneralComponentHelper

import javax.inject.Inject

/**
 * Created by mariotaku on 16/3/11.
 */
abstract class AbsFriendshipOperationTask(
        protected val context: Context,
        @FriendshipTaskEvent.Action protected val action: Int
) : AbstractTask<AbsFriendshipOperationTask.Arguments, SingleResponse<ParcelableUser>, Any>() {
    @Inject
    lateinit protected var bus: Bus
    @Inject
    lateinit protected var twitter: AsyncTwitterWrapper
    @Inject
    lateinit protected var preferences: SharedPreferencesWrapper
    @Inject
    lateinit protected var manager: UserColorNameManager

    init {
        GeneralComponentHelper.build(context).inject(this)
    }


    override fun beforeExecute() {
        val params = params
        twitter.addUpdatingRelationshipId(params.accountKey, params.userKey)
        val event = FriendshipTaskEvent(action, params.accountKey,
                params.userKey)
        event.isFinished = false
        bus.post(event)
    }

    override fun afterExecute(callback: Any?, result: SingleResponse<ParcelableUser>?) {
        val params = params
        twitter.removeUpdatingRelationshipId(params.accountKey, params.userKey)
        val event = FriendshipTaskEvent(action, params.accountKey,
                params.userKey)
        event.isFinished = true
        if (result!!.hasData()) {
            val user = result.data
            showSucceededMessage(params, user!!)
            event.isSucceeded = true
            event.user = result.data
        } else {
            showErrorMessage(params, result.exception)
        }
        bus.post(event)
    }

    public override fun doLongOperation(args: Arguments): SingleResponse<ParcelableUser> {
        val credentials = ParcelableCredentialsUtils.getCredentials(context,
                args.accountKey) ?: return SingleResponse.getInstance<ParcelableUser>()
        val twitter = MicroBlogAPIFactory.getInstance(context, credentials, false, false) ?: return SingleResponse.getInstance<ParcelableUser>()
        try {
            val user = perform(twitter, credentials, args)
            val parcelableUser = ParcelableUserUtils.fromUser(user, args.accountKey)
            succeededWorker(twitter, credentials, args, parcelableUser)
            return SingleResponse.getInstance(parcelableUser)
        } catch (e: MicroBlogException) {
            return SingleResponse.getInstance<ParcelableUser>(e)
        }

    }

    @Throws(MicroBlogException::class)
    protected abstract fun perform(twitter: MicroBlog,
                                   credentials: ParcelableCredentials,
                                   args: Arguments): User

    protected abstract fun succeededWorker(twitter: MicroBlog,
                                           credentials: ParcelableCredentials,
                                           args: Arguments,
                                           user: ParcelableUser)

    protected abstract fun showSucceededMessage(params: Arguments, user: ParcelableUser)

    protected abstract fun showErrorMessage(params: Arguments, exception: Exception?)

    fun setup(accountKey: UserKey, userKey: UserKey) {
        params = Arguments(accountKey, userKey)
    }

    class Arguments(val accountKey: UserKey, val userKey: UserKey)

}
