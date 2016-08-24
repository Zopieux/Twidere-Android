package org.mariotaku.twidere.task

import android.content.Context
import android.net.Uri

import org.mariotaku.microblog.library.MicroBlog
import org.mariotaku.microblog.library.MicroBlogException
import org.mariotaku.microblog.library.twitter.model.Activity
import org.mariotaku.microblog.library.twitter.model.CursorTimestampResponse
import org.mariotaku.microblog.library.twitter.model.Paging
import org.mariotaku.microblog.library.twitter.model.ResponseList
import org.mariotaku.microblog.library.twitter.model.Status
import org.mariotaku.twidere.annotation.ReadPositionTag
import org.mariotaku.twidere.model.ParcelableAccount
import org.mariotaku.twidere.model.ParcelableCredentials
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.model.util.ParcelableAccountUtils
import org.mariotaku.twidere.provider.TwidereDataStore.Activities
import org.mariotaku.twidere.task.twitter.GetActivitiesTask
import org.mariotaku.twidere.util.ErrorInfoStore
import org.mariotaku.twidere.util.Utils

/**
 * Created by mariotaku on 16/2/11.
 */
class GetActivitiesAboutMeTask(context: Context) : GetActivitiesTask(context) {

    override val errorInfoKey: String
        get() = ErrorInfoStore.KEY_INTERACTIONS

    override val contentUri: Uri
        get() = Activities.AboutMe.CONTENT_URI

    override fun saveReadPosition(accountKey: UserKey, credentials: ParcelableCredentials, twitter: MicroBlog) {
        if (ParcelableAccount.Type.TWITTER == ParcelableAccountUtils.getAccountType(credentials)) {
            if (Utils.isOfficialCredentials(context, credentials)) {
                try {
                    val response = twitter.getActivitiesAboutMeUnread(true)
                    val tag = Utils.getReadPositionTagWithAccount(ReadPositionTag.ACTIVITIES_ABOUT_ME,
                            accountKey)
                    readStateManager.setPosition(tag, response.cursor, false)
                } catch (e: MicroBlogException) {
                    // Ignore
                }

            }
        }
    }

    @Throws(MicroBlogException::class)
    override fun getActivities(twitter: MicroBlog,
                               credentials: ParcelableCredentials,
                               paging: Paging): ResponseList<Activity> {
        if (Utils.isOfficialCredentials(context, credentials)) {
            return twitter.getActivitiesAboutMe(paging)
        }
        val activities = ResponseList<Activity>()
        val statuses: ResponseList<Status>
        when (ParcelableAccountUtils.getAccountType(credentials)) {
            ParcelableAccount.Type.FANFOU -> {
                statuses = twitter.getMentions(paging)
            }
            else -> {
                statuses = twitter.getMentionsTimeline(paging)
            }
        }
        for (status in statuses) {
            activities.add(Activity.fromMention(credentials.account_key.id, status))
        }
        return activities
    }
}
