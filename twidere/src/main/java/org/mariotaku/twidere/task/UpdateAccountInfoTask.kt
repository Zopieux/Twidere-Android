package org.mariotaku.twidere.task

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.support.v4.util.LongSparseArray
import android.text.TextUtils
import org.mariotaku.abstask.library.AbstractTask
import org.mariotaku.sqliteqb.library.Expression
import org.mariotaku.twidere.model.ParcelableAccount
import org.mariotaku.twidere.model.ParcelableUser
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.provider.TwidereDataStore.*
import org.mariotaku.twidere.util.JsonSerializer

/**
 * Created by mariotaku on 16/3/8.
 */
class UpdateAccountInfoTask(
        private val context: Context
) : AbstractTask<Pair<ParcelableAccount, ParcelableUser>, Any, Any>() {

    override fun doLongOperation(params: Pair<ParcelableAccount, ParcelableUser>): Any? {
        val resolver = context.contentResolver
        val account = params.first
        val user = params.second
        if (user.is_cache) {
            return null
        }
        if (!user.key.maybeEquals(user.account_key)) {
            return null
        }

        val accountWhere = Expression.equalsArgs(Accounts._ID).sql
        val accountWhereArgs = arrayOf(account.id.toString())

        val accountValues = ContentValues()
        accountValues.put(Accounts.NAME, user.name)
        accountValues.put(Accounts.SCREEN_NAME, user.screen_name)
        accountValues.put(Accounts.PROFILE_IMAGE_URL, user.profile_image_url)
        accountValues.put(Accounts.PROFILE_BANNER_URL, user.profile_banner_url)
        accountValues.put(Accounts.ACCOUNT_USER, JsonSerializer.serialize(user, ParcelableUser::class.java))
        accountValues.put(Accounts.ACCOUNT_KEY, user.key.toString())

        resolver.update(Accounts.CONTENT_URI, accountValues, accountWhere, accountWhereArgs)

        val accountKeyValues = ContentValues()
        accountKeyValues.put(AccountSupportColumns.ACCOUNT_KEY, user.key.toString())
        val accountKeyWhere = Expression.equalsArgs(AccountSupportColumns.ACCOUNT_KEY).sql
        val accountKeyWhereArgs = arrayOf(account.account_key.toString())


        resolver.update(Statuses.CONTENT_URI, accountKeyValues, accountKeyWhere, accountKeyWhereArgs)
        resolver.update(Activities.AboutMe.CONTENT_URI, accountKeyValues, accountKeyWhere, accountKeyWhereArgs)
        resolver.update(DirectMessages.Inbox.CONTENT_URI, accountKeyValues, accountKeyWhere, accountKeyWhereArgs)
        resolver.update(DirectMessages.Outbox.CONTENT_URI, accountKeyValues, accountKeyWhere, accountKeyWhereArgs)
        resolver.update(CachedRelationships.CONTENT_URI, accountKeyValues, accountKeyWhere, accountKeyWhereArgs)

        updateTabs(context, resolver, user.key)


        return null
    }

    private fun updateTabs(context: Context, resolver: ContentResolver, accountKey: UserKey) {
        val tabsCursor = resolver.query(Tabs.CONTENT_URI, Tabs.COLUMNS, null, null, null) ?: return
        try {
//            val indices = TabCursorIndices(tabsCursor)
            tabsCursor.moveToFirst()
            val values = LongSparseArray<ContentValues>()
            while (!tabsCursor.isAfterLast) {
//                val tab = indices.newObject(tabsCursor)
//                val arguments = tab.getArguments()
//                if (arguments != null) {
//                    val accountId = arguments!!.getAccountId()
//                    val keys = arguments!!.getAccountKeys()
//                    if (TextUtils.equals(accountKey.id, accountId) && keys == null) {
//                        arguments!!.setAccountKeys(arrayOf(accountKey))
//                        values.put(tab.getId(), TabValuesCreator.create(tab))
//                    }
//                }
                tabsCursor.moveToNext()
            }
            val where = Expression.equalsArgs(Tabs._ID).sql
            for (i in 0 until values.size()) {
                val whereArgs = arrayOf(values.keyAt(i).toString())
                resolver.update(Tabs.CONTENT_URI, values.valueAt(i), where, whereArgs)
            }
        } finally {
            tabsCursor.close()
        }
    }
}
