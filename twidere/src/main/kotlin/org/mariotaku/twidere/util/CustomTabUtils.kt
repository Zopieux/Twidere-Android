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

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.res.ResourcesCompat
import android.text.TextUtils
import org.apache.commons.lang3.ArrayUtils
import org.mariotaku.twidere.BuildConfig
import org.mariotaku.twidere.R
import org.mariotaku.twidere.annotation.CustomTabType
import org.mariotaku.twidere.annotation.ReadPositionTag
import org.mariotaku.twidere.constant.IntentConstants.*
import org.mariotaku.twidere.fragment.*
import org.mariotaku.twidere.model.CustomTabConfiguration
import org.mariotaku.twidere.model.CustomTabConfiguration.ExtraConfiguration
import org.mariotaku.twidere.model.SupportTabSpec
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.model.tab.argument.TabArguments
import org.mariotaku.twidere.model.tab.argument.TextQueryArguments
import org.mariotaku.twidere.model.tab.argument.UserArguments
import org.mariotaku.twidere.model.tab.argument.UserListArguments
import org.mariotaku.twidere.model.tab.extra.HomeTabExtras
import org.mariotaku.twidere.model.tab.extra.InteractionsTabExtras
import org.mariotaku.twidere.model.tab.extra.TabExtras
import org.mariotaku.twidere.provider.TwidereDataStore.Tabs
import java.io.File
import java.util.*

object CustomTabUtils {
    private val CUSTOM_TABS_CONFIGURATION_MAP = HashMap<String, CustomTabConfiguration>()
    private val CUSTOM_TABS_ICON_NAME_MAP = HashMap<String, Int>()

    init {
        CUSTOM_TABS_CONFIGURATION_MAP.put(CustomTabType.HOME_TIMELINE, CustomTabConfiguration(
                HomeTimelineFragment::class.java, R.string.home, R.drawable.ic_action_home,
                CustomTabConfiguration.ACCOUNT_OPTIONAL, CustomTabConfiguration.FIELD_TYPE_NONE, 0, false,
                ExtraConfiguration.newBoolean(EXTRA_HIDE_RETWEETS, R.string.hide_retweets, false),
                ExtraConfiguration.newBoolean(EXTRA_HIDE_QUOTES, R.string.hide_quotes, false),
                ExtraConfiguration.newBoolean(EXTRA_HIDE_REPLIES, R.string.hide_replies, false)))

        CUSTOM_TABS_CONFIGURATION_MAP.put(CustomTabType.NOTIFICATIONS_TIMELINE, CustomTabConfiguration(
                InteractionsTimelineFragment::class.java, R.string.interactions, R.drawable.ic_action_notification,
                CustomTabConfiguration.ACCOUNT_OPTIONAL, CustomTabConfiguration.FIELD_TYPE_NONE, 1, false,
                ExtraConfiguration.newBoolean(EXTRA_MY_FOLLOWING_ONLY, R.string.following_only, false),
                ExtraConfiguration.newBoolean(EXTRA_MENTIONS_ONLY, R.string.mentions_only, false)))

        if (BuildConfig.DEBUG) {
            CUSTOM_TABS_CONFIGURATION_MAP.put(CustomTabType.DIRECT_MESSAGES_NEXT, CustomTabConfiguration(
                    MessagesEntriesFragment::class.java, R.string.direct_messages_next, R.drawable.ic_action_message,
                    CustomTabConfiguration.ACCOUNT_OPTIONAL, CustomTabConfiguration.FIELD_TYPE_NONE, 2, false))
        }
        CUSTOM_TABS_CONFIGURATION_MAP.put(CustomTabType.DIRECT_MESSAGES, CustomTabConfiguration(
                DirectMessagesFragment::class.java, R.string.direct_messages, R.drawable.ic_action_message,
                CustomTabConfiguration.ACCOUNT_OPTIONAL, CustomTabConfiguration.FIELD_TYPE_NONE, 2, false))

        CUSTOM_TABS_CONFIGURATION_MAP.put(CustomTabType.TRENDS_SUGGESTIONS, CustomTabConfiguration(
                TrendsSuggestionsFragment::class.java, R.string.trends, R.drawable.ic_action_hashtag,
                CustomTabConfiguration.ACCOUNT_NONE, CustomTabConfiguration.FIELD_TYPE_NONE, 3, true))
        CUSTOM_TABS_CONFIGURATION_MAP.put(CustomTabType.FAVORITES, CustomTabConfiguration(UserFavoritesFragment::class.java,
                R.string.likes, R.drawable.ic_action_heart, CustomTabConfiguration.ACCOUNT_REQUIRED,
                CustomTabConfiguration.FIELD_TYPE_USER, 4))
        CUSTOM_TABS_CONFIGURATION_MAP.put(CustomTabType.USER_TIMELINE, CustomTabConfiguration(
                UserTimelineFragment::class.java, R.string.users_statuses, R.drawable.ic_action_quote,
                CustomTabConfiguration.ACCOUNT_REQUIRED, CustomTabConfiguration.FIELD_TYPE_USER, 5))
        CUSTOM_TABS_CONFIGURATION_MAP.put(CustomTabType.SEARCH_STATUSES, CustomTabConfiguration(
                StatusesSearchFragment::class.java, R.string.search_statuses, R.drawable.ic_action_search,
                CustomTabConfiguration.ACCOUNT_REQUIRED, CustomTabConfiguration.FIELD_TYPE_TEXT, R.string.query,
                EXTRA_QUERY, 6))

        CUSTOM_TABS_CONFIGURATION_MAP.put(CustomTabType.LIST_TIMELINE, CustomTabConfiguration(
                UserListTimelineFragment::class.java, R.string.list_timeline, R.drawable.ic_action_list,
                CustomTabConfiguration.ACCOUNT_REQUIRED, CustomTabConfiguration.FIELD_TYPE_USER_LIST, 7))

        CUSTOM_TABS_ICON_NAME_MAP.put("accounts", R.drawable.ic_action_accounts)
        CUSTOM_TABS_ICON_NAME_MAP.put("hashtag", R.drawable.ic_action_hashtag)
        CUSTOM_TABS_ICON_NAME_MAP.put("heart", R.drawable.ic_action_heart)
        CUSTOM_TABS_ICON_NAME_MAP.put("home", R.drawable.ic_action_home)
        CUSTOM_TABS_ICON_NAME_MAP.put("list", R.drawable.ic_action_list)
        CUSTOM_TABS_ICON_NAME_MAP.put("mention", R.drawable.ic_action_at)
        CUSTOM_TABS_ICON_NAME_MAP.put("notifications", R.drawable.ic_action_notification)
        CUSTOM_TABS_ICON_NAME_MAP.put("gallery", R.drawable.ic_action_gallery)
        CUSTOM_TABS_ICON_NAME_MAP.put("message", R.drawable.ic_action_message)
        CUSTOM_TABS_ICON_NAME_MAP.put("quote", R.drawable.ic_action_quote)
        CUSTOM_TABS_ICON_NAME_MAP.put("search", R.drawable.ic_action_search)
        CUSTOM_TABS_ICON_NAME_MAP.put("staggered", R.drawable.ic_action_view_quilt)
        CUSTOM_TABS_ICON_NAME_MAP.put("star", R.drawable.ic_action_star)
        CUSTOM_TABS_ICON_NAME_MAP.put("trends", R.drawable.ic_action_trends)
        CUSTOM_TABS_ICON_NAME_MAP.put("twidere", R.drawable.ic_action_twidere)
        CUSTOM_TABS_ICON_NAME_MAP.put("twitter", R.drawable.ic_action_twitter)
        CUSTOM_TABS_ICON_NAME_MAP.put("user", R.drawable.ic_action_user)
    }

    fun findTabIconKey(iconRes: Int): String? {
        for ((key, value) in iconMap) {
            if (value === iconRes) return key
        }
        return null
    }

    fun findTabType(cls: Class<out Fragment>): String? {
        for ((key, value) in configurationMap) {
            if (cls == value.fragmentClass) return key
        }
        return null
    }


    val configurationMap: HashMap<String, CustomTabConfiguration>
        get() = HashMap(CUSTOM_TABS_CONFIGURATION_MAP)

    fun getHomeTabs(context: Context?): List<SupportTabSpec> {
        if (context == null) return emptyList()
        val resolver = context.contentResolver
        val cur = resolver.query(Tabs.CONTENT_URI, Tabs.COLUMNS, null, null, Tabs.DEFAULT_SORT_ORDER) ?: return emptyList()
        val tabs = ArrayList<SupportTabSpec>()
        cur.moveToFirst()
//        val indices = TabCursorIndices(cur)
        val idxArguments = cur.getColumnIndex(Tabs.ARGUMENTS)
        val idxExtras = cur.getColumnIndex(Tabs.EXTRAS)
        while (!cur.isAfterLast) {
//            @CustomTabType
//            val type = getTabTypeAlias(cur.getString(indices.type))
//            val position = cur.getInt(indices.position)
//            val iconType = cur.getString(indices.icon)
//            val name = cur.getString(indices.name)
//            val args = Bundle()
//            val tabArguments = parseTabArguments(type!!, cur.getString(idxArguments))
//            tabArguments?.copyToBundle(args)
//            @ReadPositionTag
//            val tag = getTagByType(type)
//            args.putInt(EXTRA_TAB_POSITION, position)
//            args.putLong(EXTRA_TAB_ID, cur.getLong(indices.id))
//            val tabExtras = parseTabExtras(type, cur.getString(idxExtras))
//            if (tabExtras != null) {
//                args.putParcelable(EXTRA_EXTRAS, tabExtras)
//            }
//            val conf = getTabConfiguration(type)
//            val cls = if (conf != null) conf.fragmentClass else InvalidTabFragment::class.java
//            val tabTypeName = getTabTypeName(context, type)
//            val tabIconObject = getTabIconObject(iconType)
//            tabs.add(SupportTabSpec(if (TextUtils.isEmpty(name)) tabTypeName else name, tabIconObject,
//                    type, cls, args, position, tag))
            cur.moveToNext()
        }
        cur.close()
        Collections.sort(tabs)
        return tabs
    }

    fun newTabArguments(@CustomTabType type: String): TabArguments? {
        return parseTabArguments(type, "{}")
    }

    fun parseTabArguments(@CustomTabType type: String, json: String): TabArguments? {
        when (type) {
            CustomTabType.HOME_TIMELINE, CustomTabType.NOTIFICATIONS_TIMELINE, CustomTabType.DIRECT_MESSAGES -> {
                return JsonSerializer.parse(json, TabArguments::class.java)
            }
            CustomTabType.USER_TIMELINE, CustomTabType.FAVORITES -> {
                return JsonSerializer.parse(json, UserArguments::class.java)
            }
            CustomTabType.LIST_TIMELINE -> {
                return JsonSerializer.parse(json, UserListArguments::class.java)
            }
            CustomTabType.SEARCH_STATUSES -> {
                return JsonSerializer.parse(json, TextQueryArguments::class.java)
            }
        }
        return null
    }

    fun parseTabExtras(@CustomTabType type: String, json: String): TabExtras? {
        when (type) {
            CustomTabType.NOTIFICATIONS_TIMELINE -> {
                return JsonSerializer.parse(json, InteractionsTabExtras::class.java)
            }
            CustomTabType.HOME_TIMELINE -> {
                return JsonSerializer.parse(json, HomeTabExtras::class.java)
            }
        }
        return null
    }

    @ReadPositionTag
    fun getTagByType(@CustomTabType tabType: String): String? {
        when (tabType) {
            CustomTabType.HOME_TIMELINE -> return ReadPositionTag.HOME_TIMELINE
            "activities_about_me", CustomTabType.NOTIFICATIONS_TIMELINE -> return ReadPositionTag.ACTIVITIES_ABOUT_ME
            CustomTabType.DIRECT_MESSAGES -> return ReadPositionTag.DIRECT_MESSAGES
        }
        return null
    }

    val iconMap: HashMap<String, Int>
        get() = HashMap(CUSTOM_TABS_ICON_NAME_MAP)

    fun getTabConfiguration(tabType: String?): CustomTabConfiguration? {
        if (tabType == null) return null
        return CUSTOM_TABS_CONFIGURATION_MAP[getTabTypeAlias(tabType)]
    }

    @CustomTabType
    fun getTabTypeAlias(key: String?): String? {
        if (key == null) return null
        when (key) {
            "mentions_timeline", "activities_about_me" -> return CustomTabType.NOTIFICATIONS_TIMELINE
        }
        return key
    }

    fun getTabIconDrawable(context: Context, iconObj: Any?): Drawable {
        return getTabIconDrawable(context.resources, iconObj)
    }

    fun getTabIconDrawable(res: Resources, iconObj: Any?): Drawable {
        if (iconObj is Int) {
            try {
                return ResourcesCompat.getDrawable(res, iconObj, null)!!
            } catch (e: Resources.NotFoundException) {
                // Ignore.
            }

        } else if (iconObj is Bitmap)
            return BitmapDrawable(res, iconObj)
        else if (iconObj is Drawable)
            return iconObj
        else if (iconObj is File) {
            val b = getTabIconFromFile(iconObj, res)
            if (b != null) return BitmapDrawable(res, b)
        }
        return ResourcesCompat.getDrawable(res, R.drawable.ic_action_list, null)!!
    }

    fun getTabIconFromFile(file: File?, res: Resources): Bitmap? {
        if (file == null || !file.exists()) return null
        val path = file.path
        val o = BitmapFactory.Options()
        o.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, o)
        if (o.outHeight <= 0 || o.outWidth <= 0) return null
        o.inSampleSize = (Math.max(o.outWidth, o.outHeight) / (48 * res.displayMetrics.density)).toInt()
        o.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, o)
    }

    fun getTabIconObject(type: String?): Any {
        if (type == null) return R.drawable.ic_action_list
        val value = CUSTOM_TABS_ICON_NAME_MAP[type]
        if (value != null)
            return value
        else if (type.contains("/")) {
            try {
                val file = File(type)
                if (file.exists()) return file
            } catch (e: Exception) {
                return R.drawable.ic_action_list
            }

        }
        return R.drawable.ic_action_list
    }

    fun getTabTypeName(context: Context?, type: String): String? {
        if (context == null) return null
        val conf = getTabConfiguration(type) ?: return null
        return context.getString(conf.defaultTitle)
    }

    fun isSingleTab(type: String?): Boolean {
        if (type == null) return false
        val conf = getTabConfiguration(type)
        return conf != null && conf.isSingleTab
    }

    fun isTabAdded(context: Context?, type: String?): Boolean {
        if (context == null || type == null) return false
        val resolver = context.contentResolver
        val where = Tabs.TYPE + " = ?"
        val cur = resolver.query(Tabs.CONTENT_URI, arrayOfNulls<String>(0), where, arrayOf(type),
                Tabs.DEFAULT_SORT_ORDER) ?: return false
        val added = cur.count > 0
        cur.close()
        return added
    }

    fun isTabTypeValid(tabType: String?): Boolean {
        return tabType != null && CUSTOM_TABS_CONFIGURATION_MAP.containsKey(getTabTypeAlias(tabType))
    }

    fun hasAccountId(context: Context, args: Bundle,
                     activatedAccountKeys: Array<UserKey>?, accountKey: UserKey): Boolean {
        val accountKeys = Utils.getAccountKeys(context, args)
        if (accountKeys != null) {
            return ArrayUtils.contains(accountKeys, accountKey)
        }
        if (activatedAccountKeys != null) {
            return ArrayUtils.contains(activatedAccountKeys, accountKey)
        }
        return false
    }
}
