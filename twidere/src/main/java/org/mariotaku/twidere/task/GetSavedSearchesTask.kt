package org.mariotaku.twidere.task

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.util.Log

import org.mariotaku.abstask.library.AbstractTask
import org.mariotaku.microblog.library.MicroBlog
import org.mariotaku.microblog.library.MicroBlogException
import org.mariotaku.microblog.library.twitter.model.ResponseList
import org.mariotaku.microblog.library.twitter.model.SavedSearch
import org.mariotaku.sqliteqb.library.Expression
import org.mariotaku.twidere.BuildConfig
import org.mariotaku.twidere.model.SingleResponse
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.provider.TwidereDataStore.SavedSearches
import org.mariotaku.twidere.util.ContentValuesCreator
import org.mariotaku.twidere.util.MicroBlogAPIFactory
import org.mariotaku.twidere.util.content.ContentResolverUtils

import org.mariotaku.twidere.TwidereConstants.LOGTAG

/**
 * Created by mariotaku on 16/2/13.
 */
class GetSavedSearchesTask(private val mContext: Context) : AbstractTask<Array<UserKey>, SingleResponse<Any>, Any>() {

    public override fun doLongOperation(params: Array<UserKey>): SingleResponse<Any> {
        val cr = mContext.contentResolver
        for (accountKey in params) {
            val twitter = MicroBlogAPIFactory.getInstance(mContext, accountKey, true) ?: continue
            try {
                val searches = twitter.savedSearches
                val values = ContentValuesCreator.createSavedSearches(searches,
                        accountKey)
                val where = Expression.equalsArgs(SavedSearches.ACCOUNT_KEY)
                val whereArgs = arrayOf(accountKey.toString())
                cr.delete(SavedSearches.CONTENT_URI, where.sql, whereArgs)
                ContentResolverUtils.bulkInsert(cr, SavedSearches.CONTENT_URI, values)
            } catch (e: MicroBlogException) {
                if (BuildConfig.DEBUG) {
                    Log.w(LOGTAG, e)
                }
            }

        }
        return SingleResponse.getInstance<Any>()
    }
}
