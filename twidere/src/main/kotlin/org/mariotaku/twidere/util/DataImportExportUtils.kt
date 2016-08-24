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

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.support.annotation.WorkerThread
import android.util.Log
import com.bluelinelabs.logansquare.LoganSquare
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import org.mariotaku.commons.logansquare.LoganSquareMapperFinder
import org.mariotaku.library.objectcursor.ObjectCursor
import org.mariotaku.twidere.TwidereConstants.*
import org.mariotaku.twidere.annotation.Preference
import org.mariotaku.twidere.annotation.PreferenceType
import org.mariotaku.twidere.constant.SharedPreferenceConstants
import org.mariotaku.twidere.model.FiltersData
import org.mariotaku.twidere.model.Tab
import org.mariotaku.twidere.provider.TwidereDataStore.Filters
import org.mariotaku.twidere.provider.TwidereDataStore.Tabs
import org.mariotaku.twidere.util.content.ContentResolverUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.reflect.Modifier
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object DataImportExportUtils {

    val ENTRY_PREFERENCES = "preferences.json"
    val ENTRY_NICKNAMES = "nicknames.json"
    val ENTRY_USER_COLORS = "user_colors.json"
    val ENTRY_HOST_MAPPING = "host_mapping.json"
    val ENTRY_KEYBOARD_SHORTCUTS = "keyboard_shortcuts.json"
    val ENTRY_FILTERS = "filters.json"
    val ENTRY_TABS = "tabs.json"

    val FLAG_PREFERENCES = 1
    val FLAG_NICKNAMES = 2
    val FLAG_USER_COLORS = 4
    val FLAG_HOST_MAPPING = 8
    val FLAG_KEYBOARD_SHORTCUTS = 16
    val FLAG_FILTERS = 32
    val FLAG_TABS = 64
    val FLAG_ALL = FLAG_PREFERENCES or FLAG_NICKNAMES or FLAG_USER_COLORS or FLAG_HOST_MAPPING or FLAG_KEYBOARD_SHORTCUTS or FLAG_FILTERS or FLAG_TABS

    @WorkerThread
    @Throws(IOException::class)
    fun exportData(context: Context, dst: File, flags: Int) {
        dst.delete()
        val fos = FileOutputStream(dst)
        val zos = ZipOutputStream(fos)
        try {
            if (hasFlag(flags, FLAG_PREFERENCES)) {
                exportSharedPreferencesData(zos, context, SHARED_PREFERENCES_NAME, ENTRY_PREFERENCES, AnnotationProcessStrategy(SharedPreferenceConstants::class.java))
            }
            if (hasFlag(flags, FLAG_NICKNAMES)) {
                exportSharedPreferencesData(zos, context, USER_NICKNAME_PREFERENCES_NAME, ENTRY_NICKNAMES, ConvertToStringProcessStrategy)
            }
            if (hasFlag(flags, FLAG_USER_COLORS)) {
                exportSharedPreferencesData(zos, context, USER_COLOR_PREFERENCES_NAME, ENTRY_USER_COLORS, ConvertToIntProcessStrategy)
            }
            if (hasFlag(flags, FLAG_HOST_MAPPING)) {
                exportSharedPreferencesData(zos, context, HOST_MAPPING_PREFERENCES_NAME, ENTRY_HOST_MAPPING, ConvertToStringProcessStrategy)
            }
            if (hasFlag(flags, FLAG_KEYBOARD_SHORTCUTS)) {
                exportSharedPreferencesData(zos, context, KEYBOARD_SHORTCUTS_PREFERENCES_NAME, ENTRY_KEYBOARD_SHORTCUTS, ConvertToStringProcessStrategy)
            }
            if (hasFlag(flags, FLAG_FILTERS)) {
                // TODO export filters
                val data = FiltersData()

                val cr = context.contentResolver
//                data.users = queryAll(cr, Filters.Users.CONTENT_URI, Filters.Users.COLUMNS,
//                        `FiltersData$UserItemCursorIndices`::class.java)
//                data.keywords = queryAll(cr, Filters.Keywords.CONTENT_URI, Filters.Keywords.COLUMNS,
//                        `FiltersData$BaseItemCursorIndices`::class.java)
//                data.sources = queryAll(cr, Filters.Sources.CONTENT_URI, Filters.Sources.COLUMNS,
//                        `FiltersData$BaseItemCursorIndices`::class.java)
//                data.links = queryAll(cr, Filters.Links.CONTENT_URI, Filters.Links.COLUMNS,
//                        `FiltersData$BaseItemCursorIndices`::class.java)
                exportItem(zos, ENTRY_FILTERS, FiltersData::class.java, data)
            }
            if (hasFlag(flags, FLAG_TABS)) {
                // TODO export tabs
                val cr = context.contentResolver
                val c = cr.query(Tabs.CONTENT_URI, Tabs.COLUMNS, null, null, null)
                if (c != null) {
                    val tabs = ArrayList<Tab>(c.count)
                    try {
//                        val ci = TabCursorIndices(c)
                        c.moveToFirst()
                        while (!c.isAfterLast) {
//                            tabs.add(ci.newObject(c))
                            c.moveToNext()
                        }
                    } finally {
                        c.close()
                    }
                    exportItemsList(zos, ENTRY_TABS, Tab::class.java, tabs)
                }
            }
            zos.finish()
            zos.flush()
        } finally {
            Utils.closeSilently(zos)
            Utils.closeSilently(fos)
        }
    }

    private fun <T> queryAll(cr: ContentResolver, uri: Uri, projection: Array<String>,
                             cls: Class<out ObjectCursor.CursorIndices<T>>): List<T>? {
        val c = cr.query(uri, projection, null, null, null) ?: return null
        try {
            val ci: ObjectCursor.CursorIndices<T>
            try {
                ci = cls.getConstructor(Cursor::class.java).newInstance(c)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }

            val items = ArrayList<T>(c.count)
            c.moveToFirst()
            while (!c.isAfterLast) {
                items.add(ci.newObject(c))
                c.moveToNext()
            }
            return items
        } finally {
            c.close()
        }
    }

    @WorkerThread
    @Throws(IOException::class)
    fun getImportedSettingsFlags(src: File): Int {
        val zipFile = ZipFile(src)
        var flags = 0
        if (zipFile.getEntry(ENTRY_PREFERENCES) != null) {
            flags = flags or FLAG_PREFERENCES
        }
        if (zipFile.getEntry(ENTRY_NICKNAMES) != null) {
            flags = flags or FLAG_NICKNAMES
        }
        if (zipFile.getEntry(ENTRY_USER_COLORS) != null) {
            flags = flags or FLAG_USER_COLORS
        }
        if (zipFile.getEntry(ENTRY_HOST_MAPPING) != null) {
            flags = flags or FLAG_HOST_MAPPING
        }
        if (zipFile.getEntry(ENTRY_KEYBOARD_SHORTCUTS) != null) {
            flags = flags or FLAG_KEYBOARD_SHORTCUTS
        }
        if (zipFile.getEntry(ENTRY_FILTERS) != null) {
            flags = flags or FLAG_FILTERS
        }
        if (zipFile.getEntry(ENTRY_TABS) != null) {
            flags = flags or FLAG_TABS
        }
        zipFile.close()
        return flags
    }

    fun getSupportedPreferencesMap(cls: Class<*>): HashMap<String, Preference> {
        val fields = cls.declaredFields
        val supportedPrefsMap = HashMap<String, Preference>()
        for (field in fields) {
            val annotation = field.getAnnotation(Preference::class.java)
            if (Modifier.isStatic(field.modifiers) && field.type == String::class.java
                    && annotation != null && annotation.exportable && annotation.type != PreferenceType.INVALID) {
                try {
                    supportedPrefsMap.put(field.get(null) as String, annotation)
                } catch (e: IllegalAccessException) {
                    Log.w(LOGTAG, e)
                } catch (e: IllegalArgumentException) {
                    Log.w(LOGTAG, e)
                }

            }
        }
        return supportedPrefsMap
    }

    @Throws(IOException::class)
    fun importData(context: Context, src: File?, flags: Int) {
        if (src == null) throw FileNotFoundException()
        val zipFile = ZipFile(src)
        if (hasFlag(flags, FLAG_PREFERENCES)) {
            importSharedPreferencesData(zipFile, context, SHARED_PREFERENCES_NAME, ENTRY_PREFERENCES, AnnotationProcessStrategy(SharedPreferenceConstants::class.java))
        }
        if (hasFlag(flags, FLAG_NICKNAMES)) {
            importSharedPreferencesData(zipFile, context, USER_NICKNAME_PREFERENCES_NAME, ENTRY_NICKNAMES, ConvertToStringProcessStrategy)
        }
        if (hasFlag(flags, FLAG_USER_COLORS)) {
            importSharedPreferencesData(zipFile, context, USER_COLOR_PREFERENCES_NAME, ENTRY_USER_COLORS, ConvertToIntProcessStrategy)
        }
        if (hasFlag(flags, FLAG_HOST_MAPPING)) {
            importSharedPreferencesData(zipFile, context, HOST_MAPPING_PREFERENCES_NAME, ENTRY_HOST_MAPPING, ConvertToStringProcessStrategy)
        }
        if (hasFlag(flags, FLAG_KEYBOARD_SHORTCUTS)) {
            importSharedPreferencesData(zipFile, context, KEYBOARD_SHORTCUTS_PREFERENCES_NAME, ENTRY_KEYBOARD_SHORTCUTS, ConvertToStringProcessStrategy)
        }
        if (hasFlag(flags, FLAG_FILTERS)) {
            importItem(context, zipFile, ENTRY_FILTERS, FiltersData::class.java, object : ContentResolverProcessStrategy<FiltersData> {
                override fun importItem(cr: ContentResolver, item: FiltersData): Boolean {
                    insertBase(cr, Filters.Keywords.CONTENT_URI, item.keywords)
                    insertBase(cr, Filters.Sources.CONTENT_URI, item.sources)
                    insertBase(cr, Filters.Links.CONTENT_URI, item.links)
                    insertUser(cr, Filters.Users.CONTENT_URI, item.users)
                    return true
                }

                internal fun insertBase(cr: ContentResolver, uri: Uri, items: List<FiltersData.BaseItem>?) {
                    if (items == null) return
                    val values = ArrayList<ContentValues>(items.size)
                    for (item in items) {
//                        values.add(`FiltersData$BaseItemValuesCreator`.create(item))
                    }
                    ContentResolverUtils.bulkInsert(cr, uri, values)
                }

                internal fun insertUser(cr: ContentResolver, uri: Uri, items: List<FiltersData.UserItem>?) {
                    if (items == null) return
                    val values = ArrayList<ContentValues>(items.size)
                    for (item in items) {
//                        values.add(`FiltersData$UserItemValuesCreator`.create(item))
                    }
                    ContentResolverUtils.bulkInsert(cr, uri, values)
                }
            })
        }
        if (hasFlag(flags, FLAG_TABS)) {
            importItemsList(context, zipFile, ENTRY_TABS, Tab::class.java, object : ContentResolverProcessStrategy<List<Tab>> {
                override fun importItem(cr: ContentResolver, items: List<Tab>): Boolean {
                    val values = ArrayList<ContentValues>(items.size)
                    for (item in items) {
//                        values.add(TabValuesCreator.create(item))
                    }
                    cr.delete(Tabs.CONTENT_URI, null, null)
                    ContentResolverUtils.bulkInsert(cr, Tabs.CONTENT_URI, values)
                    return true
                }
            })
        }
        zipFile.close()
    }

    private fun hasFlag(flags: Int, flag: Int): Boolean {
        return flags and flag != 0
    }

    @Throws(IOException::class)
    private fun importSharedPreferencesData(zipFile: ZipFile, context: Context,
                                            preferencesName: String, entryName: String,
                                            strategy: SharedPreferencesProcessStrategy) {
        val entry = zipFile.getEntry(entryName) ?: return
        val jsonParser = LoganSquare.JSON_FACTORY.createParser(zipFile.getInputStream(entry))
        if (jsonParser.currentToken == null) {
            jsonParser.nextToken()
        }
        if (jsonParser.currentToken != JsonToken.START_OBJECT) {
            jsonParser.skipChildren()
            return
        }
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            val key = jsonParser.currentName
            strategy.importValue(jsonParser, key, editor)
        }
        editor.apply()
    }

    @Throws(IOException::class)
    private fun exportSharedPreferencesData(zos: ZipOutputStream, context: Context,
                                            preferencesName: String, entryName: String,
                                            strategy: SharedPreferencesProcessStrategy) {
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val map = preferences.all
        zos.putNextEntry(ZipEntry(entryName))
        val jsonGenerator = LoganSquare.JSON_FACTORY.createGenerator(zos)
        jsonGenerator.writeStartObject()
        for (key in map.keys) {
            strategy.exportValue(jsonGenerator, key, preferences)
        }
        jsonGenerator.writeEndObject()
        jsonGenerator.flush()
        zos.closeEntry()
    }

    @Throws(IOException::class)
    private fun <T> importItemsList(context: Context,
                                    zipFile: ZipFile,
                                    entryName: String,
                                    itemCls: Class<T>,
                                    strategy: ContentResolverProcessStrategy<List<T>>) {
        val entry = zipFile.getEntry(entryName) ?: return
        val mapper = LoganSquareMapperFinder.mapperFor(itemCls)
        val itemsList = mapper.parseList(zipFile.getInputStream(entry)) ?: return
        strategy.importItem(context.contentResolver, itemsList)
    }


    @Throws(IOException::class)
    private fun <T> exportItemsList(zos: ZipOutputStream,
                                    entryName: String,
                                    itemCls: Class<T>,
                                    itemList: List<T>) {
        zos.putNextEntry(ZipEntry(entryName))
        val jsonGenerator = LoganSquare.JSON_FACTORY.createGenerator(zos)
        LoganSquareMapperFinder.mapperFor(itemCls).serialize(itemList, jsonGenerator)
        jsonGenerator.flush()
        zos.closeEntry()
    }

    @Throws(IOException::class)
    private fun <T> importItem(context: Context,
                               zipFile: ZipFile,
                               entryName: String,
                               itemCls: Class<T>,
                               strategy: ContentResolverProcessStrategy<T>) {
        val entry = zipFile.getEntry(entryName) ?: return
        val mapper = LoganSquareMapperFinder.mapperFor(itemCls)
        val item = mapper.parse(zipFile.getInputStream(entry))
        strategy.importItem(context.contentResolver, item)
    }


    @Throws(IOException::class)
    private fun <T> exportItem(zos: ZipOutputStream,
                               entryName: String,
                               itemCls: Class<T>,
                               item: T) {
        zos.putNextEntry(ZipEntry(entryName))
        val jsonGenerator = LoganSquare.JSON_FACTORY.createGenerator(zos)
        LoganSquareMapperFinder.mapperFor(itemCls).serialize(item, jsonGenerator, true)
        jsonGenerator.flush()
        zos.closeEntry()
    }

    private interface ContentResolverProcessStrategy<T> {
        fun importItem(cr: ContentResolver, item: T): Boolean
    }

    private interface SharedPreferencesProcessStrategy {
        @Throws(IOException::class)
        fun importValue(jsonParser: JsonParser, key: String, editor: SharedPreferences.Editor): Boolean

        @Throws(IOException::class)
        fun exportValue(jsonGenerator: JsonGenerator, key: String, preferences: SharedPreferences): Boolean
    }

    private object ConvertToStringProcessStrategy : SharedPreferencesProcessStrategy {

        @Throws(IOException::class)
        override fun importValue(jsonParser: JsonParser, key: String, editor: SharedPreferences.Editor): Boolean {
            val token = jsonParser.nextToken() ?: return false
            editor.putString(key, jsonParser.valueAsString)
            return true
        }

        override fun exportValue(jsonGenerator: JsonGenerator, key: String, preferences: SharedPreferences): Boolean {
            if (!preferences.contains(key)) return false
            try {
                jsonGenerator.writeStringField(key, preferences.getString(key, null))
            } catch (ignore: Exception) {
                return false
            }

            return true
        }

    }

    private object ConvertToIntProcessStrategy : SharedPreferencesProcessStrategy {

        @Throws(IOException::class)
        override fun importValue(jsonParser: JsonParser, key: String, editor: SharedPreferences.Editor): Boolean {
            val token = jsonParser.nextToken() ?: return false
            editor.putInt(key, jsonParser.valueAsInt)
            return true
        }

        override fun exportValue(jsonGenerator: JsonGenerator, key: String, preferences: SharedPreferences): Boolean {
            if (!preferences.contains(key)) return false
            try {
                jsonGenerator.writeNumberField(key, preferences.getInt(key, 0))
            } catch (ignore: Exception) {
                return false
            }

            return true
        }

    }

    private class AnnotationProcessStrategy internal constructor(cls: Class<*>) : SharedPreferencesProcessStrategy {

        private val supportedMap: HashMap<String, Preference>

        init {
            this.supportedMap = getSupportedPreferencesMap(cls)
        }

        @SuppressLint("SwitchIntDef")
        @Throws(IOException::class)
        override fun importValue(jsonParser: JsonParser, key: String, editor: SharedPreferences.Editor): Boolean {
            val token = jsonParser.nextToken() ?: return false
            val preference = supportedMap[key]
            if (preference == null || !preference.exportable) return false
            when (preference.type) {
                PreferenceType.BOOLEAN -> {
                    editor.putBoolean(key, jsonParser.valueAsBoolean)
                }
                PreferenceType.INT -> {
                    editor.putInt(key, jsonParser.valueAsInt)
                }
                PreferenceType.LONG -> {
                    editor.putLong(key, jsonParser.valueAsLong)
                }
                PreferenceType.FLOAT -> {
                    editor.putFloat(key, jsonParser.valueAsDouble.toFloat())
                }
                PreferenceType.STRING -> {
                    editor.putString(key, jsonParser.valueAsString)
                }
                else -> {
                }
            }
            return true
        }

        @SuppressLint("SwitchIntDef")
        @Throws(IOException::class)
        override fun exportValue(jsonGenerator: JsonGenerator, key: String, preferences: SharedPreferences): Boolean {
            val preference = supportedMap[key]
            if (preference == null || !preference.exportable) return false
            try {
                when (preference.type) {
                    PreferenceType.BOOLEAN -> jsonGenerator.writeBooleanField(key, preferences.getBoolean(key, preference.defaultBoolean))
                    PreferenceType.INT -> jsonGenerator.writeNumberField(key, preferences.getInt(key, preference.defaultInt))
                    PreferenceType.LONG -> jsonGenerator.writeNumberField(key, preferences.getLong(key, preference.defaultLong))
                    PreferenceType.FLOAT -> jsonGenerator.writeNumberField(key, preferences.getFloat(key, preference.defaultFloat))
                    PreferenceType.STRING -> jsonGenerator.writeStringField(key, preferences.getString(key, preference.defaultString))
                    else -> {
                    }
                }
            } catch (e: ClassCastException) {
                return false
            }

            return true
        }
    }

}
