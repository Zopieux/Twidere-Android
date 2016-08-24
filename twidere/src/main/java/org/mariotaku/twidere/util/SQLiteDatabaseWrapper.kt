package org.mariotaku.twidere.util

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase

class SQLiteDatabaseWrapper(private val lazyLoadCallback: SQLiteDatabaseWrapper.LazyLoadCallback?) {
    var sqliteDatabase: SQLiteDatabase? = null

    fun beginTransaction() {
        tryCreateDatabase()
        sqliteDatabase?.beginTransaction()
    }

    fun delete(table: String, whereClause: String, whereArgs: Array<String>): Int {
        tryCreateDatabase()
        return sqliteDatabase?.delete(table, whereClause, whereArgs) ?: 0
    }

    fun endTransaction() {
        tryCreateDatabase()
        sqliteDatabase?.endTransaction()
    }

    fun insert(table: String, nullColumnHack: String, values: ContentValues): Long {
        tryCreateDatabase()
        return sqliteDatabase?.insert(table, nullColumnHack, values) ?: -1
    }

    fun insertWithOnConflict(table: String, nullColumnHack: String,
                             initialValues: ContentValues, conflictAlgorithm: Int): Long {
        tryCreateDatabase()
        return sqliteDatabase?.insertWithOnConflict(table, nullColumnHack, initialValues, conflictAlgorithm) ?: -1

    }

    @Throws(SQLException::class)
    fun execSQL(sql: String) {
        tryCreateDatabase()
        sqliteDatabase?.execSQL(sql)
    }

    @Throws(SQLException::class)
    fun execSQL(sql: String, bindArgs: Array<Any>) {
        tryCreateDatabase()
        sqliteDatabase?.execSQL(sql, bindArgs)
    }

    val isReady: Boolean
        get() {
            if (lazyLoadCallback != null) return true
            return sqliteDatabase != null
        }

    fun query(table: String, columns: Array<String>, selection: String,
              selectionArgs: Array<String>, groupBy: String, having: String, orderBy: String): Cursor? {
        tryCreateDatabase()
        return sqliteDatabase?.query(table, columns, selection, selectionArgs, groupBy, having, orderBy)
    }

    fun rawQuery(sql: String, selectionArgs: Array<String>): Cursor? {
        tryCreateDatabase()
        return sqliteDatabase?.rawQuery(sql, selectionArgs)
    }

    fun setTransactionSuccessful() {
        tryCreateDatabase()
        sqliteDatabase?.setTransactionSuccessful()
    }


    fun query(distinct: Boolean, table: String, columns: Array<String>, selection: String, selectionArgs: Array<String>, groupBy: String, having: String, orderBy: String, limit: String): Cursor? {
        tryCreateDatabase()
        return sqliteDatabase?.query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit)
    }

    fun query(table: String, columns: Array<String>, selection: String, selectionArgs: Array<String>, groupBy: String, having: String, orderBy: String, limit: String): Cursor? {
        tryCreateDatabase()
        return sqliteDatabase?.query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit)
    }

    fun update(table: String, values: ContentValues, whereClause: String, whereArgs: Array<String>): Int {
        tryCreateDatabase()
        return sqliteDatabase?.update(table, values, whereClause, whereArgs) ?: 0
    }

    private fun tryCreateDatabase() {
        if (lazyLoadCallback == null || sqliteDatabase != null) return
        sqliteDatabase = lazyLoadCallback.onCreateSQLiteDatabase()
    }

    interface LazyLoadCallback {
        fun onCreateSQLiteDatabase(): SQLiteDatabase
    }

}