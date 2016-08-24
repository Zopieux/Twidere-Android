package org.mariotaku.twidere.model.util

import android.content.Context
import android.database.Cursor

import org.mariotaku.sqliteqb.library.ArgsArray
import org.mariotaku.sqliteqb.library.Columns
import org.mariotaku.sqliteqb.library.Expression
import org.mariotaku.twidere.R
import org.mariotaku.twidere.model.ParcelableAccount
import org.mariotaku.twidere.model.ParcelableAccountCursorIndices
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.provider.TwidereDataStore.Accounts
import org.mariotaku.twidere.util.DataStoreUtils
import org.mariotaku.twidere.util.TwidereArrayUtils

/**
 * Created by mariotaku on 16/2/20.
 */
object ParcelableAccountUtils {

    fun getAccountKeys(accounts: Array<out ParcelableAccount>): Array<UserKey> {
        return Array(accounts.size) {
            accounts[it].account_key
        }
    }

    fun getAccount(context: Context, accountKey: UserKey): ParcelableAccount? {
        val c = DataStoreUtils.getAccountCursor(context,
                Accounts.COLUMNS_NO_CREDENTIALS, accountKey) ?: return null
        try {
            val i = ParcelableAccountCursorIndices(c)
            if (c.moveToFirst()) {
                return i.newObject(c)
            }
        } finally {
            c.close()
        }
        return null
    }

    fun getAccounts(context: Context, activatedOnly: Boolean,
                    officialKeyOnly: Boolean): Array<ParcelableAccount> {
        val list = DataStoreUtils.getAccountsList(context, activatedOnly, officialKeyOnly)
        return list.toTypedArray()
    }

    fun getAccounts(context: Context): Array<ParcelableAccount> {
        val cur = context.contentResolver.query(Accounts.CONTENT_URI,
                Accounts.COLUMNS_NO_CREDENTIALS, null, null, Accounts.SORT_POSITION) ?: return emptyArray()
        return getAccounts(cur, ParcelableAccountCursorIndices(cur))
    }

    fun getAccounts(context: Context, vararg accountIds: UserKey): Array<ParcelableAccount> {
        val where = Expression.`in`(Columns.Column(Accounts.ACCOUNT_KEY),
                ArgsArray(accountIds.size)).sql
        val whereArgs = accountIds.map(Any::toString).toTypedArray()
        val cur = context.contentResolver.query(Accounts.CONTENT_URI,
                Accounts.COLUMNS_NO_CREDENTIALS, where, whereArgs, null) ?: return emptyArray()
        return getAccounts(cur, ParcelableAccountCursorIndices(cur))
    }

    fun getAccounts(cursor: Cursor?): Array<ParcelableAccount> {
        if (cursor == null) return emptyArray()
        return getAccounts(cursor, ParcelableAccountCursorIndices(cursor))
    }

    fun getAccounts(cursor: Cursor?, indices: ParcelableAccountCursorIndices?): Array<ParcelableAccount> {
        if (cursor == null || indices == null) return emptyArray()
        try {
            return Array(cursor.count) {
                cursor.moveToPosition(it)
                indices.newObject(cursor)
            }
        } finally {
            cursor.close()
        }
    }

    @ParcelableAccount.Type
    fun getAccountType(account: ParcelableAccount): String {
        return account.account_type ?: ParcelableAccount.Type.TWITTER
    }

    fun getAccountTypeIcon(accountType: String?): Int {
        if (accountType == null) return R.drawable.ic_account_logo_twitter
        when (accountType) {
            ParcelableAccount.Type.TWITTER -> {
                return R.drawable.ic_account_logo_twitter
            }
            ParcelableAccount.Type.FANFOU -> {
                return R.drawable.ic_account_logo_fanfou
            }
            ParcelableAccount.Type.STATUSNET -> {
                return R.drawable.ic_account_logo_statusnet
            }
        }
        return R.drawable.ic_account_logo_twitter
    }
}
