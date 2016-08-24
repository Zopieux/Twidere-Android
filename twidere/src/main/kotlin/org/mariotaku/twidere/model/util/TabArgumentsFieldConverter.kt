package org.mariotaku.twidere.model.util

import android.content.ContentValues
import android.database.Cursor
import android.text.TextUtils

import org.mariotaku.library.objectcursor.converter.CursorFieldConverter
import org.mariotaku.twidere.model.tab.argument.TabArguments
import org.mariotaku.twidere.provider.TwidereDataStore.Tabs
import org.mariotaku.twidere.util.CustomTabUtils
import org.mariotaku.twidere.util.JsonSerializer

import java.lang.reflect.ParameterizedType

/**
 * Created by mariotaku on 16/3/6.
 */
class TabArgumentsFieldConverter : CursorFieldConverter<TabArguments> {

    override fun parseField(cursor: Cursor, columnIndex: Int, fieldType: ParameterizedType): TabArguments? {
        val tabType = CustomTabUtils.getTabTypeAlias(cursor.getString(cursor.getColumnIndex(Tabs.TYPE)))
        if (TextUtils.isEmpty(tabType)) return null
        return CustomTabUtils.parseTabArguments(tabType!!, cursor.getString(columnIndex))
    }

    override fun writeField(values: ContentValues, `object`: TabArguments?, columnName: String, fieldType: ParameterizedType) {
        if (`object` == null) return
        values.put(columnName, JsonSerializer.serialize(`object`))
    }
}
