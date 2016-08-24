package org.mariotaku.twidere.task

import android.content.Context

import org.mariotaku.microblog.library.MicroBlog
import org.mariotaku.microblog.library.MicroBlogException
import org.mariotaku.microblog.library.twitter.model.User
import org.mariotaku.twidere.model.ParcelableCredentials

/**
 * Created by mariotaku on 16/3/11.
 */
class ReportSpamAndBlockTask(context: Context) : CreateUserBlockTask(context) {

    @Throws(MicroBlogException::class)
    override fun perform(twitter: MicroBlog, credentials: ParcelableCredentials,
                         args: AbsFriendshipOperationTask.Arguments): User {
        return twitter.reportSpam(args.userKey.id)
    }
}
