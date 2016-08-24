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

package org.mariotaku.twidere.provider

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.*
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.database.Cursor
import android.database.MatrixCursor
import android.database.MergeCursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteFullException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.provider.BaseColumns
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationCompat.InboxStyle
import android.support.v4.text.BidiFormatter
import android.support.v4.util.LongSparseArray
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.StyleSpan
import android.util.Log
import com.nostra13.universalimageloader.core.ImageLoader
import com.squareup.otto.Bus
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.math.NumberUtils
import org.mariotaku.ktextension.convert
import org.mariotaku.microblog.library.twitter.model.Activity
import org.mariotaku.sqliteqb.library.*
import org.mariotaku.sqliteqb.library.Columns.Column
import org.mariotaku.twidere.BuildConfig
import org.mariotaku.twidere.Constants
import org.mariotaku.twidere.R
import org.mariotaku.twidere.TwidereConstants.*
import org.mariotaku.twidere.activity.HomeActivity
import org.mariotaku.twidere.annotation.CustomTabType
import org.mariotaku.twidere.annotation.NotificationType
import org.mariotaku.twidere.annotation.ReadPositionTag
import org.mariotaku.twidere.app.TwidereApplication
import org.mariotaku.twidere.constant.IntentConstants
import org.mariotaku.twidere.constant.SharedPreferenceConstants
import org.mariotaku.twidere.model.*
import org.mariotaku.twidere.model.message.UnreadCountUpdatedEvent
import org.mariotaku.twidere.model.util.ParcelableActivityUtils
import org.mariotaku.twidere.provider.TwidereDataStore.*
import org.mariotaku.twidere.receiver.NotificationReceiver
import org.mariotaku.twidere.service.BackgroundOperationService
import org.mariotaku.twidere.util.*
import org.mariotaku.twidere.util.SQLiteDatabaseWrapper.LazyLoadCallback
import org.mariotaku.twidere.util.TwidereQueryBuilder.CachedUsersQueryBuilder
import org.mariotaku.twidere.util.TwidereQueryBuilder.ConversationQueryBuilder
import org.mariotaku.twidere.util.Utils
import org.mariotaku.twidere.util.collection.CompactHashSet
import org.mariotaku.twidere.util.dagger.GeneralComponentHelper
import org.mariotaku.twidere.util.media.preview.PreviewMediaExtractor
import org.mariotaku.twidere.util.net.TwidereDns
import org.oshkimaadziig.george.androidutils.SpanFormatter
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.regex.Pattern
import javax.inject.Inject

class TwidereDataProvider : ContentProvider(), Constants, OnSharedPreferenceChangeListener, LazyLoadCallback {
    @Inject
    lateinit var readStateManager: ReadStateManager
    @Inject
    lateinit var mTwitterWrapper: AsyncTwitterWrapper
    @Inject
    lateinit var mMediaLoader: ImageLoader
    @Inject
    lateinit var notificationManager: NotificationManagerWrapper
    @Inject
    lateinit var preferences: SharedPreferencesWrapper
    @Inject
    lateinit var dns: TwidereDns
    @Inject
    lateinit var bus: Bus
    @Inject
    lateinit var userColorNameManager: UserColorNameManager
    @Inject
    lateinit var mBidiFormatter: BidiFormatter
    @Inject
    lateinit var activityTracker: ActivityTracker
    @Inject
    lateinit var permissionsManager: PermissionsManager

    private var mHandler: Handler? = null
    private var mContentResolver: ContentResolver? = null
    private var databaseWrapper: SQLiteDatabaseWrapper? = null
    private var mImagePreloader: ImagePreloader? = null
    private var mBackgroundExecutor: Executor? = null
    private var mNameFirst: Boolean = false
    private var mUseStarForLikes: Boolean = false

    override fun bulkInsert(uri: Uri, valuesArray: Array<ContentValues>): Int {
        try {
            return bulkInsertInternal(uri, valuesArray)
        } catch (e: SQLException) {
            if (handleSQLException(e)) {
                try {
                    return bulkInsertInternal(uri, valuesArray)
                } catch (e1: SQLException) {
                    throw IllegalStateException(e1)
                }

            }
            throw IllegalStateException(e)
        }

    }

    private fun handleSQLException(e: SQLException): Boolean {
        try {
            if (e is SQLiteFullException) {
                // Drop cached databases
                databaseWrapper!!.delete(CachedUsers.TABLE_NAME, null, null)
                databaseWrapper!!.delete(CachedStatuses.TABLE_NAME, null, null)
                databaseWrapper!!.delete(CachedHashtags.TABLE_NAME, null, null)
                databaseWrapper!!.execSQL("VACUUM")
                return true
            }
        } catch (ee: SQLException) {
            throw IllegalStateException(ee)
        }

        throw IllegalStateException(e)
    }

    private fun bulkInsertInternal(uri: Uri, valuesArray: Array<ContentValues>): Int {
        val tableId = DataStoreUtils.getTableId(uri)
        val table = DataStoreUtils.getTableNameById(tableId)
        checkWritePermission(tableId, table)
        when (tableId) {
            TABLE_ID_DIRECT_MESSAGES_CONVERSATION, TABLE_ID_DIRECT_MESSAGES, TABLE_ID_DIRECT_MESSAGES_CONVERSATIONS_ENTRIES -> return 0
        }
        var result = 0
        val newIds = LongArray(valuesArray.size)
        if (table != null && valuesArray.size > 0) {
            databaseWrapper!!.beginTransaction()
            if (tableId == TABLE_ID_CACHED_USERS) {
                for (values in valuesArray) {
                    val where = Expression.equalsArgs(CachedUsers.USER_KEY)
                    databaseWrapper!!.update(table, values, where.sql, arrayOf(values.getAsString(CachedUsers.USER_KEY)))
                    newIds[result++] = databaseWrapper!!.insertWithOnConflict(table, null,
                            values, SQLiteDatabase.CONFLICT_REPLACE)
                }
            } else if (tableId == TABLE_ID_SEARCH_HISTORY) {
                for (values in valuesArray) {
                    values.put(SearchHistory.RECENT_QUERY, System.currentTimeMillis())
                    val where = Expression.equalsArgs(SearchHistory.QUERY)
                    val args = arrayOf(values.getAsString(SearchHistory.QUERY))
                    databaseWrapper!!.update(table, values, where.sql, args)
                    newIds[result++] = databaseWrapper!!.insertWithOnConflict(table, null,
                            values, SQLiteDatabase.CONFLICT_IGNORE)
                }
            } else if (shouldReplaceOnConflict(tableId)) {
                for (values in valuesArray) {
                    newIds[result++] = databaseWrapper!!.insertWithOnConflict(table, null,
                            values, SQLiteDatabase.CONFLICT_REPLACE)
                }
            } else {
                for (values in valuesArray) {
                    newIds[result++] = databaseWrapper!!.insert(table, null, values)
                }
            }
            databaseWrapper!!.setTransactionSuccessful()
            databaseWrapper!!.endTransaction()
        }
        if (result > 0) {
            onDatabaseUpdated(tableId, uri)
        }
        onNewItemsInserted(uri, tableId, valuesArray)
        return result
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        try {
            return deleteInternal(uri, selection, selectionArgs)
        } catch (e: SQLException) {
            if (handleSQLException(e)) {
                try {
                    return deleteInternal(uri, selection, selectionArgs)
                } catch (e1: SQLException) {
                    throw IllegalStateException(e1)
                }

            }
            throw IllegalStateException(e)
        }

    }

    private fun deleteInternal(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val tableId = DataStoreUtils.getTableId(uri)
        val table = DataStoreUtils.getTableNameById(tableId)
        checkWritePermission(tableId, table)
        when (tableId) {
            TABLE_ID_DIRECT_MESSAGES_CONVERSATION, TABLE_ID_DIRECT_MESSAGES, TABLE_ID_DIRECT_MESSAGES_CONVERSATIONS_ENTRIES -> return 0
            VIRTUAL_TABLE_ID_NOTIFICATIONS -> {
                val segments = uri.pathSegments
                if (segments.size == 1) {
                    clearNotification()
                } else if (segments.size == 2) {
                    val notificationType = NumberUtils.toInt(segments[1], -1)
                    clearNotification(notificationType, null)
                } else if (segments.size == 3) {
                    val notificationType = NumberUtils.toInt(segments[1], -1)
                    val accountKey = UserKey.valueOf(segments[2])
                    clearNotification(notificationType, accountKey)
                }
                return 1
            }
            VIRTUAL_TABLE_ID_UNREAD_COUNTS -> {
                return 0
            }
        }
        if (table == null) return 0
        val result = databaseWrapper!!.delete(table, selection, selectionArgs)
        if (result > 0) {
            onDatabaseUpdated(tableId, uri)
        }
        return result
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        try {
            return insertInternal(uri, values)
        } catch (e: SQLException) {
            if (handleSQLException(e)) {
                try {
                    return insertInternal(uri, values)
                } catch (e1: SQLException) {
                    throw IllegalStateException(e1)
                }

            }
            throw IllegalStateException(e)
        }

    }

    private fun insertInternal(uri: Uri, values: ContentValues?): Uri? {
        val valuesNonNull = values ?: return null
        val tableId = DataStoreUtils.getTableId(uri)
        val table = DataStoreUtils.getTableNameById(tableId)
        checkWritePermission(tableId, table)
        when (tableId) {
            TABLE_ID_DIRECT_MESSAGES_CONVERSATION, TABLE_ID_DIRECT_MESSAGES, TABLE_ID_DIRECT_MESSAGES_CONVERSATIONS_ENTRIES -> return null
        }
        val rowId: Long
        when (tableId) {
            TABLE_ID_CACHED_USERS -> {
                val where = Expression.equalsArgs(CachedUsers.USER_KEY)
                val whereArgs = arrayOf(valuesNonNull.getAsString(CachedUsers.USER_KEY))
                databaseWrapper!!.update(table, valuesNonNull, where.sql, whereArgs)
                rowId = databaseWrapper!!.insertWithOnConflict(table, null, valuesNonNull,
                        SQLiteDatabase.CONFLICT_IGNORE)
            }
            TABLE_ID_SEARCH_HISTORY -> {
                valuesNonNull.put(SearchHistory.RECENT_QUERY, System.currentTimeMillis())
                val where = Expression.equalsArgs(SearchHistory.QUERY)
                val args = arrayOf(valuesNonNull.getAsString(SearchHistory.QUERY))
                databaseWrapper!!.update(table, valuesNonNull, where.sql, args)
                rowId = databaseWrapper!!.insertWithOnConflict(table, null, valuesNonNull,
                        SQLiteDatabase.CONFLICT_IGNORE)
            }
            TABLE_ID_CACHED_RELATIONSHIPS -> {
                val accountKey = valuesNonNull.getAsString(CachedRelationships.ACCOUNT_KEY)
                val userId = valuesNonNull.getAsString(CachedRelationships.USER_KEY)
                val where = Expression.and(
                        Expression.equalsArgs(CachedRelationships.ACCOUNT_KEY),
                        Expression.equalsArgs(CachedRelationships.USER_KEY))
                val whereArgs = arrayOf(accountKey, userId)
                if (databaseWrapper!!.update(table, valuesNonNull, where.sql, whereArgs) > 0) {
                    val projection = arrayOf(CachedRelationships._ID)
                    val c = databaseWrapper!!.query(table, projection, where.sql, null,
                            null, null, null)
                    if (c.moveToFirst()) {
                        rowId = c.getLong(0)
                    } else {
                        rowId = 0
                    }
                    c.close()
                } else {
                    rowId = databaseWrapper!!.insertWithOnConflict(table, null, valuesNonNull,
                            SQLiteDatabase.CONFLICT_IGNORE)
                }
            }
            VIRTUAL_TABLE_ID_DRAFTS_NOTIFICATIONS -> {
                rowId = showDraftNotification(valuesNonNull)
            }
            else -> {
                if (shouldReplaceOnConflict(tableId)) {
                    rowId = databaseWrapper!!.insertWithOnConflict(table, null, valuesNonNull,
                            SQLiteDatabase.CONFLICT_REPLACE)
                } else if (table != null) {
                    rowId = databaseWrapper!!.insert(table, null, valuesNonNull)
                } else {
                    return null
                }
            }
        }
        onDatabaseUpdated(tableId, uri)
        onNewItemsInserted(uri, tableId, valuesNonNull)
        return Uri.withAppendedPath(uri, rowId.toString())
    }

    private fun showDraftNotification(values: ContentValues?): Long {
        val context = context
        if (values == null || context == null) return -1
        val draftId = values.getAsLong(BaseColumns._ID) ?: return -1
        val where = Expression.equals(Drafts._ID, draftId)
        val c = contentResolver!!.query(Drafts.CONTENT_URI, Drafts.COLUMNS, where.sql, null, null) ?: return -1
        val i = DraftCursorIndices(c)
        val item: Draft
        try {
            if (!c.moveToFirst()) return -1
            item = i.newObject(c)
        } finally {
            c.close()
        }
        val title = context.getString(R.string.status_not_updated)
        val message = context.getString(R.string.status_not_updated_summary)
        val intent = Intent()
        intent.`package` = BuildConfig.APPLICATION_ID
        val uriBuilder = Uri.Builder()
        uriBuilder.scheme(SCHEME_TWIDERE)
        uriBuilder.authority(AUTHORITY_DRAFTS)
        intent.data = uriBuilder.build()
        val nb = NotificationCompat.Builder(context)
        nb.setTicker(message)
        nb.setContentTitle(title)
        nb.setContentText(item.text)
        nb.setAutoCancel(true)
        nb.setWhen(System.currentTimeMillis())
        nb.setSmallIcon(R.drawable.ic_stat_draft)
        val discardIntent = Intent(context, BackgroundOperationService::class.java)
        discardIntent.action = IntentConstants.INTENT_ACTION_DISCARD_DRAFT
        val draftUri = Uri.withAppendedPath(Drafts.CONTENT_URI, draftId.toString())
        discardIntent.data = draftUri
        nb.addAction(R.drawable.ic_action_delete, context.getString(R.string.discard), PendingIntent.getService(context, 0,
                discardIntent, PendingIntent.FLAG_ONE_SHOT))

        val sendIntent = Intent(context, BackgroundOperationService::class.java)
        sendIntent.action = IntentConstants.INTENT_ACTION_SEND_DRAFT
        sendIntent.data = draftUri
        nb.addAction(R.drawable.ic_action_send, context.getString(R.string.send),
                PendingIntent.getService(context, 0, sendIntent, PendingIntent.FLAG_ONE_SHOT))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        nb.setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT))
        notificationManager.notify(draftUri.toString(), NOTIFICATION_ID_DRAFTS,
                nb.build())
        return draftId
    }

    override fun onCreate(): Boolean {
        val context = context!!
        GeneralComponentHelper.build(context).inject(this)
        mHandler = Handler(Looper.getMainLooper())
        databaseWrapper = SQLiteDatabaseWrapper(this)
        preferences.registerOnSharedPreferenceChangeListener(this)
        mBackgroundExecutor = Executors.newSingleThreadExecutor()
        updatePreferences()
        mImagePreloader = ImagePreloader(context, mMediaLoader)
        // final GetWritableDatabaseTask task = new
        // GetWritableDatabaseTask(context, helper, mDatabaseWrapper);
        // task.executeTask();
        return true
    }

    override fun onCreateSQLiteDatabase(): SQLiteDatabase {
        val app = TwidereApplication.getInstance(context!!)
        val helper = app.sqLiteOpenHelper
        return helper.writableDatabase
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String) {
        updatePreferences()
    }

    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val table_id = DataStoreUtils.getTableId(uri)
        val table = DataStoreUtils.getTableNameById(table_id)
        val modeCode: Int
        when (mode) {
            "r" -> modeCode = ParcelFileDescriptor.MODE_READ_ONLY
            "rw" -> modeCode = ParcelFileDescriptor.MODE_READ_WRITE
            "rwt" -> modeCode = ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_TRUNCATE
            else -> throw IllegalArgumentException()
        }
        if (modeCode == ParcelFileDescriptor.MODE_READ_ONLY) {
            checkReadPermission(table_id, table, null)
        } else if (modeCode and ParcelFileDescriptor.MODE_READ_WRITE != 0) {
            checkReadPermission(table_id, table, null)
            checkWritePermission(table_id, table)
        }
        when (table_id) {
            VIRTUAL_TABLE_ID_CACHED_IMAGES -> {
                return getCachedImageFd(uri.getQueryParameter(QUERY_PARAM_URL))
            }
            VIRTUAL_TABLE_ID_CACHE_FILES -> {
                return getCacheFileFd(uri.lastPathSegment)
            }
        }
        return null
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?,
                       sortOrder: String?): Cursor? {
        try {
            val tableId = DataStoreUtils.getTableId(uri)
            val table = DataStoreUtils.getTableNameById(tableId)
            checkReadPermission(tableId, table, projection)
            when (tableId) {
                VIRTUAL_TABLE_ID_DATABASE_READY -> {
                    if (databaseWrapper!!.isReady)
                        return MatrixCursor(projection ?: arrayOfNulls<String>(0))
                    return null
                }
                VIRTUAL_TABLE_ID_PERMISSIONS -> {
                    val context = context ?: return null
                    val c = MatrixCursor(Permissions.MATRIX_COLUMNS)
                    val pm = context.packageManager
                    if (Binder.getCallingUid() == Process.myUid()) {
                        val map = permissionsManager.all
                        for ((key, value) in map) {
                            c.addRow(arrayOf<Any>(key, value))
                        }
                    } else {
                        val map = permissionsManager.all
                        val callingPackages = pm.getPackagesForUid(Binder.getCallingUid())
                        for ((key, value) in map) {
                            if (ArrayUtils.contains(callingPackages, key)) {
                                c.addRow(arrayOf<Any>(key, value))
                            }
                        }
                    }
                    return c
                }
                VIRTUAL_TABLE_ID_ALL_PREFERENCES -> {
                    return getPreferencesCursor(preferences, null)
                }
                VIRTUAL_TABLE_ID_PREFERENCES -> {
                    return getPreferencesCursor(preferences, uri.lastPathSegment)
                }
                VIRTUAL_TABLE_ID_DNS -> {
                    return getDNSCursor(uri.lastPathSegment)
                }
                VIRTUAL_TABLE_ID_CACHED_IMAGES -> {
                    return getCachedImageCursor(uri.getQueryParameter(QUERY_PARAM_URL))
                }
                VIRTUAL_TABLE_ID_NOTIFICATIONS -> {
                    val segments = uri.pathSegments
                    if (segments.size == 2) {
                        val def = -1
                        return getNotificationsCursor(NumberUtils.toInt(segments[1], def))
                    } else
                        return notificationsCursor
                }
                VIRTUAL_TABLE_ID_UNREAD_COUNTS -> {
                    val segments = uri.pathSegments
                    if (segments.size == 2) {
                        val def = -1
                        return getUnreadCountsCursor(NumberUtils.toInt(segments[1], def))
                    } else
                        return unreadCountsCursor
                }
                VIRTUAL_TABLE_ID_UNREAD_COUNTS_BY_TYPE -> {
                    val segments = uri.pathSegments
                    if (segments.size != 3) return null
                    return getUnreadCountsCursorByType(segments[2])
                }
                TABLE_ID_DIRECT_MESSAGES_CONVERSATION -> {
                    val segments = uri.pathSegments
                    if (segments.size != 4) return null
                    val accountId = UserKey.valueOf(segments[2])
                    val conversationId = segments[3]
                    val query = ConversationQueryBuilder.buildByConversationId(projection, accountId, conversationId, selection,
                            sortOrder)
                    val c = databaseWrapper!!.rawQuery(query.first.sql, query.second)
                    setNotificationUri(c, DirectMessages.CONTENT_URI)
                    return c
                }
                TABLE_ID_DIRECT_MESSAGES_CONVERSATION_SCREEN_NAME -> {
                    val segments = uri.pathSegments
                    if (segments.size != 4) return null
                    val accountKey = UserKey.valueOf(segments[2])
                    val screenName = segments[3]
                    val query = ConversationQueryBuilder.byScreenName(
                            projection, accountKey, screenName, selection, sortOrder)
                    val c = databaseWrapper!!.rawQuery(query.first.sql, query.second)
                    setNotificationUri(c, DirectMessages.CONTENT_URI)
                    return c
                }
                VIRTUAL_TABLE_ID_CACHED_USERS_WITH_RELATIONSHIP -> {
                    val accountKey = UserKey.valueOf(uri.lastPathSegment)
                    val query = CachedUsersQueryBuilder.withRelationship(projection,
                            selection, selectionArgs, sortOrder, accountKey)
                    val c = databaseWrapper!!.rawQuery(query.first.sql, query.second)
                    setNotificationUri(c, CachedUsers.CONTENT_URI)
                    return c
                }
                VIRTUAL_TABLE_ID_CACHED_USERS_WITH_SCORE -> {
                    val accountKey = UserKey.valueOf(uri.lastPathSegment)
                    val query = CachedUsersQueryBuilder.withScore(projection,
                            selection, selectionArgs, sortOrder, accountKey, 0)
                    val c = databaseWrapper!!.rawQuery(query.first.sql, query.second)
                    setNotificationUri(c, CachedUsers.CONTENT_URI)
                    return c
                }
                VIRTUAL_TABLE_ID_DRAFTS_UNSENT -> {
                    val twitter = mTwitterWrapper
                    val sendingIds = RawItemArray(twitter.sendingDraftIds)
                    val where: Expression
                    if (selection != null) {
                        where = Expression.and(Expression(selection),
                                Expression.notIn(Column(Drafts._ID), sendingIds))
                    } else {
                        where = Expression.and(Expression.notIn(Column(Drafts._ID), sendingIds))
                    }
                    val c = databaseWrapper!!.query(Drafts.TABLE_NAME, projection,
                            where.sql, selectionArgs, null, null, sortOrder)
                    setNotificationUri(c, Utils.getNotificationUri(tableId, uri))
                    return c
                }
                VIRTUAL_TABLE_ID_SUGGESTIONS_AUTO_COMPLETE -> {
                    return getAutoCompleteSuggestionsCursor(uri)
                }
                VIRTUAL_TABLE_ID_SUGGESTIONS_SEARCH -> {
                    return getSearchSuggestionCursor(uri)
                }
                VIRTUAL_TABLE_ID_EMPTY -> {
                    return MatrixCursor(projection)
                }
                VIRTUAL_TABLE_ID_RAW_QUERY -> {
                    if (projection != null || selection != null || sortOrder != null) {
                        throw IllegalArgumentException()
                    }
                    return databaseWrapper!!.rawQuery(uri.lastPathSegment, selectionArgs)
                }
            }
            if (table == null) return null
            val c = databaseWrapper!!.query(table, projection, selection, selectionArgs,
                    null, null, sortOrder)
            setNotificationUri(c, Utils.getNotificationUri(tableId, uri))
            return c
        } catch (e: SQLException) {
            throw IllegalStateException(e)
        }

    }

    private fun getSearchSuggestionCursor(uri: Uri): Cursor? {
        val query = uri.getQueryParameter(QUERY_PARAM_QUERY)
        val accountKey = uri.getQueryParameter(QUERY_PARAM_ACCOUNT_KEY)?.convert(UserKey::valueOf)
        if (query == null || accountKey == null) return null
        val emptyQuery = TextUtils.isEmpty(query)
        val queryEscaped = query.replace("_", "^_")
        val cursors: MutableList<Cursor>
        val historyProjection = arrayOf(Column(SearchHistory._ID, Suggestions.Search._ID).sql, Column("'" + Suggestions.Search.TYPE_SEARCH_HISTORY + "'", Suggestions.Search.TYPE).sql, Column(SearchHistory.QUERY, Suggestions.Search.TITLE).sql, Column(SQLConstants.NULL, Suggestions.Search.SUMMARY).sql, Column(SQLConstants.NULL, Suggestions.Search.ICON).sql, Column("0", Suggestions.Search.EXTRA_ID).sql, Column(SQLConstants.NULL, Suggestions.Search.EXTRA).sql, Column(SearchHistory.QUERY, Suggestions.Search.VALUE).sql)
        val historySelection = Expression.likeRaw(Column(SearchHistory.QUERY), "?||'%'", "^")
        @SuppressLint("Recycle") val historyCursor = databaseWrapper!!.query(true,
                SearchHistory.TABLE_NAME, historyProjection, historySelection.sql,
                arrayOf(queryEscaped), null, null, SearchHistory.DEFAULT_SORT_ORDER,
                if (TextUtils.isEmpty(query)) "3" else "2")
        if (emptyQuery) {
            val savedSearchesProjection = arrayOf(Column(SavedSearches._ID, Suggestions.Search._ID).sql, Column("'" + Suggestions.Search.TYPE_SAVED_SEARCH + "'", Suggestions.Search.TYPE).sql, Column(SavedSearches.QUERY, Suggestions.Search.TITLE).sql, Column(SQLConstants.NULL, Suggestions.Search.SUMMARY).sql, Column(SQLConstants.NULL, Suggestions.Search.ICON).sql, Column("0", Suggestions.Search.EXTRA_ID).sql, Column(SQLConstants.NULL, Suggestions.Search.EXTRA).sql, Column(SavedSearches.QUERY, Suggestions.Search.VALUE).sql)
            val savedSearchesWhere = Expression.equalsArgs(SavedSearches.ACCOUNT_KEY)
            val whereArgs = arrayOf(accountKey.toString())
            @SuppressLint("Recycle") val savedSearchesCursor = databaseWrapper!!.query(true,
                    SavedSearches.TABLE_NAME, savedSearchesProjection, savedSearchesWhere.sql,
                    whereArgs, null, null, SavedSearches.DEFAULT_SORT_ORDER, null)
            cursors = mutableListOf(savedSearchesCursor)
        } else {
            val usersProjection = arrayOf(Column(CachedUsers._ID, Suggestions.Search._ID).sql, Column("'" + Suggestions.Search.TYPE_USER + "'", Suggestions.Search.TYPE).sql, Column(CachedUsers.NAME, Suggestions.Search.TITLE).sql, Column(CachedUsers.SCREEN_NAME, Suggestions.Search.SUMMARY).sql, Column(CachedUsers.PROFILE_IMAGE_URL, Suggestions.Search.ICON).sql, Column(CachedUsers.USER_KEY, Suggestions.Search.EXTRA_ID).sql, Column(SQLConstants.NULL, Suggestions.Search.EXTRA).sql, Column(CachedUsers.SCREEN_NAME, Suggestions.Search.VALUE).sql)
            val queryTrimmed = if (queryEscaped.startsWith("@")) queryEscaped.substring(1) else queryEscaped
            val nicknameKeys = Utils.getMatchedNicknameKeys(query, userColorNameManager)
            val usersSelection = Expression.or(
                    Expression.likeRaw(Column(CachedUsers.SCREEN_NAME), "?||'%'", "^"),
                    Expression.likeRaw(Column(CachedUsers.NAME), "?||'%'", "^"),
                    Expression.inArgs(Column(CachedUsers.USER_KEY), nicknameKeys.size))
            val selectionArgs = arrayOf(queryTrimmed, queryTrimmed, *nicknameKeys)
            val order = arrayOf(CachedUsers.LAST_SEEN, CachedUsers.SCORE, CachedUsers.SCREEN_NAME, CachedUsers.NAME)
            val ascending = booleanArrayOf(false, false, true, true)
            val orderBy = OrderBy(order, ascending)

            val usersQuery = CachedUsersQueryBuilder.withScore(usersProjection,
                    usersSelection.sql, selectionArgs, orderBy.sql, accountKey, 0)
            @SuppressLint("Recycle") val usersCursor = databaseWrapper!!.rawQuery(usersQuery.first.sql, usersQuery.second)
            val exactUserSelection = Expression.or(Expression.likeRaw(Column(CachedUsers.SCREEN_NAME), "?", "^"))
            val exactUserCursor = databaseWrapper!!.query(CachedUsers.TABLE_NAME,
                    arrayOf(SQLFunctions.COUNT()), exactUserSelection.sql,
                    arrayOf(queryTrimmed), null, null, null, "1")
            val hasName = exactUserCursor.moveToPosition(0) && exactUserCursor.getInt(0) > 0
            exactUserCursor.close()
            val screenNameCursor = MatrixCursor(Suggestions.Search.COLUMNS)
            if (!hasName) {
                val m = PATTERN_SCREEN_NAME.matcher(query)
                if (m.matches()) {
                    val screenName = m.group(1)
                    screenNameCursor.addRow(arrayOf(0, Suggestions.Search.TYPE_SCREEN_NAME,
                            screenName, null, null, 0, null, screenName))
                }
            }
            cursors = mutableListOf(screenNameCursor, usersCursor)
        }
        cursors.add(0, historyCursor)
        return MergeCursor(cursors.toTypedArray())
    }

    private fun getAutoCompleteSuggestionsCursor(uri: Uri): Cursor? {
        val query = uri.getQueryParameter(QUERY_PARAM_QUERY)
        val type = uri.getQueryParameter(QUERY_PARAM_TYPE)
        val accountKey = uri.getQueryParameter(QUERY_PARAM_ACCOUNT_KEY)
        if (query == null || type == null) return null
        val queryEscaped = query.replace("_", "^_")
        if (Suggestions.AutoComplete.TYPE_USERS == type) {
            val nicknameKeys = Utils.getMatchedNicknameKeys(query, userColorNameManager!!)
            val where = Expression.or(Expression.likeRaw(Column(CachedUsers.SCREEN_NAME), "?||'%'", "^"),
                    Expression.likeRaw(Column(CachedUsers.NAME), "?||'%'", "^"),
                    Expression.inArgs(Column(CachedUsers.USER_KEY), nicknameKeys.size))
            val whereArgs = arrayOf(queryEscaped, queryEscaped, *nicknameKeys)
            val mappedProjection = arrayOf(Column(CachedUsers._ID, Suggestions._ID).sql, Column("'" + Suggestions.AutoComplete.TYPE_USERS + "'", Suggestions.TYPE).sql, Column(CachedUsers.NAME, Suggestions.TITLE).sql, Column(CachedUsers.SCREEN_NAME, Suggestions.SUMMARY).sql, Column(CachedUsers.USER_KEY, Suggestions.EXTRA_ID).sql, Column(CachedUsers.PROFILE_IMAGE_URL, Suggestions.ICON).sql, Column(CachedUsers.SCREEN_NAME, Suggestions.VALUE).sql)
            val orderBy = arrayOf(CachedUsers.SCORE, CachedUsers.LAST_SEEN, CachedUsers.SCREEN_NAME, CachedUsers.NAME)
            val ascending = booleanArrayOf(false, false, true, true)
            return query(Uri.withAppendedPath(CachedUsers.CONTENT_URI_WITH_SCORE, accountKey),
                    mappedProjection, where.sql, whereArgs, OrderBy(orderBy, ascending).sql)
        } else if (Suggestions.AutoComplete.TYPE_HASHTAGS == type) {
            val where = Expression.likeRaw(Column(CachedHashtags.NAME), "?||'%'", "^")
            val whereArgs = arrayOf(queryEscaped)
            val mappedProjection = arrayOf(Column(CachedHashtags._ID, Suggestions._ID).sql, Column("'" + Suggestions.AutoComplete.TYPE_HASHTAGS + "'", Suggestions.TYPE).sql, Column(CachedHashtags.NAME, Suggestions.TITLE).sql, Column("NULL", Suggestions.SUMMARY).sql, Column("0", Suggestions.EXTRA_ID).sql, Column("NULL", Suggestions.ICON).sql, Column(CachedHashtags.NAME, Suggestions.VALUE).sql)
            return query(CachedHashtags.CONTENT_URI, mappedProjection, where.sql,
                    whereArgs, null)
        }
        return null
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        try {
            return updateInternal(uri, values, selection, selectionArgs)
        } catch (e: SQLException) {
            if (handleSQLException(e)) {
                try {
                    return updateInternal(uri, values, selection, selectionArgs)
                } catch (e1: SQLException) {
                    throw IllegalStateException(e1)
                }

            }
            throw IllegalStateException(e)
        }

    }

    private fun updateInternal(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        val tableId = DataStoreUtils.getTableId(uri)
        val table = DataStoreUtils.getTableNameById(tableId)
        checkWritePermission(tableId, table)
        var result = 0
        if (table != null) {
            when (tableId) {
                TABLE_ID_DIRECT_MESSAGES_CONVERSATION, TABLE_ID_DIRECT_MESSAGES, TABLE_ID_DIRECT_MESSAGES_CONVERSATIONS_ENTRIES -> return 0
            }
            result = databaseWrapper!!.update(table, values, selection, selectionArgs)
        }
        if (result > 0) {
            onDatabaseUpdated(tableId, uri)
        }
        return result
    }

    private fun checkPermission(vararg permissions: String): Boolean {
        return permissionsManager.checkCallingPermission(*permissions)
    }

    private fun checkReadPermission(id: Int, table: String, projection: Array<String>?) {
        if (Binder.getCallingPid() == Process.myPid()) return
        when (id) {
            VIRTUAL_TABLE_ID_PERMISSIONS -> {
                return
            }
            VIRTUAL_TABLE_ID_PREFERENCES, VIRTUAL_TABLE_ID_DNS -> {
                if (!checkPermission(PERMISSION_PREFERENCES))
                    throw SecurityException("Access preferences requires level PERMISSION_LEVEL_PREFERENCES")
            }
            TABLE_ID_ACCOUNTS -> {
                // Reading some information like user_id, screen_name etc is
                // okay, but reading columns like password requires higher
                // permission level.
                if (checkPermission(PERMISSION_ACCOUNTS)) {
                    return
                }
                // Only querying basic information
                if (TwidereArrayUtils.contains(Accounts.COLUMNS_NO_CREDENTIALS, projection) && !checkPermission(PERMISSION_READ)) {
                    val pkgName = permissionsManager.getPackageNameByUid(Binder.getCallingUid())
                    throw SecurityException("Access database $table requires level PERMISSION_LEVEL_READ, package: $pkgName")
                }
                val pkgName = permissionsManager.getPackageNameByUid(Binder.getCallingUid())
                val callingSensitiveCols = ArrayList<String>()
                if (projection != null) {
                    Collections.addAll(callingSensitiveCols, *projection)
                    callingSensitiveCols.removeAll(Arrays.asList(*Accounts.COLUMNS_NO_CREDENTIALS))
                } else {
                    callingSensitiveCols.add("*")
                }
                throw SecurityException("Access column ${callingSensitiveCols.joinToString(",")} in " +
                        "database accounts requires level PERMISSION_LEVEL_ACCOUNTS, package: $pkgName")
            }
            TABLE_ID_DIRECT_MESSAGES, TABLE_ID_DIRECT_MESSAGES_INBOX, TABLE_ID_DIRECT_MESSAGES_OUTBOX, TABLE_ID_DIRECT_MESSAGES_CONVERSATION, TABLE_ID_DIRECT_MESSAGES_CONVERSATION_SCREEN_NAME, TABLE_ID_DIRECT_MESSAGES_CONVERSATIONS_ENTRIES -> {
                if (!checkPermission(PERMISSION_DIRECT_MESSAGES))
                    throw SecurityException("Access database " + table
                            + " requires level PERMISSION_LEVEL_DIRECT_MESSAGES")
            }
            TABLE_ID_STATUSES, TABLE_ID_MENTIONS, TABLE_ID_TABS, TABLE_ID_DRAFTS, TABLE_ID_CACHED_USERS, TABLE_ID_FILTERED_USERS, TABLE_ID_FILTERED_KEYWORDS, TABLE_ID_FILTERED_SOURCES, TABLE_ID_FILTERED_LINKS, TABLE_ID_TRENDS_LOCAL, TABLE_ID_CACHED_STATUSES, TABLE_ID_CACHED_HASHTAGS -> {
                if (!checkPermission(PERMISSION_READ))
                    throw SecurityException("Access database $table requires level PERMISSION_LEVEL_READ")
            }
            else -> {
                if (!permissionsManager.checkSignature(Binder.getCallingUid())) {
                    throw SecurityException("Internal database $id is not allowed for third-party applications")
                }
            }
        }
    }

    private fun checkWritePermission(id: Int, table: String) {
        if (Binder.getCallingPid() == Process.myPid()) return
        when (id) {
            TABLE_ID_ACCOUNTS -> {
                // Writing to accounts database is not allowed for third-party
                // applications.
                if (!permissionsManager.checkSignature(Binder.getCallingUid()))
                    throw SecurityException(
                            "Writing to accounts database is not allowed for third-party applications")
            }
            TABLE_ID_DIRECT_MESSAGES, TABLE_ID_DIRECT_MESSAGES_INBOX, TABLE_ID_DIRECT_MESSAGES_OUTBOX, TABLE_ID_DIRECT_MESSAGES_CONVERSATION, TABLE_ID_DIRECT_MESSAGES_CONVERSATION_SCREEN_NAME, TABLE_ID_DIRECT_MESSAGES_CONVERSATIONS_ENTRIES -> {
                if (!checkPermission(PERMISSION_DIRECT_MESSAGES))
                    throw SecurityException("Access database " + table
                            + " requires level PERMISSION_LEVEL_DIRECT_MESSAGES")
            }
            TABLE_ID_STATUSES, TABLE_ID_MENTIONS, TABLE_ID_TABS, TABLE_ID_DRAFTS, TABLE_ID_CACHED_USERS, TABLE_ID_FILTERED_USERS, TABLE_ID_FILTERED_KEYWORDS, TABLE_ID_FILTERED_SOURCES, TABLE_ID_FILTERED_LINKS, TABLE_ID_TRENDS_LOCAL, TABLE_ID_CACHED_STATUSES, TABLE_ID_CACHED_HASHTAGS -> {
                if (!checkPermission(PERMISSION_WRITE))
                    throw SecurityException("Access database $table requires level PERMISSION_LEVEL_WRITE")
            }
            else -> {
                if (!permissionsManager.checkSignature(Binder.getCallingUid())) {
                    throw SecurityException("Internal database is not allowed for third-party applications")
                }
            }
        }
    }

    private fun clearNotification() {
        notificationManager.cancelAll()
    }

    private fun clearNotification(notificationType: Int, accountId: UserKey?) {
        notificationManager.cancelById(Utils.getNotificationId(notificationType, accountId))
    }

    private fun getCachedImageCursor(url: String?): Cursor {
        if (BuildConfig.DEBUG) {
            Log.d(LOGTAG, String.format("getCachedImageCursor(%s)", url))
        }
        val c = MatrixCursor(CachedImages.MATRIX_COLUMNS)
        val file = mImagePreloader!!.getCachedImageFile(url)
        if (url != null && file != null) {
            c.addRow(arrayOf(url, file.path))
        }
        return c
    }

    @Throws(FileNotFoundException::class)
    private fun getCachedImageFd(url: String): ParcelFileDescriptor? {
        if (BuildConfig.DEBUG) {
            Log.d(LOGTAG, String.format("getCachedImageFd(%s)", url))
        }
        val file = mImagePreloader!!.getCachedImageFile(url) ?: return null
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    @Throws(FileNotFoundException::class)
    private fun getCacheFileFd(name: String?): ParcelFileDescriptor? {
        if (name == null) return null
        val context = context!!
        val cacheDir = context.cacheDir
        val file = File(cacheDir, name)
        if (!file.exists()) return null
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private val contentResolver: ContentResolver?
        get() = context?.contentResolver

    private fun getDNSCursor(host: String): Cursor {
        val c = MatrixCursor(DNS.MATRIX_COLUMNS)
        try {
            val addresses = dns.lookup(host)
            for (address in addresses) {
                c.addRow(arrayOf(host, address.hostAddress))
            }
        } catch (ignore: IOException) {
            if (BuildConfig.DEBUG) {
                Log.w(LOGTAG, ignore)
            }
        }

        return c
    }

    private val notificationsCursor: Cursor
        get() {
            val c = MatrixCursor(Notifications.MATRIX_COLUMNS)
            return c
        }

    private fun getNotificationsCursor(id: Int): Cursor {
        val c = MatrixCursor(Notifications.MATRIX_COLUMNS)
        return c
    }

    private fun getProfileImageForNotification(profileImageUrl: String): Bitmap {
        val context = context!!
        val res = context.resources
        val w = res.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
        val h = res.getDimensionPixelSize(android.R.dimen.notification_large_icon_height)
        val profile_image_file = mImagePreloader!!.getCachedImageFile(profileImageUrl)
        val profile_image = if (profile_image_file != null && profile_image_file.isFile)
            BitmapFactory.decodeFile(profile_image_file.path)
        else
            null
        if (profile_image != null) return Bitmap.createScaledBitmap(profile_image, w, h, true)
        return Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.mipmap.ic_launcher), w, h, true)
    }

    private val unreadCountsCursor: Cursor
        get() {
            val c = MatrixCursor(UnreadCounts.MATRIX_COLUMNS)
            return c
        }

    private fun getUnreadCountsCursor(position: Int): Cursor {
        val c = MatrixCursor(UnreadCounts.MATRIX_COLUMNS)

        return c
    }

    private fun getUnreadCountsCursorByType(type: String): Cursor {
        val c = MatrixCursor(UnreadCounts.MATRIX_COLUMNS)
        return c
    }

    private val isNotificationAudible: Boolean
        get() = !activityTracker.isHomeActivityStarted

    private fun notifyContentObserver(uri: Uri) {
        if (!uri.getBooleanQueryParameter(QUERY_PARAM_NOTIFY, true)) return
        mHandler!!.post(Runnable {
            val cr = contentResolver ?: return@Runnable
            cr.notifyChange(uri, null)
        })
    }

    private fun notifyUnreadCountChanged(position: Int) {
        mHandler!!.post { bus.post(UnreadCountUpdatedEvent(position)) }
        notifyContentObserver(UnreadCounts.CONTENT_URI)
    }

    private fun onDatabaseUpdated(tableId: Int, uri: Uri?) {
        if (uri == null) return
        when (tableId) {
            TABLE_ID_ACCOUNTS -> {
                DataStoreUtils.clearAccountName()
            }
        }
        notifyContentObserver(Utils.getNotificationUri(tableId, uri))

    }

    private fun onNewItemsInserted(uri: Uri, tableId: Int, values: ContentValues) {
        onNewItemsInserted(uri, tableId, arrayOf(values))
    }

    private fun onNewItemsInserted(uri: Uri?, tableId: Int, valuesArray: Array<ContentValues>?) {
        val context = context
        if (uri == null || valuesArray == null || valuesArray.size == 0 || context == null)
            return
        preloadMedia(*valuesArray)
        when (tableId) {
            TABLE_ID_STATUSES -> {
                mBackgroundExecutor!!.execute {
                    val prefs = AccountPreferences.getNotificationEnabledPreferences(context,
                            DataStoreUtils.getAccountKeys(context))
                    for (pref in prefs) {
                        if (!pref.isHomeTimelineNotificationEnabled) continue
                        val positionTag = getPositionTag(CustomTabType.HOME_TIMELINE, pref.accountKey)
                        showTimelineNotification(pref, positionTag)
                    }
                    notifyUnreadCountChanged(NOTIFICATION_ID_HOME_TIMELINE)
                }
            }
            TABLE_ID_ACTIVITIES_ABOUT_ME -> {
                mBackgroundExecutor!!.execute {
                    val prefs = AccountPreferences.getNotificationEnabledPreferences(context,
                            DataStoreUtils.getAccountKeys(context))
                    val combined = preferences!!.getBoolean(KEY_COMBINED_NOTIFICATIONS)
                    for (pref in prefs) {
                        if (!pref.isInteractionsNotificationEnabled) continue
                        showInteractionsNotification(pref, getPositionTag(ReadPositionTag.ACTIVITIES_ABOUT_ME,
                                pref.accountKey), combined)
                    }
                    notifyUnreadCountChanged(NOTIFICATION_ID_INTERACTIONS_TIMELINE)
                }
            }
            TABLE_ID_DIRECT_MESSAGES_INBOX -> {
                val prefs = AccountPreferences.getNotificationEnabledPreferences(context,
                        DataStoreUtils.getAccountKeys(context))
                for (pref in prefs) {
                    if (!pref.isDirectMessagesNotificationEnabled) continue
                    val pairs = readStateManager.getPositionPairs(CustomTabType.DIRECT_MESSAGES)
                    showMessagesNotification(pref, pairs, valuesArray)
                }
                notifyUnreadCountChanged(NOTIFICATION_ID_DIRECT_MESSAGES)
            }
            TABLE_ID_DRAFTS -> {
            }
        }
    }

    private fun getPositionTag(tag: String, accountKey: UserKey): Long {
        val position = readStateManager.getPosition(Utils.getReadPositionTagWithAccount(tag,
                accountKey))
        if (position != -1L) return position
        return readStateManager.getPosition(tag)
    }

    private fun showTimelineNotification(pref: AccountPreferences, position: Long) {
        val accountKey = pref.accountKey
        val context = context ?: return
        val resources = context.resources
        val nm = notificationManager
        val selection = Expression.and(Expression.equalsArgs(AccountSupportColumns.ACCOUNT_KEY),
                Expression.greaterThan(Statuses.STATUS_ID, position))
        val filteredSelection = DataStoreUtils.buildStatusFilterWhereClause(Statuses.TABLE_NAME,
                selection).sql
        val selectionArgs = arrayOf(accountKey.toString())
        val userProjection = arrayOf(Statuses.USER_KEY, Statuses.USER_NAME, Statuses.USER_SCREEN_NAME)
        val statusProjection = arrayOf(Statuses.STATUS_ID)
        val statusCursor = databaseWrapper!!.query(Statuses.TABLE_NAME, statusProjection,
                filteredSelection, selectionArgs, null, null, Statuses.DEFAULT_SORT_ORDER)
        val userCursor = databaseWrapper!!.query(Statuses.TABLE_NAME, userProjection,
                filteredSelection, selectionArgs, Statuses.USER_KEY, null, Statuses.DEFAULT_SORT_ORDER)
        //noinspection TryFinallyCanBeTryWithResources
        try {
            val usersCount = userCursor.count
            val statusesCount = statusCursor.count
            if (statusesCount == 0 || usersCount == 0) return
            val idxStatusId = statusCursor.getColumnIndex(Statuses.STATUS_ID)
            val idxUserName = userCursor.getColumnIndex(Statuses.USER_NAME)
            val idxUserScreenName = userCursor.getColumnIndex(Statuses.USER_NAME)
            val idxUserId = userCursor.getColumnIndex(Statuses.USER_NAME)
            val statusId = if (statusCursor.moveToFirst()) statusCursor.getLong(idxStatusId) else -1
            val notificationTitle = resources.getQuantityString(R.plurals.N_new_statuses,
                    statusesCount, statusesCount)
            val notificationContent: String
            userCursor.moveToFirst()
            val displayName = userColorNameManager.getDisplayName(userCursor.getString(idxUserId),
                    userCursor.getString(idxUserName), userCursor.getString(idxUserScreenName),
                    mNameFirst)
            if (usersCount == 1) {
                notificationContent = context.getString(R.string.from_name, displayName)
            } else if (usersCount == 2) {
                userCursor.moveToPosition(1)
                val othersName = userColorNameManager.getDisplayName(userCursor.getString(idxUserId),
                        userCursor.getString(idxUserName), userCursor.getString(idxUserScreenName),
                        mNameFirst)
                notificationContent = resources.getString(R.string.from_name_and_name, displayName, othersName)
            } else {
                notificationContent = resources.getString(R.string.from_name_and_N_others, displayName, usersCount - 1)
            }

            // Setup notification
            val builder = NotificationCompat.Builder(context)
            builder.setAutoCancel(true)
            builder.setSmallIcon(R.drawable.ic_stat_twitter)
            builder.setTicker(notificationTitle)
            builder.setContentTitle(notificationTitle)
            builder.setContentText(notificationContent)
            builder.setCategory(NotificationCompat.CATEGORY_SOCIAL)
            builder.setContentIntent(getContentIntent(context, CustomTabType.HOME_TIMELINE,
                    NotificationType.HOME_TIMELINE, accountKey, statusId))
            builder.setDeleteIntent(getMarkReadDeleteIntent(context, NotificationType.HOME_TIMELINE,
                    accountKey, statusId, false))
            builder.setNumber(statusesCount)
            builder.setCategory(NotificationCompat.CATEGORY_SOCIAL)
            applyNotificationPreferences(builder, pref, pref.homeTimelineNotificationType)
            try {
                nm.notify("home_" + accountKey, Utils.getNotificationId(NOTIFICATION_ID_HOME_TIMELINE, accountKey), builder.build())
                Utils.sendPebbleNotification(context, notificationContent)
            } catch (e: SecurityException) {
                // Silently ignore
            }

        } finally {
            statusCursor.close()
            userCursor.close()
        }
    }

    private fun showInteractionsNotification(pref: AccountPreferences, position: Long, combined: Boolean) {
        val context = context ?: return
        val db = databaseWrapper!!.sqliteDatabase
        val accountKey = pref.accountKey
        val where = Expression.and(
                Expression.equalsArgs(AccountSupportColumns.ACCOUNT_KEY),
                Expression.greaterThanArgs(Activities.TIMESTAMP)).sql
        val whereArgs = arrayOf(accountKey.toString(), position.toString())
        val c = query(Activities.AboutMe.CONTENT_URI, Activities.COLUMNS, where, whereArgs,
                OrderBy(Activities.TIMESTAMP, false).sql) ?: return
        val builder = NotificationCompat.Builder(context)
        val pebbleNotificationStringBuilder = StringBuilder()
        try {
            val count = c.count
            if (count == 0) return
            builder.setSmallIcon(R.drawable.ic_stat_notification)
            builder.setCategory(NotificationCompat.CATEGORY_SOCIAL)
            applyNotificationPreferences(builder, pref, pref.mentionsNotificationType)

            val resources = context.resources
            val accountName = DataStoreUtils.getAccountDisplayName(context, accountKey, mNameFirst)
            builder.setContentText(accountName)
            val style = InboxStyle()
            builder.setStyle(style)
            builder.setAutoCancel(true)
            style.setSummaryText(accountName)
            val ci = ParcelableActivityCursorIndices(c)
            var messageLines = 0

            var timestamp: Long = -1
            c.moveToPosition(-1)
            while (c.moveToNext()) {
                if (messageLines == 5) {
                    style.addLine(resources.getString(R.string.and_N_more, count - c.position))
                    pebbleNotificationStringBuilder.append(resources.getString(R.string.and_N_more, count - c.position))
                    break
                }
                val activity = ci.newObject(c)
                if (pref.isNotificationMentionsOnly && !ArrayUtils.contains(Activity.Action.MENTION_ACTIONS,
                        activity.action)) {
                    continue
                }
                if (activity.status_id != null && InternalTwitterContentUtils.isFiltered(db,
                        activity.status_user_key, activity.status_text_plain,
                        activity.status_quote_text_plain, activity.status_spans,
                        activity.status_quote_spans, activity.status_source,
                        activity.status_quote_source, activity.status_retweeted_by_user_key,
                        activity.status_quoted_user_key)) {
                    continue
                }
                val filteredUserIds = DataStoreUtils.getFilteredUserIds(context)
                if (timestamp == -1L) {
                    timestamp = activity.timestamp
                }
                ParcelableActivityUtils.initAfterFilteredSourceIds(activity, filteredUserIds,
                        pref.isNotificationFollowingOnly)
                val sources = ParcelableActivityUtils.getAfterFilteredSources(activity)
                if (ArrayUtils.isEmpty(sources)) continue
                val message = ActivityTitleSummaryMessage.get(context,
                        userColorNameManager, activity, sources,
                        0, false, mUseStarForLikes, mNameFirst)
                if (message != null) {
                    val summary = message.summary
                    if (TextUtils.isEmpty(summary)) {
                        style.addLine(message.title)
                        pebbleNotificationStringBuilder.append(message.title)
                        pebbleNotificationStringBuilder.append("\n")
                    } else {
                        style.addLine(SpanFormatter.format(resources.getString(R.string.title_summary_line_format),
                                message.title, summary))
                        pebbleNotificationStringBuilder.append(message.title)
                        pebbleNotificationStringBuilder.append(": ")
                        pebbleNotificationStringBuilder.append(message.summary)
                        pebbleNotificationStringBuilder.append("\n")
                    }
                    messageLines++
                }
            }
            if (messageLines == 0) return
            val displayCount = messageLines + count - c.position
            val title = resources.getQuantityString(R.plurals.N_new_interactions,
                    displayCount, displayCount)
            builder.setContentTitle(title)
            style.setBigContentTitle(title)
            builder.setNumber(displayCount)
            builder.setContentIntent(getContentIntent(context, CustomTabType.NOTIFICATIONS_TIMELINE,
                    NotificationType.INTERACTIONS, accountKey, timestamp))
            if (timestamp != -1L) {
                builder.setDeleteIntent(getMarkReadDeleteIntent(context,
                        NotificationType.INTERACTIONS, accountKey, timestamp, false))
            }
        } finally {
            c.close()
        }
        val notificationId = Utils.getNotificationId(NOTIFICATION_ID_INTERACTIONS_TIMELINE,
                accountKey)
        notificationManager.notify("interactions", notificationId, builder.build())

        Utils.sendPebbleNotification(context, context.resources.getString(R.string.interactions), pebbleNotificationStringBuilder.toString())

    }

    private fun getContentIntent(context: Context, @CustomTabType type: String,
                                 @NotificationType notificationType: String,
                                 accountKey: UserKey?, readPosition: Long): PendingIntent {
        // Setup click intent
        val homeIntent = Intent(context, HomeActivity::class.java)
        val homeLinkBuilder = Uri.Builder()
        homeLinkBuilder.scheme(SCHEME_TWIDERE)
        homeLinkBuilder.authority(type)
        if (accountKey != null)
            homeLinkBuilder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        homeLinkBuilder.appendQueryParameter(QUERY_PARAM_FROM_NOTIFICATION, true.toString())
        homeLinkBuilder.appendQueryParameter(QUERY_PARAM_TIMESTAMP, System.currentTimeMillis().toString())
        homeLinkBuilder.appendQueryParameter(QUERY_PARAM_NOTIFICATION_TYPE, notificationType)
        if (readPosition > 0) {
            homeLinkBuilder.appendQueryParameter(QUERY_PARAM_READ_POSITION, readPosition.toString())
        }
        homeIntent.data = homeLinkBuilder.build()
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return PendingIntent.getActivity(context, 0, homeIntent, 0)
    }

    private fun applyNotificationPreferences(builder: NotificationCompat.Builder, pref: AccountPreferences, defaultFlags: Int) {
        var notificationDefaults = 0
        if (AccountPreferences.isNotificationHasLight(defaultFlags)) {
            notificationDefaults = notificationDefaults or NotificationCompat.DEFAULT_LIGHTS
        }
        if (isNotificationAudible) {
            if (AccountPreferences.isNotificationHasVibration(defaultFlags)) {
                notificationDefaults = notificationDefaults or NotificationCompat.DEFAULT_VIBRATE
            } else {
                notificationDefaults = notificationDefaults and NotificationCompat.DEFAULT_VIBRATE.inv()
            }
            if (AccountPreferences.isNotificationHasRingtone(defaultFlags)) {
                builder.setSound(pref.notificationRingtone, AudioManager.STREAM_NOTIFICATION)
            }
        } else {
            notificationDefaults = notificationDefaults and (NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_SOUND).inv()
        }
        builder.setColor(pref.notificationLightColor)
        builder.setDefaults(notificationDefaults)
        builder.setOnlyAlertOnce(true)
    }

    private fun showMessagesNotification(pref: AccountPreferences, pairs: Array<StringLongPair>, valuesArray: Array<ContentValues>) {
        val context = context!!
        val accountKey = pref.accountKey
        val prevOldestId = readStateManager.getPosition(TAG_OLDEST_MESSAGES,
                accountKey.toString())
        var oldestId: Long = -1
        for (contentValues in valuesArray) {
            val messageId = contentValues.getAsLong(DirectMessages.MESSAGE_ID)!!
            oldestId = if (oldestId < 0) messageId else Math.min(oldestId, messageId)
            if (messageId <= prevOldestId) return
        }
        readStateManager.setPosition(TAG_OLDEST_MESSAGES, accountKey.toString(), oldestId,
                false)
        val resources = context.resources
        val nm = notificationManager
        val orExpressions = ArrayList<Expression>()
        val prefix = "$accountKey-"
        val prefixLength = prefix.length
        val senderIds = CompactHashSet<String>()
        val whereArgs = ArrayList<String>()
        for (pair in pairs) {
            val key = pair.key
            if (key.startsWith(prefix)) {
                val senderId = key.substring(prefixLength)
                senderIds.add(senderId)
                val expression = Expression.and(
                        Expression.equalsArgs(DirectMessages.SENDER_ID),
                        Expression.greaterThanArgs(DirectMessages.MESSAGE_ID))
                whereArgs.add(senderId)
                whereArgs.add(pair.value.toString())
                orExpressions.add(expression)
            }
        }
        orExpressions.add(Expression.notIn(Column(DirectMessages.SENDER_ID), ArgsArray(senderIds.size)))
        whereArgs.addAll(senderIds)
        val selection = Expression.and(
                Expression.equalsArgs(AccountSupportColumns.ACCOUNT_KEY),
                Expression.greaterThanArgs(DirectMessages.MESSAGE_ID),
                Expression.or(*orExpressions.toTypedArray()))
        whereArgs.add(accountKey.toString())
        whereArgs.add(prevOldestId.toString())
        val filteredSelection = selection.sql
        val selectionArgs = whereArgs.toTypedArray()
        val userProjection = arrayOf(DirectMessages.SENDER_ID, DirectMessages.SENDER_NAME, DirectMessages.SENDER_SCREEN_NAME)
        val messageProjection = arrayOf(DirectMessages.MESSAGE_ID, DirectMessages.SENDER_ID, DirectMessages.SENDER_NAME, DirectMessages.SENDER_SCREEN_NAME, DirectMessages.TEXT_UNESCAPED, DirectMessages.MESSAGE_TIMESTAMP)
        val messageCursor = databaseWrapper!!.query(DirectMessages.Inbox.TABLE_NAME,
                messageProjection, filteredSelection, selectionArgs, null, null,
                DirectMessages.DEFAULT_SORT_ORDER)
        val userCursor = databaseWrapper!!.query(DirectMessages.Inbox.TABLE_NAME,
                userProjection, filteredSelection, selectionArgs, DirectMessages.SENDER_ID, null,
                DirectMessages.DEFAULT_SORT_ORDER)

        val pebbleNotificationBuilder = StringBuilder()

        //noinspection TryFinallyCanBeTryWithResources
        try {
            val usersCount = userCursor.count
            val messagesCount = messageCursor.count
            if (messagesCount == 0 || usersCount == 0) return
            val accountName = DataStoreUtils.getAccountName(context, accountKey)
            val accountScreenName = DataStoreUtils.getAccountScreenName(context, accountKey)
            val messageIndices = ParcelableDirectMessageCursorIndices(messageCursor)
            val idxUserName = userCursor.getColumnIndex(DirectMessages.SENDER_NAME)
            val idxUserScreenName = userCursor.getColumnIndex(DirectMessages.SENDER_NAME)
            val idxUserId = userCursor.getColumnIndex(DirectMessages.SENDER_NAME)

            val notificationTitle = resources.getQuantityString(R.plurals.N_new_messages,
                    messagesCount, messagesCount)
            val notificationContent: String
            userCursor.moveToFirst()
            val displayName = userColorNameManager!!.getUserNickname(userCursor.getString(idxUserId),
                    if (mNameFirst) userCursor.getString(idxUserName) else userCursor.getString(idxUserScreenName))
            if (usersCount == 1) {
                if (messagesCount == 1) {
                    notificationContent = context.getString(R.string.notification_direct_message, displayName)
                } else {
                    notificationContent = context.getString(R.string.notification_direct_message_multiple_messages,
                            displayName, messagesCount)
                }
            } else {
                notificationContent = context.getString(R.string.notification_direct_message_multiple_users,
                        displayName, usersCount - 1, messagesCount)
            }

            val idsMap = LongSparseArray<Long>()
            // Add rich notification and get latest tweet timestamp
            var `when`: Long = -1
            val style = InboxStyle()
            run {
                var i = 0
                while (messageCursor.moveToPosition(i) && i < messagesCount) {
                    if (`when` < 0) {
                        `when` = messageCursor.getLong(messageIndices.timestamp)
                    }
                    if (i < 5) {
                        val sb = SpannableStringBuilder()
                        sb.append(userColorNameManager!!.getUserNickname(messageCursor.getString(idxUserId),
                                if (mNameFirst)
                                    messageCursor.getString(messageIndices.sender_name)
                                else
                                    messageCursor.getString(messageIndices.sender_screen_name)))
                        sb.setSpan(StyleSpan(Typeface.BOLD), 0, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        sb.append(' ')
                        sb.append(messageCursor.getString(messageIndices.text_unescaped))
                        style.addLine(sb)
                        pebbleNotificationBuilder.append(userColorNameManager!!.getUserNickname(messageCursor.getString(idxUserId),
                                if (mNameFirst)
                                    messageCursor.getString(messageIndices.sender_name)
                                else
                                    messageCursor.getString(messageIndices.sender_screen_name)))
                        pebbleNotificationBuilder.append(": ")
                        pebbleNotificationBuilder.append(messageCursor.getString(messageIndices.text_unescaped))
                        pebbleNotificationBuilder.append("\n")
                    }
                    val userId = messageCursor.getLong(messageIndices.sender_id)
                    val messageId = messageCursor.getLong(messageIndices.id)
                    idsMap.put(userId, Math.max(idsMap.get(userId, -1L), messageId))
                    i++
                }
            }
            if (mNameFirst) {
                style.setSummaryText(accountName)
            } else {
                style.setSummaryText("@" + accountScreenName)
            }
            val positions = Array<StringLongPair>(idsMap.size()) { i ->
                StringLongPair(idsMap.keyAt(i).toString(), idsMap.valueAt(i))
            }

            // Setup notification
            val builder = NotificationCompat.Builder(context)
            builder.setAutoCancel(true)
            builder.setSmallIcon(R.drawable.ic_stat_message)
            builder.setTicker(notificationTitle)
            builder.setContentTitle(notificationTitle)
            builder.setContentText(notificationContent)
            builder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
            builder.setContentIntent(getContentIntent(context, CustomTabType.DIRECT_MESSAGES,
                    NotificationType.DIRECT_MESSAGES, accountKey, -1))
            builder.setDeleteIntent(getMarkReadDeleteIntent(context,
                    NotificationType.DIRECT_MESSAGES, accountKey, positions))
            builder.setNumber(messagesCount)
            builder.setWhen(`when`)
            builder.setStyle(style)
            builder.setColor(pref.notificationLightColor)
            applyNotificationPreferences(builder, pref, pref.directMessagesNotificationType)
            try {
                nm.notify("messages_" + accountKey, NOTIFICATION_ID_DIRECT_MESSAGES, builder.build())

                //TODO: Pebble notification - Only notify about recently added DMs, not previous ones?
                Utils.sendPebbleNotification(context, "DM", pebbleNotificationBuilder.toString())
            } catch (e: SecurityException) {
                // Silently ignore
            }

        } finally {
            messageCursor.close()
            userCursor.close()
        }
    }

    private fun preloadMedia(vararg values: ContentValues) {
        val preloadProfileImages = preferences.getBoolean(SharedPreferenceConstants.KEY_PRELOAD_PROFILE_IMAGES, false)
        val preloadPreviewMedia = preferences.getBoolean(SharedPreferenceConstants.KEY_PRELOAD_PREVIEW_IMAGES, false)
        for (v in values) {
            if (preloadProfileImages) {
                mImagePreloader!!.preloadImage(v.getAsString(Statuses.USER_PROFILE_IMAGE_URL))
                mImagePreloader!!.preloadImage(v.getAsString(DirectMessages.SENDER_PROFILE_IMAGE_URL))
                mImagePreloader!!.preloadImage(v.getAsString(DirectMessages.RECIPIENT_PROFILE_IMAGE_URL))
            }
            if (preloadPreviewMedia) {
                preloadSpans(JsonSerializer.parseList(v.getAsString(Statuses.SPANS), SpanItem::class.java))
                preloadSpans(JsonSerializer.parseList(v.getAsString(Statuses.QUOTED_SPANS), SpanItem::class.java))
            }
        }
    }

    private fun preloadSpans(spans: List<SpanItem>?) {
        if (spans == null) return
        for (span in spans) {
            if (span.link == null) continue
            if (PreviewMediaExtractor.isSupported(span.link)) {
                mImagePreloader!!.preloadImage(span.link)
            }
        }
    }

    private fun setNotificationUri(c: Cursor?, uri: Uri?) {
        val cr = contentResolver
        if (cr == null || c == null || uri == null) return
        c.setNotificationUri(cr, uri)
    }

    private fun updatePreferences() {
        mNameFirst = preferences!!.getBoolean(SharedPreferenceConstants.KEY_NAME_FIRST)
        mUseStarForLikes = preferences!!.getBoolean(SharedPreferenceConstants.KEY_I_WANT_MY_STARS_BACK)
    }

    companion object {

        val TAG_OLDEST_MESSAGES = "oldest_messages"
        private val PATTERN_SCREEN_NAME = Pattern.compile("(?i)[@\uFF20]?([a-z0-9_]{1,20})")

        private fun getMarkReadDeleteIntent(context: Context, @NotificationType type: String,
                                            accountKey: UserKey?, position: Long,
                                            extraUserFollowing: Boolean): PendingIntent {
            return getMarkReadDeleteIntent(context, type, accountKey, position, -1, -1, extraUserFollowing)
        }

        private fun getMarkReadDeleteIntent(context: Context, @NotificationType type: String,
                                            accountKey: UserKey?, position: Long,
                                            extraId: Long, extraUserId: Long,
                                            extraUserFollowing: Boolean): PendingIntent {
            // Setup delete intent
            val intent = Intent(context, NotificationReceiver::class.java)
            intent.action = IntentConstants.BROADCAST_NOTIFICATION_DELETED
            val linkBuilder = Uri.Builder()
            linkBuilder.scheme(SCHEME_TWIDERE)
            linkBuilder.authority(AUTHORITY_INTERACTIONS)
            linkBuilder.appendPath(type)
            if (accountKey != null) {
                linkBuilder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
            }
            linkBuilder.appendQueryParameter(QUERY_PARAM_READ_POSITION, position.toString())
            linkBuilder.appendQueryParameter(QUERY_PARAM_TIMESTAMP, System.currentTimeMillis().toString())
            linkBuilder.appendQueryParameter(QUERY_PARAM_NOTIFICATION_TYPE, type)

            UriExtraUtils.addExtra(linkBuilder, "item_id", extraId)
            UriExtraUtils.addExtra(linkBuilder, "item_user_id", extraUserId)
            UriExtraUtils.addExtra(linkBuilder, "item_user_following", extraUserFollowing)
            intent.data = linkBuilder.build()
            return PendingIntent.getBroadcast(context, 0, intent, 0)
        }

        private fun getMarkReadDeleteIntent(context: Context, @NotificationType notificationType: String,
                                            accountKey: UserKey?, positions: Array<StringLongPair>): PendingIntent {
            // Setup delete intent
            val intent = Intent(context, NotificationReceiver::class.java)
            val linkBuilder = Uri.Builder()
            linkBuilder.scheme(SCHEME_TWIDERE)
            linkBuilder.authority(AUTHORITY_INTERACTIONS)
            linkBuilder.appendPath(notificationType)
            if (accountKey != null) {
                linkBuilder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
            }
            linkBuilder.appendQueryParameter(QUERY_PARAM_READ_POSITIONS, StringLongPair.toString(positions))
            linkBuilder.appendQueryParameter(QUERY_PARAM_TIMESTAMP, System.currentTimeMillis().toString())
            linkBuilder.appendQueryParameter(QUERY_PARAM_NOTIFICATION_TYPE, notificationType)
            intent.data = linkBuilder.build()
            return PendingIntent.getBroadcast(context, 0, intent, 0)
        }

        private fun getPreferencesCursor(preferences: SharedPreferencesWrapper, key: String?): Cursor {
            val c = MatrixCursor(Preferences.MATRIX_COLUMNS)
            val map = HashMap<String, Any?>()
            val all = preferences.all
            if (key == null) {
                map.putAll(all)
            } else {
                map.put(key, all[key])
            }
            for ((key1, value) in map) {
                val type = getPreferenceType(value)
                c.addRow(arrayOf(key1, ParseUtils.parseString(value), type))
            }
            return c
        }

        private fun getPreferenceType(`object`: Any?): Int {
            if (`object` == null)
                return Preferences.TYPE_NULL
            else if (`object` is Boolean)
                return Preferences.TYPE_BOOLEAN
            else if (`object` is Int)
                return Preferences.TYPE_INTEGER
            else if (`object` is Long)
                return Preferences.TYPE_LONG
            else if (`object` is Float)
                return Preferences.TYPE_FLOAT
            else if (`object` is String) return Preferences.TYPE_STRING
            return Preferences.TYPE_INVALID
        }

        private fun getUnreadCount(set: List<UnreadItem>?, vararg accountIds: Long): Int {
            return set?.count { ArrayUtils.contains(accountIds, it.account_id) } ?: 0
        }

        private fun shouldReplaceOnConflict(table_id: Int): Boolean {
            when (table_id) {
                TABLE_ID_CACHED_HASHTAGS, TABLE_ID_CACHED_STATUSES, TABLE_ID_CACHED_USERS, TABLE_ID_CACHED_RELATIONSHIPS, TABLE_ID_SEARCH_HISTORY, TABLE_ID_FILTERED_USERS, TABLE_ID_FILTERED_KEYWORDS, TABLE_ID_FILTERED_SOURCES, TABLE_ID_FILTERED_LINKS -> return true
            }
            return false
        }
    }

}
