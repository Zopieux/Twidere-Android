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

package org.mariotaku.twidere.util.content

import android.database.sqlite.SQLiteDatabase
import org.apache.commons.lang3.ArrayUtils
import org.mariotaku.sqliteqb.library.*
import org.mariotaku.sqliteqb.library.SQLQueryBuilder.*
import org.mariotaku.twidere.util.TwidereArrayUtils
import java.util.*

object DatabaseUpgradeHelper {

    fun safeUpgrade(db: SQLiteDatabase, table: String, newColNames: Array<String>?,
                    newColTypes: Array<String>?, dropDirectly: Boolean,
                    colAliases: Map<String, String>, onConflict: OnConflict,
                    vararg constraints: Constraint) {
        if (newColNames == null || newColTypes == null || newColNames.size != newColTypes.size)
            throw IllegalArgumentException("Invalid parameters for upgrading table " + table
                    + ", length of columns and types not match.")

        // First, create the table if not exists.
        val newCols = NewColumn.createNewColumns(newColNames, newColTypes)
        val createQuery = createTable(true, table).columns(*newCols).constraint(*constraints).buildSQL()
        db.execSQL(createQuery)

        // We need to get all data from old table.
        val oldCols = getColumnNames(db, table)
        if (oldCols == null || TwidereArrayUtils.contentMatch(newColNames, oldCols)) return
        if (dropDirectly) {
            db.beginTransaction()
            db.execSQL(dropTable(true, table).sql)
            db.execSQL(createQuery)
            db.setTransactionSuccessful()
            db.endTransaction()
            return
        }
        val tempTable = String.format(Locale.US, "temp_%s_%d", table, System.currentTimeMillis())
        db.beginTransaction()
        db.execSQL(alterTable(table).renameTo(tempTable).buildSQL())
        db.execSQL(createQuery)
        val notNullCols = getNotNullColumns(newCols)
        val insertQuery = createInsertDataQuery(table, tempTable, newColNames, oldCols, colAliases,
                notNullCols, onConflict)
        if (insertQuery != null) {
            db.execSQL(insertQuery)
        }
        db.execSQL(dropTable(true, tempTable).sql)
        db.setTransactionSuccessful()
        db.endTransaction()
    }

    fun safeUpgrade(db: SQLiteDatabase, table: String, newColNames: Array<String>,
                    newColTypes: Array<String>, dropDirectly: Boolean,
                    colAliases: Map<String, String>, vararg constraints: Constraint) {
        safeUpgrade(db, table, newColNames, newColTypes, dropDirectly, colAliases, OnConflict.REPLACE, *constraints)
    }

    private fun createInsertDataQuery(table: String, tempTable: String,
                                      newCols: Array<String>,
                                      oldCols: Array<String>,
                                      colAliases: Map<String, String>?,
                                      notNullCols: Array<String>?,
                                      onConflict: OnConflict): String? {
        val qb = insertInto(onConflict, table)
        val newInsertColsList = ArrayList<String>()
        for (newCol in newCols) {
            val oldAliasedCol = if (colAliases != null) colAliases[newCol] else null
            if (ArrayUtils.contains(oldCols, newCol) || oldAliasedCol != null && ArrayUtils.contains(oldCols, oldAliasedCol)) {
                newInsertColsList.add(newCol)
            }
        }
        val newInsertCols = newInsertColsList.toTypedArray()
        notNullCols?.forEach {
            if (!newInsertCols.contains(it)) return null
        }
        qb.columns(newInsertCols)
        val oldDataCols = arrayOfNulls<Columns.Column>(newInsertCols.size)
        var i = 0
        val j = oldDataCols.size
        while (i < j) {
            val newCol = newInsertCols[i]
            val oldAliasedCol = if (colAliases != null) colAliases[newCol] else null
            if (oldAliasedCol != null && ArrayUtils.contains(oldCols, oldAliasedCol)) {
                oldDataCols[i] = Columns.Column(oldAliasedCol, newCol)
            } else {
                oldDataCols[i] = Columns.Column(newCol)
            }
            i++
        }
        val selectOldBuilder = select(Columns(*oldDataCols))
        selectOldBuilder.from(Tables(tempTable))
        qb.select(selectOldBuilder.build())
        return qb.buildSQL()
    }

    private fun getColumnNames(db: SQLiteDatabase, table: String): Array<String>? {
        val cur = db.query(table, null, null, null, null, null, null, "1") ?: return null
        try {
            return cur.columnNames
        } finally {
            cur.close()
        }
    }

    private fun getNotNullColumns(newCols: Array<NewColumn>?): Array<String>? {
        if (newCols == null) return null
        val notNullCols = arrayOfNulls<String>(newCols.size)
        var count = 0
        for (column in newCols) {
            if (column.type.endsWith(" NOT NULL")) {
                notNullCols[count++] = column.name
            }
        }
        return ArrayUtils.subarray<String>(notNullCols, 0, count)
    }

}
