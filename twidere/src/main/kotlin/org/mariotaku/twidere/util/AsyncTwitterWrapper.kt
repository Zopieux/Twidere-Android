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

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.support.v4.util.SimpleArrayMap
import android.util.Log
import com.squareup.otto.Bus
import edu.tsinghua.hotmobi.HotMobiLogger
import edu.tsinghua.hotmobi.model.TimelineType
import edu.tsinghua.hotmobi.model.TweetEvent
import org.apache.commons.collections.primitives.ArrayIntList
import org.apache.commons.collections.primitives.ArrayLongList
import org.mariotaku.abstask.library.AbstractTask
import org.mariotaku.abstask.library.TaskStarter
import org.mariotaku.microblog.library.MicroBlog
import org.mariotaku.microblog.library.MicroBlogException
import org.mariotaku.microblog.library.twitter.http.HttpResponseCode
import org.mariotaku.microblog.library.twitter.model.*
import org.mariotaku.sqliteqb.library.Expression
import org.mariotaku.twidere.BuildConfig
import org.mariotaku.twidere.R
import org.mariotaku.twidere.TwidereConstants.*
import org.mariotaku.twidere.model.*
import org.mariotaku.twidere.model.message.*
import org.mariotaku.twidere.model.util.*
import org.mariotaku.twidere.provider.TwidereDataStore
import org.mariotaku.twidere.provider.TwidereDataStore.*
import org.mariotaku.twidere.provider.TwidereDataStore.DirectMessages.Inbox
import org.mariotaku.twidere.provider.TwidereDataStore.DirectMessages.Outbox
import org.mariotaku.twidere.service.BackgroundOperationService
import org.mariotaku.twidere.task.*
import org.mariotaku.twidere.util.dagger.GeneralComponentHelper
import java.io.IOException
import java.util.*
import javax.inject.Inject

class AsyncTwitterWrapper(val context: Context, private val mBus: Bus?, val preferences: SharedPreferencesWrapper,
                          private val mAsyncTaskManager: AsyncTaskManager) : TwitterWrapper() {
    private val mResolver: ContentResolver

    private val mCreatingFavoriteIds = ArrayIntList()
    private val mDestroyingFavoriteIds = ArrayIntList()
    private val mCreatingRetweetIds = ArrayIntList()
    private val mDestroyingStatusIds = ArrayIntList()
    private val mUpdatingRelationshipIds = ArrayIntList()

    private val mSendingDraftIds = ArrayLongList()

    init {
        mResolver = context.contentResolver
    }

    fun acceptFriendshipAsync(accountKey: UserKey, userKey: UserKey) {
        val task = AcceptFriendshipTask(context)
        task.setup(accountKey, userKey)
        TaskStarter.execute(task)
    }

    fun addSendingDraftId(id: Long) {
        synchronized(mSendingDraftIds) {
            mSendingDraftIds.add(id)
            mResolver.notifyChange(Drafts.CONTENT_URI_UNSENT, null)
        }
    }

    fun addUserListMembersAsync(accountKey: UserKey, listId: String, vararg users: ParcelableUser): Int {
        val task = AddUserListMembersTask(accountKey, listId, users)
        return mAsyncTaskManager.add(task, true)
    }

    fun cancelRetweetAsync(accountKey: UserKey, statusId: String?, myRetweetId: String?): Int {
        if (myRetweetId != null)
            return destroyStatusAsync(accountKey, myRetweetId)
        else if (statusId != null)
            return destroyStatusAsync(accountKey, statusId)
        return -1
    }

    fun clearNotificationAsync(notificationType: Int) {
        clearNotificationAsync(notificationType, null)
    }

    fun clearNotificationAsync(notificationId: Int, accountKey: UserKey?) {
        val task = ClearNotificationTask(notificationId, accountKey)
        AsyncTaskUtils.executeTask(task)
    }

    fun clearUnreadCountAsync(position: Int) {
        val task = ClearUnreadCountTask(position)
        AsyncTaskUtils.executeTask(task)
    }

    fun createBlockAsync(accountKey: UserKey, userKey: UserKey) {
        val task = CreateUserBlockTask(context)
        task.setup(accountKey, userKey)
        TaskStarter.execute(task)
    }

    fun createFavoriteAsync(accountKey: UserKey, statusId: String): Int {
        val task = CreateFavoriteTask(accountKey, statusId)
        return mAsyncTaskManager.add(task, true)
    }

    fun createFriendshipAsync(accountKey: UserKey, userKey: UserKey) {
        val task = CreateFriendshipTask(context)
        task.setup(accountKey, userKey)
        TaskStarter.execute(task)
    }

    fun createMultiBlockAsync(accountKey: UserKey, userIds: Array<String>): Int {
        val task = CreateMultiBlockTask(accountKey, userIds)
        return mAsyncTaskManager.add(task, true)
    }

    fun createMuteAsync(accountKey: UserKey, userKey: UserKey) {
        val task = CreateUserMuteTask(context)
        task.setup(accountKey, userKey)
        TaskStarter.execute(task)
    }

    fun createSavedSearchAsync(accountKey: UserKey, query: String): Int {
        val task = CreateSavedSearchTask(accountKey, query)
        return mAsyncTaskManager.add(task, true)
    }

    fun createUserListAsync(accountKey: UserKey, listName: String, isPublic: Boolean,
                            description: String): Int {
        val task = CreateUserListTask(context, accountKey, listName, isPublic,
                description)
        return mAsyncTaskManager.add(task, true)
    }

    fun createUserListSubscriptionAsync(accountKey: UserKey, listId: String): Int {
        val task = CreateUserListSubscriptionTask(accountKey, listId)
        return mAsyncTaskManager.add(task, true)
    }

    fun deleteUserListMembersAsync(accountKey: UserKey, listId: String, vararg users: ParcelableUser): Int {
        val task = DeleteUserListMembersTask(accountKey, listId, users)
        return mAsyncTaskManager.add(task, true)
    }

    fun denyFriendshipAsync(accountKey: UserKey, userKey: UserKey) {
        val task = DenyFriendshipTask(context)
        task.setup(accountKey, userKey)
        TaskStarter.execute(task)
    }

    fun destroyBlockAsync(accountKey: UserKey, userKey: UserKey) {
        val task = DestroyUserBlockTask(context)
        task.setup(accountKey, userKey)
        TaskStarter.execute(task)
    }

    fun destroyDirectMessageAsync(accountKey: UserKey, messageId: String): Int {
        val task = DestroyDirectMessageTask(accountKey, messageId)
        return mAsyncTaskManager.add(task, true)
    }

    fun destroyMessageConversationAsync(accountKey: UserKey, userId: String): Int {
        val task = DestroyMessageConversationTask(accountKey, userId)
        return mAsyncTaskManager.add(task, true)
    }

    fun destroyFavoriteAsync(accountKey: UserKey, statusId: String): Int {
        val task = DestroyFavoriteTask(accountKey, statusId)
        return mAsyncTaskManager.add(task, true)
    }

    fun destroyFriendshipAsync(accountKey: UserKey, userKey: UserKey) {
        val task = DestroyFriendshipTask(context)
        task.setup(accountKey, userKey)
        TaskStarter.execute(task)
    }

    fun destroyMuteAsync(accountKey: UserKey, userKey: UserKey) {
        val task = DestroyUserMuteTask(context)
        task.setup(accountKey, userKey)
        TaskStarter.execute(task)
    }

    fun destroySavedSearchAsync(accountKey: UserKey, searchId: Long): Int {
        val task = DestroySavedSearchTask(accountKey, searchId)
        return mAsyncTaskManager.add(task, true)
    }

    fun destroyStatusAsync(accountKey: UserKey, statusId: String): Int {
        val task = DestroyStatusTask(accountKey, statusId)
        return mAsyncTaskManager.add(task, true)
    }

    fun destroyUserListAsync(accountKey: UserKey, listId: String): Int {
        val task = DestroyUserListTask(context, accountKey, listId)
        return mAsyncTaskManager.add(task, true)
    }

    fun destroyUserListSubscriptionAsync(accountKey: UserKey, listId: String): Int {
        val task = DestroyUserListSubscriptionTask(accountKey, listId)
        return mAsyncTaskManager.add(task, true)
    }

    fun getHomeTimelineAsync(param: RefreshTaskParam): Boolean {
        val task = GetHomeTimelineTask(context)
        task.params = param
        TaskStarter.execute(task)
        return true
    }

    fun getLocalTrendsAsync(accountId: UserKey, woeid: Int) {
        val task = GetLocalTrendsTask(context, accountId, woeid)
        TaskStarter.execute(task)
    }

    fun getReceivedDirectMessagesAsync(param: RefreshTaskParam) {
        val task = GetReceivedDirectMessagesTask(context)
        task.params = param
        TaskStarter.execute(task)
    }

    fun getSentDirectMessagesAsync(param: RefreshTaskParam) {
        val task = GetSentDirectMessagesTask(context)
        task.params = param
        TaskStarter.execute(task)
    }

    fun getSavedSearchesAsync(accountKeys: Array<UserKey>) {
        val task = GetSavedSearchesTask(context)
        task.params = accountKeys
        TaskStarter.execute(task)
    }

    val sendingDraftIds: LongArray
        get() = mSendingDraftIds.toArray()

    fun isCreatingFavorite(accountId: UserKey?, statusId: String?): Boolean {
        return mCreatingFavoriteIds.contains(calculateHashCode(accountId, statusId))
    }

    fun isCreatingRetweet(accountKey: UserKey?, statusId: String?): Boolean {
        return mCreatingRetweetIds.contains(calculateHashCode(accountKey, statusId))
    }

    fun isDestroyingFavorite(accountKey: UserKey?, statusId: String?): Boolean {
        return mDestroyingFavoriteIds.contains(calculateHashCode(accountKey, statusId))
    }

    fun isDestroyingStatus(accountId: UserKey?, statusId: String?): Boolean {
        return mDestroyingStatusIds.contains(calculateHashCode(accountId, statusId))
    }

    val isHomeTimelineRefreshing: Boolean
        get() = mAsyncTaskManager.hasRunningTasksForTag(TASK_TAG_GET_HOME_TIMELINE)

    val isReceivedDirectMessagesRefreshing: Boolean
        get() = mAsyncTaskManager.hasRunningTasksForTag(TASK_TAG_GET_RECEIVED_DIRECT_MESSAGES)

    val isSentDirectMessagesRefreshing: Boolean
        get() = mAsyncTaskManager.hasRunningTasksForTag(TASK_TAG_GET_SENT_DIRECT_MESSAGES)

    fun refreshAll() {
        refreshAll(object : GetAccountKeysClosure {
            override val accountKeys: Array<UserKey>
                get() = DataStoreUtils.getActivatedAccountKeys(context)
        })
    }

    fun refreshAll(accountKeys: Array<UserKey>): Boolean {
        return refreshAll(object : GetAccountKeysClosure {
            override val accountKeys: Array<UserKey>
                get() = accountKeys
        })
    }

    fun refreshAll(closure: GetAccountKeysClosure): Boolean {
        getHomeTimelineAsync(object : SimpleRefreshTaskParam() {

            override fun getAccountKeysWorker(): Array<UserKey> {
                return closure.accountKeys
            }

            override fun getSinceIds(): Array<String>? {
                return DataStoreUtils.getNewestStatusIds(context, Statuses.CONTENT_URI,
                        accountKeys)
            }
        })
        if (preferences.getBoolean(KEY_HOME_REFRESH_MENTIONS)) {
            getActivitiesAboutMeAsync(object : SimpleRefreshTaskParam() {
                override fun getAccountKeysWorker(): Array<UserKey> {
                    return closure.accountKeys
                }

                override fun getSinceIds(): Array<String>? {
                    return DataStoreUtils.getNewestActivityMaxPositions(context,
                            Activities.AboutMe.CONTENT_URI, accountKeys)
                }
            })
        }
        if (preferences.getBoolean(KEY_HOME_REFRESH_DIRECT_MESSAGES)) {
            getReceivedDirectMessagesAsync(object : SimpleRefreshTaskParam() {
                override fun getAccountKeysWorker(): Array<UserKey> {
                    return closure.accountKeys
                }
            })
            getSentDirectMessagesAsync(object : SimpleRefreshTaskParam() {
                override fun getAccountKeysWorker(): Array<UserKey> {
                    return closure.accountKeys
                }
            })
        }
        if (preferences.getBoolean(KEY_HOME_REFRESH_SAVED_SEARCHES)) {
            getSavedSearchesAsync(closure.accountKeys)
        }
        return true
    }

    fun removeSendingDraftId(id: Long) {
        synchronized(mSendingDraftIds) {
            mSendingDraftIds.removeElement(id)
            mResolver.notifyChange(Drafts.CONTENT_URI_UNSENT, null)
        }
    }

    fun removeUnreadCountsAsync(position: Int, counts: SimpleArrayMap<UserKey, out Set<String>>) {
        val task = RemoveUnreadCountsTask(position, counts)
        AsyncTaskUtils.executeTask(task)
    }

    fun reportMultiSpam(accountKey: UserKey, userIds: Array<String>) {
        // TODO implementation
    }

    fun reportSpamAsync(accountKey: UserKey, userKey: UserKey) {
        val task = ReportSpamAndBlockTask(context)
        task.setup(accountKey, userKey)
        TaskStarter.execute(task)
    }

    fun retweetStatusAsync(accountKey: UserKey, statusId: String): Int {
        val task = RetweetStatusTask(accountKey, statusId)
        return mAsyncTaskManager.add(task, true)
    }

    fun sendDirectMessageAsync(accountKey: UserKey, recipientId: String, text: String,
                               imageUri: String?): Int {
        val intent = Intent(context, BackgroundOperationService::class.java)
        intent.action = INTENT_ACTION_SEND_DIRECT_MESSAGE
        intent.putExtra(EXTRA_ACCOUNT_KEY, accountKey)
        intent.putExtra(EXTRA_RECIPIENT_ID, recipientId)
        intent.putExtra(EXTRA_TEXT, text)
        intent.putExtra(EXTRA_IMAGE_URI, imageUri)
        context.startService(intent)
        return 0
    }

    fun updateUserListDetails(accountKey: UserKey, listId: String,
                              update: UserListUpdate): Int {
        val task = UpdateUserListDetailsTask(context, accountKey,
                listId, update)
        return mAsyncTaskManager.add(task, true)
    }

    fun updateFriendship(accountKey: UserKey, userKey: UserKey, update: FriendshipUpdate) {
        val bus = mBus ?: return
        TaskStarter.execute(object : AbstractTask<Any, SingleResponse<Relationship>, Bus>() {
            public override fun doLongOperation(param: Any): SingleResponse<Relationship> {
                val twitter = MicroBlogAPIFactory.getInstance(context, accountKey, true) ?: throw MicroBlogException()
                try {
                    val relationship = twitter.updateFriendship(userKey.id, update)
                    if (!relationship.isSourceWantRetweetsFromTarget) {
                        // TODO remove cached retweets
                        val where = Expression.and(
                                Expression.equalsArgs(Statuses.ACCOUNT_KEY),
                                Expression.equalsArgs(Statuses.RETWEETED_BY_USER_KEY))
                        val selectionArgs = arrayOf(accountKey.toString(), userKey.toString())
                        context.contentResolver.delete(Statuses.CONTENT_URI, where.sql, selectionArgs)
                    }
                    return SingleResponse.getInstance(relationship)
                } catch (e: MicroBlogException) {
                    return SingleResponse.getInstance<Relationship>(e)
                }

            }

            public override fun afterExecute(handler: Bus?, result: SingleResponse<Relationship>?) {
                if (result!!.hasData()) {
                    handler!!.post(FriendshipUpdatedEvent(accountKey, userKey, result.data!!))
                } else if (result.hasException()) {
                    if (BuildConfig.DEBUG) {
                        Log.w(LOGTAG, "Unable to update friendship", result.exception)
                    }
                }
            }
        }.setCallback(bus))
    }

    fun getActivitiesAboutMeAsync(param: RefreshTaskParam) {
        val task = GetActivitiesAboutMeTask(context)
        task.params = param
        TaskStarter.execute(task)
    }

    fun setActivitiesAboutMeUnreadAsync(accountKeys: Array<UserKey>, cursor: Long) {
        val task = object : AbstractTask<Any, Any, AsyncTwitterWrapper>() {

            public override fun doLongOperation(o: Any): Any? {
                for (accountId in accountKeys) {
                    if (!Utils.isOfficialCredentials(context, accountId)) continue
                    try {
                        val twitter = MicroBlogAPIFactory.getInstance(context, accountId, false) ?: throw MicroBlogException()
                        twitter.setActivitiesAboutMeUnread(cursor)
                    } catch (e: MicroBlogException) {
                        if (BuildConfig.DEBUG) {
                            Log.w(LOGTAG, e)
                        }
                    }

                }
                return null
            }
        }
        TaskStarter.execute(task)
    }

    fun addUpdatingRelationshipId(accountKey: UserKey, userId: UserKey) {
        mUpdatingRelationshipIds.add(ParcelableUser.calculateHashCode(accountKey, userId))
    }

    fun removeUpdatingRelationshipId(accountKey: UserKey, userId: UserKey) {
        mUpdatingRelationshipIds.removeElement(ParcelableUser.calculateHashCode(accountKey, userId))
    }

    fun isUpdatingRelationship(accountId: UserKey, userId: UserKey): Boolean {
        return mUpdatingRelationshipIds.contains(ParcelableUser.calculateHashCode(accountId, userId))
    }

    open class UpdateProfileImageTask<ResultHandler>(private val mContext: Context, private val mAccountKey: UserKey,
                                                     private val mImageUri: Uri, private val mDeleteImage: Boolean) : AbstractTask<Any, SingleResponse<ParcelableUser>, ResultHandler>() {

        @Inject
        protected var mBus: Bus? = null

        init {
            //noinspection unchecked
            GeneralComponentHelper.build(mContext).inject(this as UpdateProfileImageTask<Any>)
        }

        override fun doLongOperation(params: Any): SingleResponse<ParcelableUser> {
            try {
                val twitter = MicroBlogAPIFactory.getInstance(mContext, mAccountKey, true) ?: throw MicroBlogException()
                TwitterWrapper.updateProfileImage(mContext, twitter, mImageUri, mDeleteImage)
                // Wait for 5 seconds, see
                // https://dev.twitter.com/rest/reference/post/account/update_profile_image
                try {
                    Thread.sleep(5000L)
                } catch (e: InterruptedException) {
                    Log.w(LOGTAG, e)
                }

                val user = twitter.verifyCredentials()
                return SingleResponse.getInstance(ParcelableUserUtils.fromUser(user, mAccountKey))
            } catch (e: MicroBlogException) {
                return SingleResponse.getInstance<ParcelableUser>(e)
            } catch (e: IOException) {
                return SingleResponse.getInstance<ParcelableUser>(e)
            }

        }

        override fun afterExecute(handler: ResultHandler?, result: SingleResponse<ParcelableUser>?) {
            super.afterExecute(handler, result)
            if (result!!.hasData()) {
                Utils.showOkMessage(mContext, R.string.profile_image_updated, false)
                mBus!!.post(ProfileUpdatedEvent(result.data!!))
            } else {
                Utils.showErrorMessage(mContext, R.string.action_updating_profile_image, result.exception, true)
            }
        }

    }

    internal inner class AddUserListMembersTask(
            private val accountKey: UserKey,
            private val listId: String,
            private val users: Array<out ParcelableUser>
    ) : ManagedAsyncTask<Any, Any, SingleResponse<ParcelableUserList>>(this.context) {

        override fun doInBackground(vararg params: Any): SingleResponse<ParcelableUserList> {
            val twitter = MicroBlogAPIFactory.getInstance(this.context, accountKey, false) ?: return SingleResponse.getInstance<ParcelableUserList>()
            try {
                val userIds = Array<UserKey>(users.size) { users[it].key }
                val result = twitter.addUserListMembers(listId, UserKey.getIds(userIds))
                val list = ParcelableUserListUtils.from(result, accountKey)
                return SingleResponse.getInstance(list)
            } catch (e: MicroBlogException) {
                return SingleResponse.getInstance<ParcelableUserList>(e)
            }

        }

        override fun onPostExecute(result: SingleResponse<ParcelableUserList>) {
            val succeed = result.hasData()
            if (succeed) {
                val message: String
                if (users.size == 1) {
                    val user = users[0]
                    val nameFirst = mPreferences.getBoolean(KEY_NAME_FIRST)
                    val displayName = mUserColorNameManager.getDisplayName(user.key, user.name,
                            user.screen_name, nameFirst)
                    message = this.context.getString(R.string.added_user_to_list, displayName, result.data!!.name)
                } else {
                    val res = this.context.resources
                    message = res.getQuantityString(R.plurals.added_N_users_to_list, users.size, users.size,
                            result.data!!.name)
                }
                Utils.showOkMessage(this.context, message, false)
            } else {
                Utils.showErrorMessage(this.context, R.string.action_adding_member, result.exception, true)
            }
            bus.post(UserListMembersChangedEvent(UserListMembersChangedEvent.Action.ADDED,
                    result.data!!, users))
            super.onPostExecute(result)
        }

    }

    internal inner class ClearNotificationTask(
            private val notificationType: Int,
            private val accountKey: UserKey?
    ) : AsyncTask<Any, Any, Int>() {

        override fun doInBackground(vararg params: Any): Int {
            return TwitterWrapper.clearNotification(context, notificationType, accountKey)
        }

    }

    internal inner class ClearUnreadCountTask(private val position: Int) : AsyncTask<Any, Any, Int>() {

        override fun doInBackground(vararg params: Any): Int {
            return TwitterWrapper.clearUnreadCount(context, position)
        }

    }

    internal inner class CreateFavoriteTask(private val mAccountKey: UserKey, private val mStatusId: String) : ManagedAsyncTask<Any, Any, SingleResponse<ParcelableStatus>>(this.context) {

        override fun doInBackground(vararg params: Any): SingleResponse<ParcelableStatus> {
            val credentials = ParcelableCredentialsUtils.getCredentials(this.context, mAccountKey) ?: return SingleResponse.getInstance<ParcelableStatus>()
            val twitter = MicroBlogAPIFactory.getInstance(this.context, credentials, true, true) ?: return SingleResponse.getInstance<ParcelableStatus>()
            try {
                val result: ParcelableStatus
                when (ParcelableAccountUtils.getAccountType(credentials)) {
                    ParcelableAccount.Type.FANFOU -> {
                        result = ParcelableStatusUtils.fromStatus(twitter.createFanfouFavorite(mStatusId),
                                mAccountKey, false)
                    }
                    else -> {
                        result = ParcelableStatusUtils.fromStatus(twitter.createFavorite(mStatusId),
                                mAccountKey, false)
                    }
                }
                ParcelableStatusUtils.updateExtraInformation(result, credentials,
                        mUserColorNameManager)
                Utils.setLastSeen(this.context, result.mentions, System.currentTimeMillis())
                val values = ContentValues()
                values.put(Statuses.IS_FAVORITE, true)
                values.put(Statuses.REPLY_COUNT, result.reply_count)
                values.put(Statuses.RETWEET_COUNT, result.retweet_count)
                values.put(Statuses.FAVORITE_COUNT, result.favorite_count)
                val statusWhere = Expression.and(
                        Expression.equalsArgs(Statuses.ACCOUNT_KEY),
                        Expression.or(
                                Expression.equalsArgs(Statuses.STATUS_ID),
                                Expression.equalsArgs(Statuses.RETWEET_ID))).sql
                val statusWhereArgs = arrayOf(mAccountKey.toString(), mStatusId.toString(), mStatusId.toString())
                for (uri in TwidereDataStore.STATUSES_URIS) {
                    mResolver.update(uri, values, statusWhere, statusWhereArgs)
                }
                DataStoreUtils.updateActivityStatus(mResolver, mAccountKey, mStatusId) { activity ->
                    val statusesMatrix = arrayOf(activity.target_statuses, activity.target_object_statuses)
                    for (statusesArray in statusesMatrix) {
                        if (statusesArray == null) continue
                        for (status in statusesArray) {
                            if (result.id != status.id) continue
                            status.is_favorite = true
                            status.reply_count = result.reply_count
                            status.retweet_count = result.retweet_count
                            status.favorite_count = result.favorite_count
                        }
                    }
                }
                return SingleResponse.getInstance(result)
            } catch (e: MicroBlogException) {
                if (BuildConfig.DEBUG) {
                    Log.w(LOGTAG, e)
                }
                return SingleResponse.getInstance<ParcelableStatus>(e)
            }

        }


        override fun onPreExecute() {
            super.onPreExecute()
            val hashCode = calculateHashCode(mAccountKey, mStatusId)
            if (!mCreatingFavoriteIds.contains(hashCode)) {
                mCreatingFavoriteIds.add(hashCode)
            }
            bus.post(StatusListChangedEvent())
        }

        override fun onPostExecute(result: SingleResponse<ParcelableStatus>) {
            mCreatingFavoriteIds.removeElement(calculateHashCode(mAccountKey, mStatusId))
            val taskEvent = FavoriteTaskEvent(FavoriteTaskEvent.Action.CREATE,
                    mAccountKey, mStatusId)
            taskEvent.isFinished = true
            if (result.hasData()) {
                val status = result.data
                taskEvent.status = status
                taskEvent.isSucceeded = true
                // BEGIN HotMobi
                val tweetEvent = TweetEvent.create(context, status, TimelineType.OTHER)
                tweetEvent.setAction(TweetEvent.Action.FAVORITE)
                HotMobiLogger.getInstance(context).log(mAccountKey, tweetEvent)
                // END HotMobi
            } else {
                taskEvent.isSucceeded = false
                Utils.showErrorMessage(this.context, R.string.action_favoriting, result.exception, true)
            }
            bus.post(taskEvent)
            bus.post(StatusListChangedEvent())
            super.onPostExecute(result)
        }

    }

    internal inner class CreateMultiBlockTask(private val mAccountKey: UserKey, private val mUserIds: Array<String>) : ManagedAsyncTask<Any, Any, ListResponse<String>>(this.context) {

        private fun deleteCaches(list: List<String>) {
            for (uri in TwidereDataStore.STATUSES_URIS) {
                // TODO delete caches
                // ContentResolverUtils.bulkDelete(mResolver, uri, Statuses.USER_ID, list,
                // Statuses.ACCOUNT_ID + " = " + mAccountKey, false);
            }
            // I bet you don't want to see these users in your auto complete list.
            //TODO insert to blocked users data
            val values = ContentValues()
            values.put(CachedRelationships.BLOCKING, true)
            values.put(CachedRelationships.FOLLOWING, false)
            values.put(CachedRelationships.FOLLOWED_BY, false)
            val where = Expression.inArgs(CachedRelationships.USER_KEY, list.size).sql
            val selectionArgs = list.toTypedArray()
            mResolver.update(CachedRelationships.CONTENT_URI, values, where, selectionArgs)
        }

        override fun doInBackground(vararg params: Any): ListResponse<String> {
            val blockedUsers = ArrayList<String>()
            val twitter = MicroBlogAPIFactory.getInstance(this.context, mAccountKey, false)
            if (twitter != null) {
                for (userId in mUserIds) {
                    try {
                        val user = twitter.createBlock(userId)
                        blockedUsers.add(user.id)
                    } catch (e: MicroBlogException) {
                        deleteCaches(blockedUsers)
                        return ListResponse.getListInstance<String>(e)
                    }

                }
            }
            deleteCaches(blockedUsers)
            return ListResponse.getListInstance(blockedUsers)
        }

        override fun onPostExecute(result: ListResponse<String>) {
            if (result.hasData()) {
                Utils.showInfoMessage(this.context, R.string.users_blocked, false)
            } else {
                Utils.showErrorMessage(this.context, R.string.action_blocking, result.exception, true)
            }
            mBus!!.post(UsersBlockedEvent(mAccountKey, mUserIds))
            super.onPostExecute(result)
        }


    }

    internal inner class CreateSavedSearchTask(private val mAccountKey: UserKey, private val mQuery: String) : ManagedAsyncTask<Any, Any, SingleResponse<SavedSearch>>(this.context) {

        override fun doInBackground(vararg params: Any): SingleResponse<SavedSearch>? {
            val twitter = MicroBlogAPIFactory.getInstance(this.context, mAccountKey, false) ?: return null
            try {
                return SingleResponse.getInstance(twitter.createSavedSearch(mQuery))
            } catch (e: MicroBlogException) {
                return SingleResponse.getInstance<SavedSearch>(e)
            }

        }

        override fun onPostExecute(result: SingleResponse<SavedSearch>) {
            if (result.hasData()) {
                val message = this.context.getString(R.string.search_name_saved, result.data!!.query)
                Utils.showOkMessage(this.context, message, false)
            } else if (result.hasException()) {
                val exception = result.exception
                // https://github.com/TwidereProject/Twidere-Android/issues/244
                if (exception is MicroBlogException && exception.statusCode == 403) {
                    val desc = this.context.getString(R.string.saved_searches_already_saved_hint)
                    Utils.showErrorMessage(this.context, R.string.action_saving_search, desc, false)
                } else {
                    Utils.showErrorMessage(this.context, R.string.action_saving_search, exception, false)
                }
            }
            super.onPostExecute(result)
        }

    }

    internal inner class CreateUserListSubscriptionTask(private val mAccountKey: UserKey, private val mListId: String) : ManagedAsyncTask<Any, Any, SingleResponse<ParcelableUserList>>(this.context) {

        override fun doInBackground(vararg params: Any): SingleResponse<ParcelableUserList> {
            val twitter = MicroBlogAPIFactory.getInstance(this.context, mAccountKey, false) ?: return SingleResponse.getInstance<ParcelableUserList>()
            try {
                val userList = twitter.createUserListSubscription(mListId)
                val list = ParcelableUserListUtils.from(userList, mAccountKey)
                return SingleResponse.getInstance(list)
            } catch (e: MicroBlogException) {
                return SingleResponse.getInstance<ParcelableUserList>(e)
            }

        }

        override fun onPostExecute(result: SingleResponse<ParcelableUserList>) {
            val succeed = result.hasData()
            if (succeed) {
                val message = this.context.getString(R.string.subscribed_to_list, result.data!!.name)
                Utils.showOkMessage(this.context, message, false)
                bus.post(UserListSubscriptionEvent(UserListSubscriptionEvent.Action.SUBSCRIBE,
                        result.data))
            } else {
                Utils.showErrorMessage(this.context, R.string.action_subscribing_to_list, result.exception, true)
            }
            super.onPostExecute(result)
        }

    }

    internal class CreateUserListTask(context: Context, private val mAccountKey: UserKey, private val mListName: String?,
                                      private val mIsPublic: Boolean, private val mDescription: String) : ManagedAsyncTask<Any, Any, SingleResponse<ParcelableUserList>>(context) {

        override fun doInBackground(vararg params: Any): SingleResponse<ParcelableUserList> {
            val twitter = MicroBlogAPIFactory.getInstance(context, mAccountKey,
                    false)
            if (twitter == null || mListName == null) return SingleResponse.getInstance<ParcelableUserList>()
            try {
                val userListUpdate = UserListUpdate()
                userListUpdate.setName(mListName)
                userListUpdate.setMode(if (mIsPublic) UserList.Mode.PUBLIC else UserList.Mode.PRIVATE)
                userListUpdate.setDescription(mDescription)
                val list = twitter.createUserList(userListUpdate)
                return SingleResponse.getInstance(ParcelableUserListUtils.from(list, mAccountKey))
            } catch (e: MicroBlogException) {
                return SingleResponse.getInstance<ParcelableUserList>(e)
            }

        }

        override fun onPostExecute(result: SingleResponse<ParcelableUserList>) {
            val context = context
            if (result.hasData()) {
                val userList = result.data
                val message = context.getString(R.string.created_list, userList!!.name)
                Utils.showOkMessage(context, message, false)
                bus.post(UserListCreatedEvent(userList))
            } else {
                Utils.showErrorMessage(context, R.string.action_creating_list, result.exception, true)
            }
            super.onPostExecute(result)
        }

    }

    internal inner class DeleteUserListMembersTask(
            private val accountKey: UserKey,
            private val userListId: String,
            private val users: Array<out ParcelableUser>
    ) : ManagedAsyncTask<Any, Any, SingleResponse<ParcelableUserList>>(this.context) {

        override fun doInBackground(vararg params: Any): SingleResponse<ParcelableUserList> {
            val twitter = MicroBlogAPIFactory.getInstance(this.context, accountKey, false) ?: return SingleResponse.getInstance<ParcelableUserList>()
            try {
                val userIds = Array<UserKey>(users.size) { users[it].key }
                val userList = twitter.deleteUserListMembers(userListId, UserKey.getIds(userIds))
                val list = ParcelableUserListUtils.from(userList, accountKey)
                return SingleResponse.getInstance(list)
            } catch (e: MicroBlogException) {
                return SingleResponse.getInstance<ParcelableUserList>(e)
            }

        }

        override fun onPostExecute(result: SingleResponse<ParcelableUserList>) {
            val succeed = result.hasData()
            val message: String
            if (succeed) {
                if (users.size == 1) {
                    val user = users[0]
                    val nameFirst = mPreferences.getBoolean(KEY_NAME_FIRST)
                    val displayName = mUserColorNameManager.getDisplayName(user.key,
                            user.name, user.screen_name, nameFirst)
                    message = this.context.getString(R.string.deleted_user_from_list, displayName,
                            result.data!!.name)
                } else {
                    val res = this.context.resources
                    message = res.getQuantityString(R.plurals.deleted_N_users_from_list, users.size, users.size,
                            result.data!!.name)
                }
                bus.post(UserListMembersChangedEvent(UserListMembersChangedEvent.Action.REMOVED,
                        result.data, users))
                Utils.showInfoMessage(this.context, message, false)
            } else {
                Utils.showErrorMessage(this.context, R.string.action_deleting, result.exception, true)
            }
            super.onPostExecute(result)
        }

    }


    internal inner class DestroyDirectMessageTask(private val mAccountKey: UserKey, private val mMessageId: String) : ManagedAsyncTask<Any, Any, SingleResponse<DirectMessage>>(this.context) {

        private fun deleteMessages() {
            val where = Expression.and(Expression.equalsArgs(DirectMessages.ACCOUNT_KEY),
                    Expression.equalsArgs(DirectMessages.MESSAGE_ID)).sql
            val whereArgs = arrayOf(mAccountKey.toString(), mMessageId)
            mResolver.delete(DirectMessages.Inbox.CONTENT_URI, where, whereArgs)
            mResolver.delete(DirectMessages.Outbox.CONTENT_URI, where, whereArgs)
        }

        private fun isMessageNotFound(e: Exception?): Boolean {
            if (e !is MicroBlogException) return false
            return e.errorCode == ErrorInfo.PAGE_NOT_FOUND || e.statusCode == HttpResponseCode.NOT_FOUND
        }

        override fun doInBackground(vararg args: Any): SingleResponse<DirectMessage> {
            val twitter = MicroBlogAPIFactory.getInstance(this.context, mAccountKey, false) ?: return SingleResponse.getInstance<DirectMessage>()
            try {
                val message = twitter.destroyDirectMessage(mMessageId)
                deleteMessages()
                return SingleResponse.getInstance(message)
            } catch (e: MicroBlogException) {
                if (isMessageNotFound(e)) {
                    deleteMessages()
                }
                return SingleResponse.getInstance<DirectMessage>(e)
            }

        }


        override fun onPostExecute(result: SingleResponse<DirectMessage>?) {
            super.onPostExecute(result)
            if (result == null) return
            if (result.hasData() || isMessageNotFound(result.exception)) {
                Utils.showInfoMessage(this.context, R.string.direct_message_deleted, false)
            } else {
                Utils.showErrorMessage(this.context, R.string.action_deleting, result.exception, true)
            }
        }


    }


    internal inner class DestroyMessageConversationTask(private val mAccountKey: UserKey, private val mUserId: String) : ManagedAsyncTask<Any, Any, SingleResponse<Void>>(this.context) {

        private fun deleteMessages(accountKey: UserKey, userId: String) {
            val whereArgs = arrayOf(accountKey.toString(), userId)
            mResolver.delete(DirectMessages.Inbox.CONTENT_URI, Expression.and(
                    Expression.equalsArgs(AccountSupportColumns.ACCOUNT_KEY),
                    Expression.equalsArgs(Inbox.SENDER_ID)).sql, whereArgs)
            mResolver.delete(DirectMessages.Outbox.CONTENT_URI, Expression.and(
                    Expression.equalsArgs(AccountSupportColumns.ACCOUNT_KEY),
                    Expression.equalsArgs(Outbox.RECIPIENT_ID)).sql, whereArgs)
        }

        private fun isMessageNotFound(e: Exception?): Boolean {
            if (e !is MicroBlogException) return false
            return e.errorCode == ErrorInfo.PAGE_NOT_FOUND || e.statusCode == HttpResponseCode.NOT_FOUND
        }

        override fun doInBackground(vararg args: Any): SingleResponse<Void> {
            val twitter = MicroBlogAPIFactory.getInstance(this.context, mAccountKey, false) ?: return SingleResponse.getInstance<Void>()
            try {
                twitter.destroyDirectMessagesConversation(mAccountKey.id, mUserId)
                deleteMessages(mAccountKey, mUserId)
                return SingleResponse.getInstance<Void>()
            } catch (e: MicroBlogException) {
                if (isMessageNotFound(e)) {
                    deleteMessages(mAccountKey, mUserId)
                }
                return SingleResponse.getInstance<Void>(e)
            }

        }


        override fun onPostExecute(result: SingleResponse<Void>?) {
            super.onPostExecute(result)
            if (result == null) return
            if (result.hasData() || isMessageNotFound(result.exception)) {
                Utils.showInfoMessage(this.context, R.string.direct_message_deleted, false)
            } else {
                Utils.showErrorMessage(this.context, R.string.action_deleting, result.exception, true)
            }
        }


    }


    internal inner class DestroyFavoriteTask(private val mAccountKey: UserKey, private val mStatusId: String) : ManagedAsyncTask<Any, Any, SingleResponse<ParcelableStatus>>(this.context) {

        override fun doInBackground(vararg params: Any): SingleResponse<ParcelableStatus> {
            val credentials = ParcelableCredentialsUtils.getCredentials(this.context, mAccountKey) ?: return SingleResponse.getInstance<ParcelableStatus>()
            val twitter = MicroBlogAPIFactory.getInstance(this.context, credentials, true, true) ?: return SingleResponse.getInstance<ParcelableStatus>()
            try {
                val result: ParcelableStatus
                when (ParcelableAccountUtils.getAccountType(credentials)) {
                    ParcelableAccount.Type.FANFOU -> {
                        result = ParcelableStatusUtils.fromStatus(twitter.destroyFanfouFavorite(mStatusId),
                                mAccountKey, false)
                    }
                    else -> {
                        result = ParcelableStatusUtils.fromStatus(twitter.destroyFavorite(mStatusId),
                                mAccountKey, false)
                    }
                }
                val values = ContentValues()
                values.put(Statuses.IS_FAVORITE, false)
                values.put(Statuses.FAVORITE_COUNT, result.favorite_count - 1)
                values.put(Statuses.RETWEET_COUNT, result.retweet_count)
                values.put(Statuses.REPLY_COUNT, result.reply_count)

                val where = Expression.and(Expression.equalsArgs(Statuses.ACCOUNT_KEY),
                        Expression.or(Expression.equalsArgs(Statuses.STATUS_ID),
                                Expression.equalsArgs(Statuses.RETWEET_ID)))
                val whereArgs = arrayOf(mAccountKey.toString(), mStatusId, mStatusId)
                for (uri in TwidereDataStore.STATUSES_URIS) {
                    mResolver.update(uri, values, where.sql, whereArgs)
                }

                DataStoreUtils.updateActivityStatus(mResolver, mAccountKey, mStatusId) { activity ->
                    val statusesMatrix = arrayOf(activity.target_statuses, activity.target_object_statuses)
                    for (statusesArray in statusesMatrix) {
                        if (statusesArray == null) continue
                        for (status in statusesArray) {
                            if (result.id != status.id) continue
                            status.is_favorite = false
                            status.reply_count = result.reply_count
                            status.retweet_count = result.retweet_count
                            status.favorite_count = result.favorite_count - 1
                        }
                    }
                }
                return SingleResponse.getInstance(result)
            } catch (e: MicroBlogException) {
                return SingleResponse.getInstance<ParcelableStatus>(e)
            }

        }

        override fun onPreExecute() {
            super.onPreExecute()
            val hashCode = calculateHashCode(mAccountKey, mStatusId)
            if (!mDestroyingFavoriteIds.contains(hashCode)) {
                mDestroyingFavoriteIds.add(hashCode)
            }
            bus.post(StatusListChangedEvent())
        }

        override fun onPostExecute(result: SingleResponse<ParcelableStatus>) {
            mDestroyingFavoriteIds.removeElement(calculateHashCode(mAccountKey, mStatusId))
            val taskEvent = FavoriteTaskEvent(FavoriteTaskEvent.Action.DESTROY,
                    mAccountKey, mStatusId)
            taskEvent.isFinished = true
            if (result.hasData()) {
                val status = result.data
                taskEvent.status = status
                taskEvent.isSucceeded = true
                // BEGIN HotMobi
                val tweetEvent = TweetEvent.create(context, status, TimelineType.OTHER)
                tweetEvent.setAction(TweetEvent.Action.UNFAVORITE)
                HotMobiLogger.getInstance(context).log(mAccountKey, tweetEvent)
                // END HotMobi
                Utils.showInfoMessage(this.context, R.string.status_unfavorited, false)
            } else {
                taskEvent.isSucceeded = false
                Utils.showErrorMessage(this.context, R.string.action_unfavoriting, result.exception, true)
            }
            bus.post(taskEvent)
            bus.post(StatusListChangedEvent())
            super.onPostExecute(result)
        }

    }

    internal inner class DestroySavedSearchTask(private val mAccountKey: UserKey, private val mSearchId: Long) : ManagedAsyncTask<Any, Any, SingleResponse<SavedSearch>>(this.context) {

        override fun doInBackground(vararg params: Any): SingleResponse<SavedSearch> {
            val twitter = MicroBlogAPIFactory.getInstance(this.context, mAccountKey, false) ?: return SingleResponse.getInstance<SavedSearch>()
            try {
                return SingleResponse.getInstance(twitter.destroySavedSearch(mSearchId))
            } catch (e: MicroBlogException) {
                return SingleResponse.getInstance<SavedSearch>(e)
            }

        }

        override fun onPostExecute(result: SingleResponse<SavedSearch>) {
            if (result.hasData()) {
                val message = this.context.getString(R.string.search_name_deleted, result.data!!.query)
                Utils.showOkMessage(this.context, message, false)
                bus.post(SavedSearchDestroyedEvent(mAccountKey, mSearchId))
            } else {
                Utils.showErrorMessage(this.context, R.string.action_deleting_search, result.exception, false)
            }
            super.onPostExecute(result)
        }

    }

    internal inner class DestroyStatusTask(private val mAccountKey: UserKey, private val mStatusId: String) : ManagedAsyncTask<Any, Any, SingleResponse<ParcelableStatus>>(this.context) {

        override fun doInBackground(vararg params: Any): SingleResponse<ParcelableStatus> {
            val credentials = ParcelableCredentialsUtils.getCredentials(this.context,
                    mAccountKey) ?: return SingleResponse.getInstance<ParcelableStatus>()
            val twitter = MicroBlogAPIFactory.getInstance(this.context, credentials, true,
                    true) ?: return SingleResponse.getInstance<ParcelableStatus>()
            var status: ParcelableStatus? = null
            var exception: MicroBlogException? = null
            try {
                status = ParcelableStatusUtils.fromStatus(twitter.destroyStatus(mStatusId),
                        mAccountKey, false)
                ParcelableStatusUtils.updateExtraInformation(status, credentials,
                        mUserColorNameManager)
            } catch (e: MicroBlogException) {
                exception = e
            }

            if (status != null || exception != null && exception.errorCode == ErrorInfo.STATUS_NOT_FOUND) {
                DataStoreUtils.deleteStatus(mResolver, mAccountKey, mStatusId, status)
                DataStoreUtils.deleteActivityStatus(mResolver, mAccountKey, mStatusId, status)
            }
            return SingleResponse(status)
        }

        override fun onPreExecute() {
            super.onPreExecute()
            val hashCode = calculateHashCode(mAccountKey, mStatusId)
            if (!mDestroyingStatusIds.contains(hashCode)) {
                mDestroyingStatusIds.add(hashCode)
            }
            bus.post(StatusListChangedEvent())
        }

        override fun onPostExecute(result: SingleResponse<ParcelableStatus>) {
            mDestroyingStatusIds.removeElement(calculateHashCode(mAccountKey, mStatusId))
            if (result.hasData()) {
                val status = result.data
                if (status!!.retweet_id != null) {
                    Utils.showInfoMessage(this.context, R.string.retweet_cancelled, false)
                } else {
                    Utils.showInfoMessage(this.context, R.string.status_deleted, false)
                }
                bus.post(StatusDestroyedEvent(status))
            } else {
                Utils.showErrorMessage(this.context, R.string.action_deleting, result.exception, true)
            }
            super.onPostExecute(result)
        }

    }

    internal inner class DestroyUserListSubscriptionTask(private val mAccountKey: UserKey, private val mListId: String) : ManagedAsyncTask<Any, Any, SingleResponse<ParcelableUserList>>(this.context) {

        override fun doInBackground(vararg params: Any): SingleResponse<ParcelableUserList> {

            val twitter = MicroBlogAPIFactory.getInstance(this.context, mAccountKey, false) ?: return SingleResponse.getInstance<ParcelableUserList>()
            try {
                val userList = twitter.destroyUserListSubscription(mListId)
                val list = ParcelableUserListUtils.from(userList, mAccountKey)
                return SingleResponse.getInstance(list)
            } catch (e: MicroBlogException) {
                return SingleResponse.getInstance<ParcelableUserList>(e)
            }

        }

        override fun onPostExecute(result: SingleResponse<ParcelableUserList>) {
            val succeed = result.hasData()
            if (succeed) {
                val message = this.context.getString(R.string.unsubscribed_from_list, result.data!!.name)
                Utils.showOkMessage(this.context, message, false)
                bus.post(UserListSubscriptionEvent(UserListSubscriptionEvent.Action.UNSUBSCRIBE,
                        result.data))
            } else {
                Utils.showErrorMessage(this.context, R.string.action_unsubscribing_from_list, result.exception, true)
            }
            super.onPostExecute(result)
        }

    }

    internal class DestroyUserListTask(context: Context, private val mAccountKey: UserKey, private val mListId: String) : ManagedAsyncTask<Any, Any, SingleResponse<ParcelableUserList>>(context) {

        override fun doInBackground(vararg params: Any): SingleResponse<ParcelableUserList> {
            val twitter = MicroBlogAPIFactory.getInstance(context, mAccountKey,
                    false) ?: return SingleResponse.getInstance<ParcelableUserList>()
            try {
                val userList = twitter.destroyUserList(mListId)
                val list = ParcelableUserListUtils.from(userList, mAccountKey)
                return SingleResponse.getInstance(list)
            } catch (e: MicroBlogException) {
                return SingleResponse.getInstance<ParcelableUserList>(e)
            }

        }

        override fun onPostExecute(result: SingleResponse<ParcelableUserList>) {
            val succeed = result.hasData()
            val context = context
            if (succeed) {
                val message = context.getString(R.string.deleted_list, result.data!!.name)
                Utils.showInfoMessage(context, message, false)
                bus.post(UserListDestroyedEvent(result.data))
            } else {
                Utils.showErrorMessage(context, R.string.action_deleting, result.exception, true)
            }
            super.onPostExecute(result)
        }

    }

    internal class GetReceivedDirectMessagesTask(context: Context) : GetDirectMessagesTask(context) {

        override val databaseUri: Uri = Inbox.CONTENT_URI

        override val isOutgoing: Boolean = false

        @Throws(MicroBlogException::class)
        override fun getDirectMessages(twitter: MicroBlog, paging: Paging): ResponseList<DirectMessage> {
            return twitter.getDirectMessages(paging)
        }

        override fun beforeExecute(params: RefreshTaskParam) {
            val intent = Intent(BROADCAST_RESCHEDULE_DIRECT_MESSAGES_REFRESHING)
            context.sendBroadcast(intent)
            super.beforeExecute(params)
        }
    }

    internal class GetSentDirectMessagesTask(context: Context) : GetDirectMessagesTask(context) {

        override val databaseUri: Uri = Outbox.CONTENT_URI

        override val isOutgoing: Boolean = true

        @Throws(MicroBlogException::class)
        override fun getDirectMessages(twitter: MicroBlog, paging: Paging): ResponseList<DirectMessage> {
            return twitter.getSentDirectMessages(paging)
        }

    }

    internal inner class RemoveUnreadCountsTask(
            private val position: Int,
            private val counts: SimpleArrayMap<UserKey, out Set<String>>
    ) : AsyncTask<Any, Any, Int>() {

        override fun doInBackground(vararg params: Any): Int {
            return TwitterWrapper.removeUnreadCounts(context, position, counts)
        }

    }

    internal inner class RetweetStatusTask(private val mAccountKey: UserKey, private val mStatusId: String) : ManagedAsyncTask<Any, Any, SingleResponse<ParcelableStatus>>(this.context) {

        override fun doInBackground(vararg params: Any): SingleResponse<ParcelableStatus> {
            val credentials = ParcelableCredentialsUtils.getCredentials(this.context,
                    mAccountKey) ?: return SingleResponse.getInstance<ParcelableStatus>()
            val twitter = MicroBlogAPIFactory.getInstance(this.context, credentials, true, true) ?: return SingleResponse.getInstance<ParcelableStatus>()
            try {
                val result = ParcelableStatusUtils.fromStatus(twitter.retweetStatus(mStatusId),
                        mAccountKey, false)
                ParcelableStatusUtils.updateExtraInformation(result, credentials,
                        mUserColorNameManager)
                Utils.setLastSeen(this.context, result.mentions, System.currentTimeMillis())
                val values = ContentValues()
                values.put(Statuses.MY_RETWEET_ID, result.id)
                values.put(Statuses.REPLY_COUNT, result.reply_count)
                values.put(Statuses.RETWEET_COUNT, result.retweet_count)
                values.put(Statuses.FAVORITE_COUNT, result.favorite_count)
                val where = Expression.or(
                        Expression.equalsArgs(Statuses.STATUS_ID),
                        Expression.equalsArgs(Statuses.RETWEET_ID))
                val whereArgs = arrayOf(mStatusId, mStatusId)
                for (uri in TwidereDataStore.STATUSES_URIS) {
                    mResolver.update(uri, values, where.sql, whereArgs)
                }
                DataStoreUtils.updateActivityStatus(mResolver, mAccountKey, mStatusId) { activity ->
                    val statusesMatrix = arrayOf(activity.target_statuses, activity.target_object_statuses)
                    activity.status_my_retweet_id = result.my_retweet_id
                    for (statusesArray in statusesMatrix) {
                        if (statusesArray == null) continue
                        for (status in statusesArray) {
                            if (mStatusId == status.id || mStatusId == status.retweet_id
                                    || mStatusId == status.my_retweet_id) {
                                status.my_retweet_id = result.id
                                status.reply_count = result.reply_count
                                status.retweet_count = result.retweet_count
                                status.favorite_count = result.favorite_count
                            }
                        }
                    }
                }
                return SingleResponse.getInstance(result)
            } catch (e: MicroBlogException) {
                return SingleResponse.getInstance<ParcelableStatus>(e)
            }

        }

        override fun onPreExecute() {
            super.onPreExecute()
            val hashCode = calculateHashCode(mAccountKey, mStatusId)
            if (!mCreatingRetweetIds.contains(hashCode)) {
                mCreatingRetweetIds.add(hashCode)
            }
            bus.post(StatusListChangedEvent())
        }

        override fun onPostExecute(result: SingleResponse<ParcelableStatus>) {
            mCreatingRetweetIds.removeElement(calculateHashCode(mAccountKey, mStatusId))
            if (result.hasData()) {
                val status = result.data
                // BEGIN HotMobi
                val event = TweetEvent.create(context, status, TimelineType.OTHER)
                event.setAction(TweetEvent.Action.RETWEET)
                HotMobiLogger.getInstance(context).log(mAccountKey, event)
                // END HotMobi

                bus.post(StatusRetweetedEvent(status))
            } else {
                Utils.showErrorMessage(this.context, R.string.action_retweeting, result.exception, true)
            }
            super.onPostExecute(result)
        }

    }


    internal class UpdateUserListDetailsTask(private val mContext: Context, private val mAccountKey: UserKey,
                                             private val listId: String, private val update: UserListUpdate) : ManagedAsyncTask<Any, Any, SingleResponse<ParcelableUserList>>(mContext) {

        override fun doInBackground(vararg params: Any): SingleResponse<ParcelableUserList> {

            val twitter = MicroBlogAPIFactory.getInstance(mContext, mAccountKey, false)
            if (twitter != null) {
                try {
                    val list = twitter.updateUserList(listId, update)
                    return SingleResponse.getInstance(ParcelableUserListUtils.from(list, mAccountKey))
                } catch (e: MicroBlogException) {
                    return SingleResponse.getInstance<ParcelableUserList>(e)
                }

            }
            return SingleResponse.getInstance<ParcelableUserList>()
        }

        override fun onPostExecute(result: SingleResponse<ParcelableUserList>) {
            if (result.hasData()) {
                val message = mContext.getString(R.string.updated_list_details, result.data!!.name)
                Utils.showOkMessage(mContext, message, false)
                bus.post(UserListUpdatedEvent(result.data))
            } else {
                Utils.showErrorMessage(mContext, R.string.action_updating_details, result.exception, true)
            }
            super.onPostExecute(result)
        }

    }

    interface GetAccountKeysClosure {
        val accountKeys: Array<UserKey>
    }

    companion object {

        internal fun calculateHashCode(accountId: UserKey?, statusId: String?): Int {
            return (if (accountId == null) 0 else accountId.hashCode()) xor if (statusId == null) 0 else statusId.hashCode()
        }

        fun getException(responses: List<Response<*>>): Exception? {
            for (response in responses) {
                if (response.hasException()) return response.exception
            }
            return null
        }
    }
}
