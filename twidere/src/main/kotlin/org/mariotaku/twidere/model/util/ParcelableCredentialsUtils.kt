package org.mariotaku.twidere.model.util

import android.content.Context
import android.database.Cursor

import org.mariotaku.twidere.model.ParcelableCredentials
import org.mariotaku.twidere.model.ParcelableCredentialsCursorIndices
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.provider.TwidereDataStore.Accounts
import org.mariotaku.twidere.util.DataStoreUtils

/**
 * Created by mariotaku on 16/3/4.
 */
object ParcelableCredentialsUtils {

    fun isOAuth(authType: Int): Boolean {
        when (authType) {
            ParcelableCredentials.AuthType.OAUTH, ParcelableCredentials.AuthType.XAUTH -> {
                return true
            }
        }
        return false
    }

    fun getCredentials(context: Context,
                       accountKey: UserKey): ParcelableCredentials? {
        val c = DataStoreUtils.getAccountCursor(context, Accounts.COLUMNS, accountKey) ?: return null
        try {
            val i = ParcelableCredentialsCursorIndices(c)
            if (c.moveToFirst()) {
                return i.newObject(c)
            }
        } finally {
            c.close()
        }
        return null
    }


    fun getCredentialses(cursor: Cursor?, indices: ParcelableCredentialsCursorIndices?): Array<ParcelableCredentials> {
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


    fun getCredentialses(context: Context): Array<ParcelableCredentials> {
        val cur = context.contentResolver.query(Accounts.CONTENT_URI,
                Accounts.COLUMNS, null, null, null) ?: return emptyArray()
        return getCredentialses(cur, ParcelableCredentialsCursorIndices(cur))
    }
}
