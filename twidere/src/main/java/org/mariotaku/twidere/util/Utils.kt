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
import android.annotation.TargetApi
import android.app.ActionBar
import android.app.Activity
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff.Mode
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.NinePatchDrawable
import android.graphics.drawable.TransitionDrawable
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.NfcAdapter.CreateNdefMessageCallback
import android.os.*
import android.provider.MediaStore
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.annotation.WorkerThread
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.app.Fragment
import android.support.v4.app.ListFragment
import android.support.v4.content.ContextCompat
import android.support.v4.net.ConnectivityManagerCompat
import android.support.v4.util.Pair
import android.support.v4.view.GravityCompat
import android.support.v4.view.accessibility.AccessibilityEventCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.menu.MenuBuilder
import android.system.ErrnoException
import android.text.TextUtils
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.text.format.Time
import android.transition.TransitionInflater
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.webkit.MimeTypeMap
import android.widget.AbsListView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import edu.tsinghua.hotmobi.HotMobiLogger
import edu.tsinghua.hotmobi.model.NotificationEvent
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.math.NumberUtils
import org.json.JSONException
import org.mariotaku.kpreferences.KPreferences
import org.mariotaku.ktextension.convert
import org.mariotaku.microblog.library.MicroBlogException
import org.mariotaku.microblog.library.twitter.model.GeoLocation
import org.mariotaku.microblog.library.twitter.model.Relationship
import org.mariotaku.sqliteqb.library.*
import org.mariotaku.sqliteqb.library.Columns.Column
import org.mariotaku.twidere.BuildConfig
import org.mariotaku.twidere.Constants.*
import org.mariotaku.twidere.R
import org.mariotaku.twidere.activity.CopyLinkActivity
import org.mariotaku.twidere.annotation.CustomTabType
import org.mariotaku.twidere.constant.bandwidthSavingModeKey
import org.mariotaku.twidere.constant.mediaPreviewKey
import org.mariotaku.twidere.fragment.*
import org.mariotaku.twidere.fragment.iface.IBaseFragment.SystemWindowsInsetsCallback
import org.mariotaku.twidere.graphic.PaddingDrawable
import org.mariotaku.twidere.model.*
import org.mariotaku.twidere.model.util.ParcelableCredentialsUtils
import org.mariotaku.twidere.model.util.ParcelableStatusUtils
import org.mariotaku.twidere.model.util.ParcelableUserUtils
import org.mariotaku.twidere.model.util.UserKeyUtils
import org.mariotaku.twidere.provider.TwidereDataStore
import org.mariotaku.twidere.provider.TwidereDataStore.*
import org.mariotaku.twidere.provider.TwidereDataStore.DirectMessages.ConversationEntries
import org.mariotaku.twidere.service.RefreshService
import org.mariotaku.twidere.util.TwidereLinkify.HighlightStyle
import org.mariotaku.twidere.util.TwidereLinkify.PATTERN_TWITTER_PROFILE_IMAGES
import org.mariotaku.twidere.view.CardMediaContainer.PreviewStyle
import org.mariotaku.twidere.view.ShapedImageView
import org.mariotaku.twidere.view.ShapedImageView.ShapeStyle
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.*
import java.util.regex.Pattern
import java.util.zip.CRC32
import javax.net.ssl.SSLException

@SuppressWarnings("unused")
object Utils {


    class NoAccountException : Exception()

    internal object UtilsL {

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        fun setSharedElementTransition(context: Context, window: Window, transitionRes: Int) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
            window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            val inflater = TransitionInflater.from(context)
            val transition = inflater.inflateTransition(transitionRes)
            window.sharedElementEnterTransition = transition
            window.sharedElementExitTransition = transition
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        fun getErrorNo(t: Throwable): Int {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return 0
            if (t is ErrnoException) {
                return t.errno
            }
            return 0
        }
    }


    val PATTERN_XML_RESOURCE_IDENTIFIER: Pattern = Pattern.compile("res/xml/([\\w_]+)\\.xml")
    val PATTERN_RESOURCE_IDENTIFIER: Pattern = Pattern.compile("@([\\w_]+)/([\\w_]+)")

    private val LINK_HANDLER_URI_MATCHER = UriMatcher(UriMatcher.NO_MATCH)
    private val HOME_TABS_URI_MATCHER = UriMatcher(UriMatcher.NO_MATCH)

    init {

        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_STATUS, null, LINK_ID_STATUS)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER, null, LINK_ID_USER)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_TIMELINE, null, LINK_ID_USER_TIMELINE)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_MEDIA_TIMELINE, null, LINK_ID_USER_MEDIA_TIMELINE)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_FOLLOWERS, null, LINK_ID_USER_FOLLOWERS)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_FRIENDS, null, LINK_ID_USER_FRIENDS)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_FAVORITES, null, LINK_ID_USER_FAVORITES)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_BLOCKS, null, LINK_ID_USER_BLOCKS)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_DIRECT_MESSAGES_CONVERSATION, null,
                LINK_ID_DIRECT_MESSAGES_CONVERSATION)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_DIRECT_MESSAGES, null, LINK_ID_DIRECT_MESSAGES)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_INTERACTIONS, null, LINK_ID_INTERACTIONS)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_PUBLIC_TIMELINE, null, LINK_ID_PUBLIC_TIMELINE)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_LIST, null, LINK_ID_USER_LIST)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_GROUP, null, LINK_ID_GROUP)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_LIST_TIMELINE, null, LINK_ID_USER_LIST_TIMELINE)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_LIST_MEMBERS, null, LINK_ID_USER_LIST_MEMBERS)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_LIST_SUBSCRIBERS, null, LINK_ID_USER_LIST_SUBSCRIBERS)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_LIST_MEMBERSHIPS, null, LINK_ID_USER_LIST_MEMBERSHIPS)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_LISTS, null, LINK_ID_USER_LISTS)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_GROUPS, null, LINK_ID_USER_GROUPS)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_SAVED_SEARCHES, null, LINK_ID_SAVED_SEARCHES)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_USER_MENTIONS, null, LINK_ID_USER_MENTIONS)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_INCOMING_FRIENDSHIPS, null, LINK_ID_INCOMING_FRIENDSHIPS)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_ITEMS, null, LINK_ID_ITEMS)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_STATUS_RETWEETERS, null, LINK_ID_STATUS_RETWEETERS)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_STATUS_FAVORITERS, null, LINK_ID_STATUS_FAVORITERS)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_SEARCH, null, LINK_ID_SEARCH)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_MUTES_USERS, null, LINK_ID_MUTES_USERS)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_MAP, null, LINK_ID_MAP)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_SCHEDULED_STATUSES, null, LINK_ID_SCHEDULED_STATUSES)

        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_ACCOUNTS, null, LINK_ID_ACCOUNTS)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_DRAFTS, null, LINK_ID_DRAFTS)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_FILTERS, null, LINK_ID_FILTERS)
        LINK_HANDLER_URI_MATCHER.addURI(AUTHORITY_PROFILE_EDITOR, null, LINK_ID_PROFILE_EDITOR)

        HOME_TABS_URI_MATCHER.addURI(CustomTabType.HOME_TIMELINE, null, TAB_CODE_HOME_TIMELINE)
        HOME_TABS_URI_MATCHER.addURI(CustomTabType.NOTIFICATIONS_TIMELINE, null, TAB_CODE_NOTIFICATIONS_TIMELINE)
        HOME_TABS_URI_MATCHER.addURI(CustomTabType.DIRECT_MESSAGES, null, TAB_CODE_DIRECT_MESSAGES)
    }

    fun addIntentToMenuForExtension(context: Context, menu: Menu,
                                    groupId: Int, action: String?,
                                    parcelableKey: String?, parcelableJSONKey: String,
                                    parcelable: Parcelable?) {
        if (action == null || parcelableKey == null || parcelable == null) return
        val pm = context.packageManager
        val res = context.resources
        val density = res.displayMetrics.density
        val padding = Math.round(density * 4)
        val queryIntent = Intent(action)
        queryIntent.setExtrasClassLoader(context.classLoader)
        val activities = pm.queryIntentActivities(queryIntent, PackageManager.GET_META_DATA)
        val parcelableJson = JsonSerializer.serialize(parcelable)
        for (info in activities) {
            val intent = Intent(queryIntent)
            if (isExtensionUseJSON(info) && parcelableJson != null) {
                intent.putExtra(parcelableJSONKey, parcelableJson)
            } else {
                intent.putExtra(parcelableKey, parcelable)
            }
            intent.setClassName(info.activityInfo.packageName, info.activityInfo.name)
            val item = menu.add(groupId, Menu.NONE, Menu.NONE, info.loadLabel(pm))
            item.intent = intent
            val metaDataDrawable = getMetadataDrawable(pm, info.activityInfo, METADATA_KEY_EXTENSION_ICON)
            val actionIconColor = ThemeUtils.getThemeForegroundColor(context)
            if (metaDataDrawable != null) {
                metaDataDrawable.mutate()
                metaDataDrawable.setColorFilter(actionIconColor, Mode.SRC_ATOP)
                item.icon = metaDataDrawable
            } else {
                val icon = info.loadIcon(pm)
                val iw = icon.intrinsicWidth
                val ih = icon.intrinsicHeight
                if (iw > 0 && ih > 0) {
                    val iconWithPadding = PaddingDrawable(icon, padding)
                    iconWithPadding.setBounds(0, 0, iw, ih)
                    item.icon = iconWithPadding
                } else {
                    item.icon = icon
                }
            }

        }
    }

    fun announceForAccessibilityCompat(context: Context, view: View, text: CharSequence,
                                       cls: Class<*>) {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        if (!accessibilityManager.isEnabled) return
        // Prior to SDK 16, announcements could only be made through FOCUSED
        // events. Jelly Bean (SDK 16) added support for speaking text verbatim
        // using the ANNOUNCEMENT event type.
        val eventType: Int
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            eventType = AccessibilityEvent.TYPE_VIEW_FOCUSED
        } else {
            eventType = AccessibilityEventCompat.TYPE_ANNOUNCEMENT
        }

        // Construct an accessibility event with the minimum recommended
        // attributes. An event without a class name or package may be dropped.
        val event = AccessibilityEvent.obtain(eventType)
        event.text.add(text)
        event.className = cls.name
        event.packageName = context.packageName
        event.setSource(view)

        // Sends the event directly through the accessibility manager. If your
        // application only targets SDK 14+, you should just call
        // getParent().requestSendAccessibilityEvent(this, event);
        accessibilityManager.sendAccessibilityEvent(event)
    }

    fun buildDirectMessageConversationUri(accountKey: UserKey, conversationId: String?,
                                          screenName: String?): Uri {
        if (conversationId == null && screenName == null) return TwidereDataStore.CONTENT_URI_NULL
        val builder: Uri.Builder
        if (conversationId != null) {
            builder = DirectMessages.Conversation.CONTENT_URI.buildUpon()
        } else {
            builder = DirectMessages.Conversation.CONTENT_URI_SCREEN_NAME.buildUpon()
        }
        builder.appendPath(accountKey.toString())
        if (conversationId != null) {
            builder.appendPath(conversationId.toString())
        } else {
            builder.appendPath(screenName)
        }
        return builder.build()
    }

    fun calculateInSampleSize(width: Int, height: Int, preferredWidth: Int,
                              preferredHeight: Int): Int {
        if (preferredHeight > height && preferredWidth > width) return 1
        val result = Math.round(Math.max(width, height) / Math.max(preferredWidth, preferredHeight).toFloat())
        return Math.max(1, result)
    }

    fun checkActivityValidity(context: Context, intent: Intent): Boolean {
        val pm = context.packageManager
        return !pm.queryIntentActivities(intent, 0).isEmpty()
    }

    fun clearListViewChoices(view: AbsListView?) {
        if (view == null) return
        val adapter = view.adapter ?: return
        view.clearChoices()
        var i = 0
        val j = view.childCount
        while (i < j) {
            view.setItemChecked(i, false)
            i++
        }
        view.post(Runnable { view.choiceMode = AbsListView.CHOICE_MODE_NONE })
        // Workaround for Android bug
        // http://stackoverflow.com/questions/9754170/listview-selection-remains-persistent-after-exiting-choice-mode
        //        final int position = view.getFirstVisiblePosition(), offset = Utils.getFirstChildOffset(view);
        //        view.setAdapter(adapter);
        //        Utils.scrollListToPosition(view, position, offset);
    }

    fun closeSilently(c: Closeable?): Boolean {
        if (c == null) return false
        try {
            c.close()
        } catch (e: IOException) {
            return false
        }

        return true
    }

    fun closeSilently(c: Cursor?): Boolean {
        if (c == null) return false
        c.close()
        return true
    }

    fun getAccountColors(accounts: Array<out ParcelableAccount>): IntArray {
        return IntArray(accounts.size) { i ->
            accounts[i].color
        }
    }

    @Throws(NoAccountException::class)
    fun createFragmentForIntent(context: Context, linkId: Int, intent: Intent): Fragment? {
        intent.setExtrasClassLoader(context.classLoader)
        val extras = intent.extras
        val uri = intent.data
        val fragment: Fragment
        if (uri == null) return null
        val args = Bundle()
        if (extras != null) {
            try {
                args.putAll(extras)
            } catch (e: BadParcelableException) {
                // When called by external app with wrong params
                return null
            }

        }
        var isAccountIdRequired = true
        when (linkId) {
            LINK_ID_ACCOUNTS -> {
                isAccountIdRequired = false
                fragment = AccountsManagerFragment()
            }
            LINK_ID_DRAFTS -> {
                isAccountIdRequired = false
                fragment = DraftsFragment()
            }
            LINK_ID_FILTERS -> {
                isAccountIdRequired = false
                fragment = FiltersFragment()
            }
            LINK_ID_PROFILE_EDITOR -> {
                fragment = UserProfileEditorFragment()
            }
            LINK_ID_MAP -> {
                isAccountIdRequired = false
                if (!args.containsKey(EXTRA_LATITUDE) && !args.containsKey(EXTRA_LONGITUDE)) {
                    val lat = NumberUtils.toDouble(uri.getQueryParameter(QUERY_PARAM_LAT), java.lang.Double.NaN)
                    val lng = NumberUtils.toDouble(uri.getQueryParameter(QUERY_PARAM_LNG), java.lang.Double.NaN)
                    if (java.lang.Double.isNaN(lat) || java.lang.Double.isNaN(lng)) return null
                    args.putDouble(EXTRA_LATITUDE, lat)
                    args.putDouble(EXTRA_LONGITUDE, lng)
                }
                fragment = MapFragmentFactory.getInstance().createMapFragment(context)
            }
            LINK_ID_STATUS -> {
                fragment = StatusFragment()
                if (!args.containsKey(EXTRA_STATUS_ID)) {
                    val paramStatusId = uri.getQueryParameter(QUERY_PARAM_STATUS_ID)
                    args.putString(EXTRA_STATUS_ID, paramStatusId)
                }
            }
            LINK_ID_USER -> {
                fragment = UserFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = UserKey.valueOf(getUserKeyParam(uri)!!)
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                }
                if (!args.containsKey(EXTRA_USER_KEY)) {
                    args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                }
                args.putString(EXTRA_REFERRAL, intent.getStringExtra(EXTRA_REFERRAL))
            }
            LINK_ID_USER_LIST_MEMBERSHIPS -> {
                fragment = UserListMembershipsFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = UserKey.valueOf(getUserKeyParam(uri)!!)
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                }
                if (!args.containsKey(EXTRA_USER_KEY)) {
                    args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                }
            }
            LINK_ID_USER_TIMELINE -> {
                fragment = UserTimelineFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = UserKey.valueOf(getUserKeyParam(uri)!!)
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                }
                if (!args.containsKey(EXTRA_USER_KEY)) {
                    args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                }
                if (TextUtils.isEmpty(paramScreenName) && paramUserKey == null) return null
            }
            LINK_ID_USER_MEDIA_TIMELINE -> {
                fragment = UserMediaTimelineFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = UserKey.valueOf(getUserKeyParam(uri)!!)
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                }
                if (!args.containsKey(EXTRA_USER_KEY)) {
                    args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                }
                if (TextUtils.isEmpty(paramScreenName) && paramUserKey == null) return null
            }
            LINK_ID_USER_FAVORITES -> {
                fragment = UserFavoritesFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = UserKey.valueOf(getUserKeyParam(uri)!!)
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                }
                if (!args.containsKey(EXTRA_USER_KEY)) {
                    args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                }
                if (!args.containsKey(EXTRA_SCREEN_NAME) && !args.containsKey(EXTRA_USER_KEY))
                    return null
            }
            LINK_ID_USER_FOLLOWERS -> {
                fragment = UserFollowersFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = UserKey.valueOf(getUserKeyParam(uri)!!)
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                }
                if (!args.containsKey(EXTRA_USER_KEY)) {
                    args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                }
                if (TextUtils.isEmpty(paramScreenName) && paramUserKey == null) return null
            }
            LINK_ID_USER_FRIENDS -> {
                fragment = UserFriendsFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = UserKey.valueOf(getUserKeyParam(uri)!!)
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                }
                if (!args.containsKey(EXTRA_USER_KEY)) {
                    args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                }
                if (TextUtils.isEmpty(paramScreenName) && paramUserKey == null) return null
            }
            LINK_ID_USER_BLOCKS -> {
                fragment = UserBlocksListFragment()
            }
            LINK_ID_MUTES_USERS -> {
                fragment = MutesUsersListFragment()
            }
            LINK_ID_DIRECT_MESSAGES_CONVERSATION -> {
                fragment = MessagesConversationFragment()
                isAccountIdRequired = false
                val paramRecipientId = uri.getQueryParameter(QUERY_PARAM_RECIPIENT_ID)
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                if (paramRecipientId != null) {
                    args.putString(EXTRA_RECIPIENT_ID, paramRecipientId)
                } else if (paramScreenName != null) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                }
            }
            LINK_ID_DIRECT_MESSAGES -> {
                fragment = DirectMessagesFragment()
            }
            LINK_ID_INTERACTIONS -> {
                fragment = InteractionsTimelineFragment()
            }
            LINK_ID_PUBLIC_TIMELINE -> {
                fragment = PublicTimelineFragment()
            }
            LINK_ID_USER_LIST -> {
                fragment = UserListFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = getUserKeyParam(uri)?.convert(UserKey::valueOf)
                val paramListId = uri.getQueryParameter(QUERY_PARAM_LIST_ID)
                val paramListName = uri.getQueryParameter(QUERY_PARAM_LIST_NAME)
                if ((TextUtils.isEmpty(paramListName) || TextUtils.isEmpty(paramScreenName) && paramUserKey == null) && TextUtils.isEmpty(paramListId)) {
                    return null
                }
                args.putString(EXTRA_LIST_ID, paramListId)
                args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                args.putString(EXTRA_LIST_NAME, paramListName)
            }
            LINK_ID_GROUP -> {
                fragment = GroupFragment()
                val paramGroupId = uri.getQueryParameter(QUERY_PARAM_GROUP_ID)
                val paramGroupName = uri.getQueryParameter(QUERY_PARAM_GROUP_NAME)
                if (TextUtils.isEmpty(paramGroupId) && TextUtils.isEmpty(paramGroupName))
                    return null
                args.putString(EXTRA_GROUP_ID, paramGroupId)
                args.putString(EXTRA_GROUP_NAME, paramGroupName)
            }
            LINK_ID_USER_LISTS -> {
                fragment = ListsFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = getUserKeyParam(uri)?.convert(UserKey::valueOf)
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                }
                if (!args.containsKey(EXTRA_USER_KEY)) {
                    args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                }
                if (TextUtils.isEmpty(paramScreenName) && paramUserKey == null) return null
            }
            LINK_ID_USER_GROUPS -> {
                fragment = UserGroupsFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = getUserKeyParam(uri)?.convert(UserKey::valueOf)
                if (!args.containsKey(EXTRA_SCREEN_NAME)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                }
                if (!args.containsKey(EXTRA_USER_KEY)) {
                    args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                }
                if (TextUtils.isEmpty(paramScreenName) && paramUserKey == null) return null
            }
            LINK_ID_USER_LIST_TIMELINE -> {
                fragment = UserListTimelineFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = getUserKeyParam(uri)?.convert(UserKey::valueOf)
                val paramListId = uri.getQueryParameter(QUERY_PARAM_LIST_ID)
                val paramListName = uri.getQueryParameter(QUERY_PARAM_LIST_NAME)
                if ((TextUtils.isEmpty(paramListName) || TextUtils.isEmpty(paramScreenName) && paramUserKey == null) && TextUtils.isEmpty(paramListId)) {
                    return null
                }
                args.putString(EXTRA_LIST_ID, paramListId)
                args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                args.putString(EXTRA_LIST_NAME, paramListName)
            }
            LINK_ID_USER_LIST_MEMBERS -> {
                fragment = UserListMembersFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = getUserKeyParam(uri)?.convert(UserKey::valueOf)
                val paramListId = uri.getQueryParameter(QUERY_PARAM_LIST_ID)
                val paramListName = uri.getQueryParameter(QUERY_PARAM_LIST_NAME)
                if ((TextUtils.isEmpty(paramListName) || TextUtils.isEmpty(paramScreenName) && paramUserKey == null) && TextUtils.isEmpty(paramListId))
                    return null
                args.putString(EXTRA_LIST_ID, paramListId)
                args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                args.putString(EXTRA_LIST_NAME, paramListName)
            }
            LINK_ID_USER_LIST_SUBSCRIBERS -> {
                fragment = UserListSubscribersFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                val paramUserKey = getUserKeyParam(uri)?.convert(UserKey::valueOf)
                val paramListId = uri.getQueryParameter(QUERY_PARAM_LIST_ID)
                val paramListName = uri.getQueryParameter(QUERY_PARAM_LIST_NAME)
                if (TextUtils.isEmpty(paramListId) && (TextUtils.isEmpty(paramListName) || TextUtils.isEmpty(paramScreenName) && paramUserKey == null))
                    return null
                args.putString(EXTRA_LIST_ID, paramListId)
                args.putParcelable(EXTRA_USER_KEY, paramUserKey)
                args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                args.putString(EXTRA_LIST_NAME, paramListName)
            }
            LINK_ID_SAVED_SEARCHES -> {
                fragment = SavedSearchesListFragment()
            }
            LINK_ID_USER_MENTIONS -> {
                fragment = UserMentionsFragment()
                val paramScreenName = uri.getQueryParameter(QUERY_PARAM_SCREEN_NAME)
                if (!args.containsKey(EXTRA_SCREEN_NAME) && !TextUtils.isEmpty(paramScreenName)) {
                    args.putString(EXTRA_SCREEN_NAME, paramScreenName)
                }
                if (TextUtils.isEmpty(args.getString(EXTRA_SCREEN_NAME))) return null
            }
            LINK_ID_INCOMING_FRIENDSHIPS -> {
                fragment = IncomingFriendshipsFragment()
            }
            LINK_ID_ITEMS -> {
                isAccountIdRequired = false
                fragment = ItemsListFragment()
            }
            LINK_ID_STATUS_RETWEETERS -> {
                fragment = StatusRetweetersListFragment()
                if (!args.containsKey(EXTRA_STATUS_ID)) {
                    val paramStatusId = uri.getQueryParameter(QUERY_PARAM_STATUS_ID)
                    args.putString(EXTRA_STATUS_ID, paramStatusId)
                }
            }
            LINK_ID_STATUS_FAVORITERS -> {
                fragment = StatusFavoritersListFragment()
                if (!args.containsKey(EXTRA_STATUS_ID)) {
                    val paramStatusId = uri.getQueryParameter(QUERY_PARAM_STATUS_ID)
                    args.putString(EXTRA_STATUS_ID, paramStatusId)
                }
            }
            LINK_ID_SEARCH -> {
                val paramQuery = uri.getQueryParameter(QUERY_PARAM_QUERY)
                if (!args.containsKey(EXTRA_QUERY) && !TextUtils.isEmpty(paramQuery)) {
                    args.putString(EXTRA_QUERY, paramQuery)
                }
                if (!args.containsKey(EXTRA_QUERY)) {
                    return null
                }
                fragment = SearchFragment()
            }
            else -> {
                return null
            }
        }
        var accountKey = args.getParcelable<UserKey>(EXTRA_ACCOUNT_KEY)
        if (accountKey == null) {
            accountKey = uri.getQueryParameter(QUERY_PARAM_ACCOUNT_KEY)?.convert(UserKey::valueOf)
        }
        if (accountKey == null) {
            val accountId = uri.getQueryParameter(QUERY_PARAM_ACCOUNT_ID)
            val paramAccountName = uri.getQueryParameter(QUERY_PARAM_ACCOUNT_NAME)
            if (accountId != null) {
                accountKey = DataStoreUtils.findAccountKey(context, accountId)
                args.putParcelable(EXTRA_ACCOUNT_KEY, accountKey)
            } else if (paramAccountName != null) {
                accountKey = DataStoreUtils.findAccountKeyByScreenName(context, paramAccountName)
            }
        }

        if (isAccountIdRequired && accountKey == null) {
            throw NoAccountException()
        }
        args.putParcelable(EXTRA_ACCOUNT_KEY, accountKey)
        fragment.arguments = args
        return fragment
    }

    fun getUserKeyParam(uri: Uri): String? {
        val paramUserKey = uri.getQueryParameter(QUERY_PARAM_USER_KEY) ?: return uri.getQueryParameter(QUERY_PARAM_USER_ID)
        return paramUserKey
    }

    fun createStatusShareIntent(context: Context, status: ParcelableStatus): Intent {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, IntentUtils.getStatusShareSubject(context, status))
        intent.putExtra(Intent.EXTRA_TEXT, IntentUtils.getStatusShareText(context, status))
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        return intent
    }

    fun getAccountKeys(context: Context, args: Bundle?): Array<UserKey>? {
        if (args == null) return null
        if (args.containsKey(EXTRA_ACCOUNT_KEYS)) {
            return newParcelableArray(args.getParcelableArray(EXTRA_ACCOUNT_KEYS), UserKey.CREATOR)
        } else if (args.containsKey(EXTRA_ACCOUNT_KEY)) {
            val accountKey = args.getParcelable<UserKey>(EXTRA_ACCOUNT_KEY) ?: return emptyArray()
            return arrayOf(accountKey)
        } else if (args.containsKey(EXTRA_ACCOUNT_ID)) {
            val accountId = args.get(EXTRA_ACCOUNT_ID).toString()
            try {
                if (java.lang.Long.parseLong(accountId) <= 0) return null
            } catch (e: NumberFormatException) {
                // Ignore
            }

            val accountKey = UserKeyUtils.findById(context, accountId)
            args.putParcelable(EXTRA_ACCOUNT_KEY, accountKey)
            if (accountKey == null) return arrayOf(UserKey(accountId, null))
            return arrayOf(accountKey)
        }
        return null
    }

    fun getAccountKey(context: Context, args: Bundle?): UserKey? {
        val accountKeys = getAccountKeys(context, args)
        if (ArrayUtils.isEmpty(accountKeys)) return null
        return accountKeys!![0]
    }

    fun getReadPositionTagWithAccount(tag: String,
                                      accountKey: UserKey?): String {
        if (accountKey == null) return tag
        return tag + "_" + accountKey
    }

    @Throws(IOException::class)
    fun encodeQueryParams(value: String): String {
        val encoded = URLEncoder.encode(value, "UTF-8")
        val buf = StringBuilder()
        val length = encoded.length
        var focus: Char
        var i = 0
        while (i < length) {
            focus = encoded[i]
            if (focus == '*') {
                buf.append("%2A")
            } else if (focus == '+') {
                buf.append("%20")
            } else if (focus == '%' && i + 1 < encoded.length && encoded[i + 1] == '7'
                    && encoded[i + 2] == 'E') {
                buf.append('~')
                i += 2
            } else {
                buf.append(focus)
            }
            i++
        }
        return buf.toString()
    }

    fun findDirectMessageInDatabases(context: Context,
                                     accountKey: UserKey,
                                     messageId: Long): ParcelableDirectMessage? {
        val resolver = context.contentResolver
        var message: ParcelableDirectMessage? = null
        val where = Expression.and(Expression.equalsArgs(DirectMessages.ACCOUNT_KEY),
                Expression.equalsArgs(DirectMessages.MESSAGE_ID)).sql
        val whereArgs = arrayOf(accountKey.toString(), messageId.toString())
        for (uri in DIRECT_MESSAGES_URIS) {
            val cur = resolver.query(uri, DirectMessages.COLUMNS, where, whereArgs, null) ?: continue
            if (cur.count > 0 && cur.moveToFirst()) {
                message = ParcelableDirectMessageCursorIndices.fromCursor(cur)
            }
            cur.close()
        }
        return message
    }

    @WorkerThread
    @Throws(MicroBlogException::class)
    fun findStatus(context: Context,
                   accountKey: UserKey,
                   statusId: String): ParcelableStatus {
        val cached = findStatusInDatabases(context, accountKey, statusId)
        if (cached != null) return cached
        val twitter = MicroBlogAPIFactory.getInstance(context, accountKey, true) ?: throw MicroBlogException("Account does not exist")
        val status = twitter.showStatus(statusId)
        val where = Expression.and(Expression.equalsArgs(Statuses.ACCOUNT_KEY),
                Expression.equalsArgs(Statuses.STATUS_ID)).sql
        val whereArgs = arrayOf(accountKey.toString(), statusId)
        val resolver = context.contentResolver
        resolver.delete(CachedStatuses.CONTENT_URI, where, whereArgs)
        resolver.insert(CachedStatuses.CONTENT_URI, ContentValuesCreator.createStatus(status, accountKey))
        return ParcelableStatusUtils.fromStatus(status, accountKey, false)
    }

    @WorkerThread
    fun findStatusInDatabases(context: Context,
                              accountKey: UserKey,
                              statusId: String): ParcelableStatus? {
        val resolver = context.contentResolver
        var status: ParcelableStatus? = null
        val where = Expression.and(Expression.equalsArgs(Statuses.ACCOUNT_KEY),
                Expression.equalsArgs(Statuses.STATUS_ID)).sql
        val whereArgs = arrayOf(accountKey.toString(), statusId)
        for (uri in STATUSES_URIS) {
            val cur = resolver.query(uri, Statuses.COLUMNS, where, whereArgs, null) ?: continue
            if (cur.count > 0) {
                cur.moveToFirst()
                status = ParcelableStatusCursorIndices.fromCursor(cur)
            }
            cur.close()
        }
        return status
    }

    @SuppressWarnings("deprecation")
    fun formatSameDayTime(context: Context, timestamp: Long): String? {
        if (DateUtils.isToday(timestamp))
            return DateUtils.formatDateTime(context, timestamp,
                    if (DateFormat.is24HourFormat(context))
                        DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_24HOUR
                    else
                        DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_12HOUR)
        return DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_SHOW_DATE)
    }

    @SuppressWarnings("deprecation")
    fun formatTimeStampString(context: Context, timestamp: Long): String? {
        val then = Time()
        then.set(timestamp)
        val now = Time()
        now.setToNow()

        var format_flags = DateUtils.FORMAT_NO_NOON_MIDNIGHT or DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_CAP_AMPM

        if (then.year != now.year) {
            format_flags = format_flags or (DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_DATE)
        } else if (then.yearDay != now.yearDay) {
            format_flags = format_flags or DateUtils.FORMAT_SHOW_DATE
        } else {
            format_flags = format_flags or DateUtils.FORMAT_SHOW_TIME
        }

        return DateUtils.formatDateTime(context, timestamp, format_flags)
    }

    @SuppressWarnings("deprecation")
    fun formatTimeStampString(context: Context, date_time: String): String? {
        return formatTimeStampString(context, Date.parse(date_time))
    }

    @SuppressWarnings("deprecation")
    fun formatToLongTimeString(context: Context, timestamp: Long): String? {
        val then = Time()
        then.set(timestamp)
        val now = Time()
        now.setToNow()

        var format_flags = DateUtils.FORMAT_NO_NOON_MIDNIGHT or DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_CAP_AMPM

        format_flags = format_flags or (DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)

        return DateUtils.formatDateTime(context, timestamp, format_flags)
    }

    fun getAccountNotificationId(notificationType: Int, accountId: Long): Int {
        return Arrays.hashCode(longArrayOf(notificationType.toLong(), accountId))
    }

    fun isComposeNowSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) return false
        return hasNavBar(context)
    }

    fun isOfficialCredentials(context: Context, accountKey: UserKey): Boolean {
        val credentials = ParcelableCredentialsUtils.getCredentials(context, accountKey) ?: return false
        return isOfficialCredentials(context, credentials)
    }

    fun isOfficialCredentials(context: Context,
                              account: ParcelableCredentials): Boolean {
        if (ParcelableAccount.Type.TWITTER == account.account_type) {
            val extra = JsonSerializer.parse(account.account_extras, TwitterAccountExtra::class.java)
            if (extra != null) {
                return extra.isOfficialCredentials
            }
        }
        val isOAuth = ParcelableCredentialsUtils.isOAuth(account.auth_type)
        val consumerKey = account.consumer_key
        val consumerSecret = account.consumer_secret
        return isOAuth && TwitterContentUtils.isOfficialKey(context, consumerKey, consumerSecret)
    }

    fun newSectionView(context: Context, titleRes: Int): TextView {
        return newSectionView(context, if (titleRes != 0) context.getString(titleRes) else null)
    }

    fun newSectionView(context: Context, title: CharSequence?): TextView {
        val textView = TextView(context, null, android.R.attr.listSeparatorTextViewStyle)
        textView.text = title
        return textView
    }

    fun setLastSeen(context: Context, entities: Array<ParcelableUserMention>?, time: Long): Boolean {
        if (entities == null) return false
        var result = false
        for (entity in entities) {
            result = result or setLastSeen(context, entity.key, time)
        }
        return result
    }

    fun setLastSeen(context: Context, userId: UserKey, time: Long): Boolean {
        val cr = context.contentResolver
        val values = ContentValues()
        if (time > 0) {
            values.put(CachedUsers.LAST_SEEN, time)
        } else {
            // Zero or negative value means remove last seen
            values.putNull(CachedUsers.LAST_SEEN)
        }
        val where = Expression.equalsArgs(CachedUsers.USER_KEY).sql
        val selectionArgs = arrayOf(userId.toString())
        return cr.update(CachedUsers.CONTENT_URI, values, where, selectionArgs) > 0
    }

    fun getBestCacheDir(context: Context, cacheDirName: String): File {
        val extCacheDir: File?
        try {
            // Workaround for https://github.com/mariotaku/twidere/issues/138
            extCacheDir = context.externalCacheDir
        } catch (e: Exception) {
            return File(context.cacheDir, cacheDirName)
        }

        if (extCacheDir != null && extCacheDir.isDirectory) {
            val cacheDir = File(extCacheDir, cacheDirName)
            if (cacheDir.isDirectory || cacheDir.mkdirs()) return cacheDir
        }
        return File(context.cacheDir, cacheDirName)
    }

    fun getBiggerTwitterProfileImage(url: String): String {
        return getTwitterProfileImageOfSize(url, "bigger")
    }

    fun getBitmap(drawable: Drawable): Bitmap? {
        if (drawable is NinePatchDrawable) return null
        if (drawable is BitmapDrawable)
            return drawable.bitmap
        else if (drawable is TransitionDrawable) {
            val layer_count = drawable.numberOfLayers
            for (i in 0..layer_count - 1) {
                val layer = drawable.getDrawable(i)
                if (layer is BitmapDrawable) return layer.bitmap
            }
        }
        return null
    }

    fun getBitmapCompressFormatByMimeType(mimeType: String,
                                          def: Bitmap.CompressFormat): Bitmap.CompressFormat {
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        if ("jpeg".equals(extension, ignoreCase = true) || "jpg".equals(extension, ignoreCase = true))
            return Bitmap.CompressFormat.JPEG
        else if ("png".equals(extension, ignoreCase = true))
            return Bitmap.CompressFormat.PNG
        else if ("webp".equals(extension, ignoreCase = true)) return Bitmap.CompressFormat.WEBP
        return def
    }

    fun getCardHighlightColor(context: Context, isMention: Boolean,
                              isFavorite: Boolean, isRetweet: Boolean): Int {
        if (isMention)
            return ContextCompat.getColor(context, R.color.highlight_reply)
        else if (isFavorite)
            return ContextCompat.getColor(context, R.color.highlight_like)
        else if (isRetweet) ContextCompat.getColor(context, R.color.highlight_retweet)
        return Color.TRANSPARENT
    }


    fun getColumnsFromProjection(projection: Array<String>?): Selectable {
        if (projection == null) return AllColumns()
        val length = projection.size
        val columns = arrayOfNulls<Column>(length)
        for (i in 0..length - 1) {
            columns[i] = Column(projection[i])
        }
        return Columns(*columns)
    }

    fun getDefaultAccountKey(context: Context): UserKey? {
        val prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE)
        val accountKey = UserKey.valueOf(prefs.getString(KEY_DEFAULT_ACCOUNT_KEY, null)!!)
        val accountKeys = DataStoreUtils.getAccountKeys(context)
        var idMatchIdx = -1
        var i = 0
        val accountIdsLength = accountKeys.size
        while (i < accountIdsLength) {
            if (accountKeys[i] == accountKey) {
                idMatchIdx = i
            }
            i++
        }
        if (idMatchIdx != -1) {
            return accountKeys[idMatchIdx]
        }
        if (accountKeys.size > 0 && !ArrayUtils.contains(accountKeys, accountKey)) {
            /* TODO: this is just a quick fix */
            return accountKeys[0]
        }
        return null
    }

    fun getDefaultTextSize(context: Context): Int {
        return context.resources.getInteger(R.integer.default_text_size)
    }

    fun getErrorMessage(context: Context, message: CharSequence?): String {
        if (TextUtils.isEmpty(message)) return context.getString(R.string.error_unknown_error)
        return context.getString(R.string.error_message, message)
    }

    fun getErrorMessage(context: Context, action: CharSequence?, message: CharSequence?): String {
        if (TextUtils.isEmpty(action)) return ParseUtils.parseString(message)
        if (TextUtils.isEmpty(message)) return context.getString(R.string.error_unknown_error)
        return context.getString(R.string.error_message_with_action, action, message)
    }

    fun getErrorMessage(context: Context, action: CharSequence?, t: Throwable?): String {
        if (t is MicroBlogException)
            return getTwitterErrorMessage(context, action, t)
        else if (t != null) return getErrorMessage(context, t.message)
        return context.getString(R.string.error_unknown_error)
    }

    fun getErrorMessage(context: Context, t: Throwable?): String {
        if (t is MicroBlogException) {
            return getTwitterErrorMessage(context, t)
        }
        return t?.message ?: context.getString(R.string.error_unknown_error)
    }

    fun getFirstChildOffset(list: AbsListView?): Int {
        if (list == null || list.childCount == 0) return 0
        val child = list.getChildAt(0)
        val location = IntArray(2)
        child.getLocationOnScreen(location)
        Log.d(LOGTAG, String.format("getFirstChildOffset %d vs %d", child.top, location[1]))
        return child.top
    }


    fun getImageMimeType(image: File?): String? {
        if (image == null) return null
        val o = BitmapFactory.Options()
        o.inJustDecodeBounds = true
        BitmapFactory.decodeFile(image.path, o)
        return o.outMimeType
    }

    fun getImageMimeType(stream: InputStream): String? {
        val o = BitmapFactory.Options()
        o.inJustDecodeBounds = true
        BitmapFactory.decodeStream(stream, null, o)
        return o.outMimeType
    }

    fun getImageMimeType(cr: ContentResolver, uri: Uri): String? {
        val o = BitmapFactory.Options()
        o.inJustDecodeBounds = true
        var stream: InputStream? = null
        try {
            stream = cr.openInputStream(uri) ?: return null
            return getImageMimeType(stream)
        } catch (e: IOException) {
            return null
        } finally {
            closeSilently(stream)
        }
    }

    fun getImagePathFromUri(context: Context, uri: Uri): String? {
        val mediaUriStart = ParseUtils.parseString(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        if (ParseUtils.parseString(uri).startsWith(mediaUriStart)) {

            val proj = arrayOf(MediaStore.Images.Media.DATA)
            val cur = context.contentResolver.query(uri, proj, null, null, null) ?: return null

            val idxData = cur.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

            cur.moveToFirst()
            try {
                return cur.getString(idxData)
            } finally {
                cur.close()
            }
        } else {
            val path = uri.path
            if (path != null && File(path).exists()) return path
        }
        return null
    }

    fun getMediaUploadStatus(links: Array<out CharSequence>?,
                             text: CharSequence): String {
        if (links == null) return ParseUtils.parseString(text)
        return "$text ${links.joinToString(" ")}"
    }

    fun getInternalCacheDir(context: Context, cacheDirName: String): File {
        val cacheDir = File(context.cacheDir, cacheDirName)
        if (cacheDir.isDirectory || cacheDir.mkdirs()) return cacheDir
        return File(context.cacheDir, cacheDirName)
    }

    fun getExternalCacheDir(context: Context, cacheDirName: String): File? {
        val externalCacheDir = context.externalCacheDir ?: return null
        val cacheDir = File(externalCacheDir, cacheDirName)
        if (cacheDir.isDirectory || cacheDir.mkdirs()) return cacheDir
        return File(context.cacheDir, cacheDirName)
    }

    fun getLinkHighlightingStyleName(context: Context): String? {
        val prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LINK_HIGHLIGHT_OPTION, VALUE_LINK_HIGHLIGHT_OPTION_NONE)
    }

    @HighlightStyle
    fun getLinkHighlightingStyle(context: Context): Int {
        return getLinkHighlightingStyleInt(getLinkHighlightingStyleName(context))
    }

    @HighlightStyle
    fun getLinkHighlightingStyleInt(option: String?): Int {
        if (option == null) return VALUE_LINK_HIGHLIGHT_OPTION_CODE_NONE
        when (option) {
            VALUE_LINK_HIGHLIGHT_OPTION_BOTH -> return VALUE_LINK_HIGHLIGHT_OPTION_CODE_BOTH
            VALUE_LINK_HIGHLIGHT_OPTION_HIGHLIGHT -> return VALUE_LINK_HIGHLIGHT_OPTION_CODE_HIGHLIGHT
            VALUE_LINK_HIGHLIGHT_OPTION_UNDERLINE -> return VALUE_LINK_HIGHLIGHT_OPTION_CODE_UNDERLINE
        }
        return VALUE_LINK_HIGHLIGHT_OPTION_CODE_NONE
    }

    fun getMatchedNicknameKeys(str: String, manager: UserColorNameManager): Array<String> {
        if (TextUtils.isEmpty(str)) return emptyArray()
        val list = ArrayList<String>()
        for ((key, value1) in manager.nameEntries) {
            val value = ParseUtils.parseString(value1)
            if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
                continue
            }
            if (TwidereStringUtils.startsWithIgnoreCase(value, str)) {
                list.add(key)
            }
        }
        return list.toTypedArray()
    }

    fun getNonEmptyString(pref: SharedPreferences?, key: String, def: String): String {
        if (pref == null) return def
        val `val` = pref.getString(key, def)
        return if (TextUtils.isEmpty(`val`)) def else `val`
    }

    fun getNormalTwitterProfileImage(url: String): String {
        return getTwitterProfileImageOfSize(url, "normal")
    }

    fun getNotificationUri(tableId: Int, def: Uri): Uri {
        when (tableId) {
            TABLE_ID_DIRECT_MESSAGES, TABLE_ID_DIRECT_MESSAGES_CONVERSATION, TABLE_ID_DIRECT_MESSAGES_CONVERSATION_SCREEN_NAME, TABLE_ID_DIRECT_MESSAGES_CONVERSATIONS_ENTRIES -> return DirectMessages.CONTENT_URI
        }
        return def
    }

    fun getOriginalTwitterProfileImage(url: String): String {
        val matcher = PATTERN_TWITTER_PROFILE_IMAGES.matcher(url)
        if (matcher.matches())
            return matcher.replaceFirst("$1$2/profile_images/$3/$4$6")
        return url
    }

    @ShapeStyle
    fun getProfileImageStyle(context: Context): Int {
        val prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val style = prefs.getString(KEY_PROFILE_IMAGE_STYLE, null)
        return getProfileImageStyle(style)
    }

    @ShapeStyle
    fun getProfileImageStyle(prefs: SharedPreferences): Int {
        val style = prefs.getString(KEY_PROFILE_IMAGE_STYLE, null)
        return getProfileImageStyle(style)
    }

    @ShapeStyle
    fun getProfileImageStyle(style: String): Int {
        if (VALUE_PROFILE_IMAGE_STYLE_SQUARE.equals(style, ignoreCase = true)) {
            return ShapedImageView.SHAPE_RECTANGLE
        }
        return ShapedImageView.SHAPE_CIRCLE
    }

    @PreviewStyle
    fun getMediaPreviewStyle(style: String): Int {
        if (VALUE_MEDIA_PREVIEW_STYLE_SCALE.equals(style, ignoreCase = true)) {
            return VALUE_MEDIA_PREVIEW_STYLE_CODE_SCALE
        }
        return VALUE_MEDIA_PREVIEW_STYLE_CODE_CROP
    }

    fun getQuoteStatus(context: Context, status: ParcelableStatus): String? {
        var quoteFormat: String = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).getString(
                KEY_QUOTE_FORMAT, DEFAULT_QUOTE_FORMAT)
        if (TextUtils.isEmpty(quoteFormat)) {
            quoteFormat = DEFAULT_QUOTE_FORMAT
        }
        var result = quoteFormat.replace(FORMAT_PATTERN_LINK, LinkCreator.getStatusWebLink(status).toString())
        result = result.replace(FORMAT_PATTERN_NAME, status.user_screen_name)
        result = result.replace(FORMAT_PATTERN_TEXT, status.text_plain)
        return result
    }

    fun getReasonablySmallTwitterProfileImage(url: String): String {
        return getTwitterProfileImageOfSize(url, "reasonably_small")
    }

    fun getResId(context: Context, string: String?): Int {
        if (string == null) return 0
        var m = PATTERN_RESOURCE_IDENTIFIER.matcher(string)
        val res = context.resources
        if (m.matches()) return res.getIdentifier(m.group(2), m.group(1), context.packageName)
        m = PATTERN_XML_RESOURCE_IDENTIFIER.matcher(string)
        if (m.matches()) return res.getIdentifier(m.group(1), "xml", context.packageName)
        return 0
    }


    fun getSenderUserName(context: Context, user: ParcelableDirectMessage?): String? {
        if (user == null) return null
        val prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val display_name = prefs.getBoolean(KEY_NAME_FIRST, true)
        return if (display_name) user.sender_name else "@" + user.sender_screen_name
    }

    fun getShareStatus(context: Context, title: String?, text: String?): String {
        var shareFormat: String = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).getString(
                KEY_SHARE_FORMAT, DEFAULT_SHARE_FORMAT)
        if (TextUtils.isEmpty(shareFormat)) {
            shareFormat = DEFAULT_SHARE_FORMAT
        }
        if (TextUtils.isEmpty(title)) return text ?: ""
        return shareFormat.replace(FORMAT_PATTERN_TITLE, title!!).replace(FORMAT_PATTERN_TEXT, text ?: "")
    }

    fun getTabDisplayOption(context: Context): String {
        val defaultOption = context.getString(R.string.default_tab_display_option)
        val prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TAB_DISPLAY_OPTION, defaultOption) ?: defaultOption
    }

    fun getTabDisplayOptionInt(context: Context): Int {
        return getTabDisplayOptionInt(getTabDisplayOption(context))
    }

    fun getTabDisplayOptionInt(option: String): Int {
        if (VALUE_TAB_DISPLAY_OPTION_ICON == option)
            return VALUE_TAB_DISPLAY_OPTION_CODE_ICON
        else if (VALUE_TAB_DISPLAY_OPTION_LABEL == option)
            return VALUE_TAB_DISPLAY_OPTION_CODE_LABEL
        return VALUE_TAB_DISPLAY_OPTION_CODE_BOTH
    }

    fun getTimestampFromDate(date: Date?): Long {
        if (date == null) return -1
        return date.time
    }

    fun hasNavBar(context: Context): Boolean {
        val resources = context.resources ?: return false
        val id = resources.getIdentifier("config_showNavigationBar", "bool", "android")
        if (id > 0) {
            return resources.getBoolean(id)
        } else {
            // Check for keys
            return !KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK) && !KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME)
        }
    }

    fun getTwitterErrorMessage(context: Context, action: CharSequence?,
                               te: MicroBlogException?): String {
        if (te == null) return context.getString(R.string.error_unknown_error)
        if (te.exceededRateLimitation()) {
            val status = te.rateLimitStatus
            val secUntilReset = (status.secondsUntilReset * 1000).toLong()
            val nextResetTime = ParseUtils.parseString(DateUtils.getRelativeTimeSpanString(System.currentTimeMillis() + secUntilReset))
            if (TextUtils.isEmpty(action))
                return context.getString(R.string.error_message_rate_limit, nextResetTime.trim { it <= ' ' })
            return context.getString(R.string.error_message_rate_limit_with_action, action, nextResetTime.trim { it <= ' ' })
        } else if (te.errorCode > 0) {
            val msg = StatusCodeMessageUtils.getTwitterErrorMessage(context, te.errorCode)
            return getErrorMessage(context, action, msg ?: te.message)
        } else if (te.cause is SSLException) {
            val msg = te.cause?.message
            if (msg != null && msg.contains("!="))
                return getErrorMessage(context, action, context.getString(R.string.ssl_error))
            else
                return getErrorMessage(context, action, context.getString(R.string.network_error))
        } else if (te.cause is IOException)
            return getErrorMessage(context, action, context.getString(R.string.network_error))
        else if (te.cause is JSONException)
            return getErrorMessage(context, action, context.getString(R.string.api_data_corrupted))
        else
            return getErrorMessage(context, action, te.message)
    }

    fun getTwitterErrorMessage(context: Context, te: MicroBlogException): String {
        if (StatusCodeMessageUtils.containsTwitterError(te.errorCode)) {
            return StatusCodeMessageUtils.getTwitterErrorMessage(context, te.errorCode)
        } else if (StatusCodeMessageUtils.containsHttpStatus(te.statusCode)) {
            return StatusCodeMessageUtils.getHttpStatusMessage(context, te.statusCode)
        } else if (te.errorMessage != null) {
            return te.errorMessage
        }
        return te.message ?: context.getString(R.string.error_unknown_error)
    }


    fun getTwitterProfileImageOfSize(url: String, size: String): String {
        val matcher = PATTERN_TWITTER_PROFILE_IMAGES.matcher(url)
        if (matcher.matches()) {
            return matcher.replaceFirst("$1$2/profile_images/$3/$4_$size$6")
        }
        return url
    }

    @DrawableRes
    fun getUserTypeIconRes(isVerified: Boolean, isProtected: Boolean): Int {
        if (isVerified)
            return R.drawable.ic_user_type_verified
        else if (isProtected) return R.drawable.ic_user_type_protected
        return 0
    }

    @StringRes
    fun getUserTypeDescriptionRes(isVerified: Boolean, isProtected: Boolean): Int {
        if (isVerified)
            return R.string.user_type_verified
        else if (isProtected) return R.string.user_type_protected
        return 0
    }

    fun hasAccountSignedWithOfficialKeys(context: Context): Boolean {
        if (context == null) return false
        val cur = context.contentResolver.query(Accounts.CONTENT_URI, Accounts.COLUMNS, null, null, null) ?: return false
        val keySecrets = context.resources.getStringArray(R.array.values_official_consumer_secret_crc32)
        val indices = ParcelableCredentialsCursorIndices(cur)
        cur.moveToFirst()
        val crc32 = CRC32()
        try {
            while (!cur.isAfterLast) {
                val consumerSecret = cur.getString(indices.consumer_secret)
                if (consumerSecret != null) {
                    val consumerSecretBytes = consumerSecret.toByteArray(Charset.forName("UTF-8"))
                    crc32.update(consumerSecretBytes, 0, consumerSecretBytes.size)
                    val value = crc32.value
                    crc32.reset()
                    for (keySecret in keySecrets) {
                        if (java.lang.Long.parseLong(keySecret, 16) == value) return true
                    }
                }
                cur.moveToNext()
            }
        } finally {
            cur.close()
        }
        return false
    }

    fun hasAutoRefreshAccounts(context: Context): Boolean {
        val accountKeys = DataStoreUtils.getAccountKeys(context)
        return !ArrayUtils.isEmpty(AccountPreferences.getAutoRefreshEnabledAccountIds(context, accountKeys))
    }

    fun hasStaggeredTimeline(): Boolean {
        return false
    }

    fun isBatteryOkay(context: Context): Boolean {
        val app = context.applicationContext
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = app.registerReceiver(null, filter) ?: return false
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0).toFloat()
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).toFloat()
        return plugged || level / scale > 0.15f
    }

    fun isDatabaseReady(context: Context): Boolean {
        val c = context.contentResolver.query(TwidereDataStore.CONTENT_URI_DATABASE_READY, null, null, null,
                null)
        try {
            return c != null
        } finally {
            c?.close()
        }
    }

    fun isMyAccount(context: Context, accountKey: UserKey?): Boolean {
        if (accountKey == null) return false
        val projection = arrayOf(SQLFunctions.COUNT())
        val cur = DataStoreUtils.getAccountCursor(context, projection, accountKey) ?: return false
        try {
            if (cur.moveToFirst()) return cur.getLong(0) > 0
        } finally {
            cur.close()
        }
        return false
    }

    fun isMyAccount(context: Context, screen_name: String): Boolean {
        val resolver = context.contentResolver
        val where = Expression.equalsArgs(Accounts.SCREEN_NAME).sql
        val projection = arrayOfNulls<String>(0)
        val cur = resolver.query(Accounts.CONTENT_URI, projection, where, arrayOf(screen_name), null)
        try {
            return cur != null && cur.count > 0
        } finally {
            cur?.close()
        }
    }

    fun isMyRetweet(status: ParcelableStatus?): Boolean {
        return status != null && isMyRetweet(status.account_key, status.retweeted_by_user_key,
                status.my_retweet_id)
    }

    fun isMyRetweet(accountId: UserKey, retweetedById: UserKey?, myRetweetId: String?): Boolean {
        return accountId == retweetedById || myRetweetId != null
    }

    fun isNetworkAvailable(context: Context): Boolean {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val info = cm.activeNetworkInfo
            return info != null && info.isConnected
        } catch (e: SecurityException) {
            return true
        }

    }

    @Deprecated("")
    fun isUserLoggedIn(context: Context, accountId: String): Boolean {
        val ids = DataStoreUtils.getAccountKeys(context)
        for (id in ids) {
            if (TextUtils.equals(id.id, accountId)) return true
        }
        return false
    }

    fun matchLinkId(uri: Uri?): Int {
        if (uri == null) return UriMatcher.NO_MATCH
        return LINK_HANDLER_URI_MATCHER.match(uri)
    }


    fun matchTabCode(uri: Uri?): Int {
        if (uri == null) return UriMatcher.NO_MATCH
        return HOME_TABS_URI_MATCHER.match(uri)
    }


    @CustomTabType
    fun matchTabType(uri: Uri?): String? {
        return getTabType(matchTabCode(uri))
    }

    @CustomTabType
    fun getTabType(code: Int): String? {
        when (code) {
            TAB_CODE_HOME_TIMELINE -> {
                return CustomTabType.HOME_TIMELINE
            }
            TAB_CODE_NOTIFICATIONS_TIMELINE -> {
                return CustomTabType.NOTIFICATIONS_TIMELINE
            }
            TAB_CODE_DIRECT_MESSAGES -> {
                return CustomTabType.DIRECT_MESSAGES
            }
        }
        return null
    }


    @SuppressWarnings("SuspiciousSystemArraycopy")
    fun <T : Parcelable> newParcelableArray(array: Array<Parcelable>?, creator: Parcelable.Creator<T>): Array<T>? {
        if (array == null) return null
        val result = creator.newArray(array.size)
        System.arraycopy(array, 0, result, 0, array.size)
        return result
    }

    fun setNdefPushMessageCallback(activity: Activity, callback: CreateNdefMessageCallback): Boolean {
        try {
            val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return false
            adapter.setNdefPushMessageCallback(callback, activity)
            return true
        } catch (e: SecurityException) {
            Log.w(LOGTAG, e)
        }

        return false
    }

    fun getInsetsTopWithoutActionBarHeight(context: Context, top: Int): Int {
        val actionBarHeight: Int
        if (context is AppCompatActivity) {
            actionBarHeight = getActionBarHeight(context.supportActionBar)
        } else if (context is Activity) {
            actionBarHeight = getActionBarHeight(context.actionBar)
        } else {
            return top
        }
        if (actionBarHeight > top) {
            return top
        }
        return top - actionBarHeight
    }

    fun getInsetsTopWithoutActionBarHeight(context: Context, top: Int, actionBarHeight: Int): Int {
        if (actionBarHeight > top) {
            return top
        }
        return top - actionBarHeight
    }

    fun restartActivity(activity: Activity?) {
        if (activity == null) return
        val enterAnim = android.R.anim.fade_in
        val exitAnim = android.R.anim.fade_out
        activity.finish()
        activity.overridePendingTransition(enterAnim, exitAnim)
        activity.startActivity(activity.intent)
        activity.overridePendingTransition(enterAnim, exitAnim)
    }

    fun scrollListToPosition(list: AbsListView, position: Int) {
        scrollListToPosition(list, position, 0)
    }

    fun scrollListToPosition(absListView: AbsListView?, position: Int, offset: Int) {
        if (absListView == null) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            if (absListView is ListView) {
                absListView.setSelectionFromTop(position, offset)
            } else {
                absListView.setSelection(position)
            }
            stopListView(absListView)
        } else {
            stopListView(absListView)
            if (absListView is ListView) {
                absListView.setSelectionFromTop(position, offset)
            } else {
                absListView.setSelection(position)
            }
        }
    }

    fun scrollListToTop(list: AbsListView?) {
        if (list == null) return
        scrollListToPosition(list, 0)
    }

    fun addCopyLinkIntent(context: Context, chooserIntent: Intent, uri: Uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val copyLinkIntent = Intent(context, CopyLinkActivity::class.java)
        copyLinkIntent.data = uri
        val alternateIntents = arrayOf(copyLinkIntent)
        chooserIntent.putExtra(Intent.EXTRA_ALTERNATE_INTENTS, alternateIntents)
    }

    internal fun isMyStatus(status: ParcelableStatus): Boolean {
        if (isMyRetweet(status)) return true
        return status.account_key.maybeEquals(status.user_key)
    }

    fun shouldStopAutoRefreshOnBatteryLow(context: Context): Boolean {
        val mPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE)
        return mPreferences.getBoolean(KEY_STOP_AUTO_REFRESH_WHEN_BATTERY_LOW, true)
    }

    fun showErrorMessage(context: Context, message: CharSequence, longMessage: Boolean) {
        val toast = Toast.makeText(context, message, if (longMessage) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
        toast.show()
    }

    fun showErrorMessage(context: Context, action: CharSequence,
                         message: CharSequence, longMessage: Boolean) {
        showErrorMessage(context, getErrorMessage(context, action, message), longMessage)
    }

    fun showErrorMessage(context: Context, action: CharSequence?,
                         t: Throwable?, longMessage: Boolean) {
        if (t is MicroBlogException) {
            showTwitterErrorMessage(context, action, t, longMessage)
            return
        }
        showErrorMessage(context, getErrorMessage(context, action, t), longMessage)
    }

    fun showErrorMessage(context: Context, actionRes: Int, desc: String,
                         longMessage: Boolean) {
        showErrorMessage(context, context.getString(actionRes), desc, longMessage)
    }

    fun showErrorMessage(context: Context, action: Int,
                         t: Throwable?,
                         long_message: Boolean) {
        showErrorMessage(context, context.getString(action), t, long_message)
    }

    fun showInfoMessage(context: Context, message: CharSequence, long_message: Boolean) {
        if (TextUtils.isEmpty(message)) return
        val toast = Toast.makeText(context, message, if (long_message) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
        toast.show()
    }

    fun showInfoMessage(context: Context, resId: Int, long_message: Boolean) {
        showInfoMessage(context, context.getText(resId), long_message)
    }

    fun showMenuItemToast(v: View, text: CharSequence) {
        val screenPos = IntArray(2)
        val displayFrame = Rect()
        v.getLocationOnScreen(screenPos)
        v.getWindowVisibleDisplayFrame(displayFrame)
        val height = v.height
        val midy = screenPos[1] + height / 2
        showMenuItemToast(v, text, midy >= displayFrame.height())
    }

    fun showMenuItemToast(v: View, text: CharSequence, isBottomBar: Boolean) {
        val screenPos = IntArray(2)
        val displayFrame = Rect()
        v.getLocationOnScreen(screenPos)
        v.getWindowVisibleDisplayFrame(displayFrame)
        val width = v.width
        val height = v.height
        val screenWidth = v.resources.displayMetrics.widthPixels
        val cheatSheet = Toast.makeText(v.context.applicationContext, text, Toast.LENGTH_SHORT)
        if (isBottomBar) {
            // Show along the bottom center
            cheatSheet.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, height)
        } else {
            // Show along the top; follow action buttons
            cheatSheet.setGravity(Gravity.TOP or GravityCompat.END, screenWidth - screenPos[0] - width / 2, height)
        }
        cheatSheet.show()
    }

    fun showOkMessage(context: Context, message: CharSequence, longMessage: Boolean) {
        if (context == null || TextUtils.isEmpty(message)) return
        val toast = Toast.makeText(context, message, if (longMessage) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
        toast.show()
    }

    fun showOkMessage(context: Context, resId: Int, long_message: Boolean) {
        if (context == null) return
        showOkMessage(context, context.getText(resId), long_message)
    }

    fun showTwitterErrorMessage(context: Context, action: CharSequence?,
                                te: MicroBlogException, long_message: Boolean) {
        val message: String
        if (action != null) {
            val status = te.rateLimitStatus
            if (te.exceededRateLimitation() && status != null) {
                val secUntilReset = (status.secondsUntilReset * 1000).toLong()
                val nextResetTime = ParseUtils.parseString(DateUtils.getRelativeTimeSpanString(System.currentTimeMillis() + secUntilReset))
                message = context.getString(R.string.error_message_rate_limit_with_action, action,
                        nextResetTime.trim { it <= ' ' })
            } else if (isErrorCodeMessageSupported(te)) {
                val msg = StatusCodeMessageUtils.getMessage(context, te.statusCode, te.errorCode)
                message = context.getString(R.string.error_message_with_action, action, msg ?: te.message)
            } else if (!TextUtils.isEmpty(te.errorMessage)) {
                message = context.getString(R.string.error_message_with_action, action,
                        te.errorMessage)
            } else if (te.cause is SSLException) {
                val msg = te.cause?.message
                if (msg != null && msg.contains("!=")) {
                    message = context.getString(R.string.error_message_with_action, action,
                            context.getString(R.string.ssl_error))
                } else {
                    message = context.getString(R.string.error_message_with_action, action,
                            context.getString(R.string.network_error))
                }
            } else if (te.cause is IOException) {
                message = context.getString(R.string.error_message_with_action, action,
                        context.getString(R.string.network_error))
            } else {
                message = context.getString(R.string.error_message_with_action, action,
                        te.message)
            }
        } else {
            message = context.getString(R.string.error_message, te.message)
        }
        showErrorMessage(context, message, long_message)
    }

    fun showWarnMessage(context: Context, message: CharSequence, longMessage: Boolean) {
        if (TextUtils.isEmpty(message)) return
        val toast = Toast.makeText(context, message, if (longMessage) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
        toast.show()
    }

    fun showWarnMessage(context: Context, resId: Int, long_message: Boolean) {
        showWarnMessage(context, context.getText(resId), long_message)
    }

    fun startRefreshServiceIfNeeded(context: Context) {
        val appContext = context.applicationContext ?: return
        val refreshServiceIntent = Intent(appContext, RefreshService::class.java)
        AsyncTask.execute {
            if (isNetworkAvailable(appContext) && hasAutoRefreshAccounts(appContext)) {
                if (BuildConfig.DEBUG) {
                    Log.d(LOGTAG, "Start background refresh service")
                }
                appContext.startService(refreshServiceIntent)
            } else {
                appContext.stopService(refreshServiceIntent)
            }
        }
    }

    fun startStatusShareChooser(context: Context, status: ParcelableStatus) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        val name = status.user_name
        val screenName = status.user_screen_name
        val timeString = formatToLongTimeString(context, status.timestamp)
        val subject = context.getString(R.string.status_share_subject_format_with_time, name, screenName, timeString)
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, status.text_plain)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
    }

    fun stopListView(list: AbsListView?) {
        if (list == null) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            list.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_CANCEL, 0f, 0f, 0))
        } else {
            list.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_DOWN, 0f, 0f, 0))
            list.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_UP, 0f, 0f, 0))
        }
    }

    fun trim(str: String?): String? {
        return str?.trim { it <= ' ' }
    }

    fun updateRelationship(context: Context, accountKey: UserKey, userKey: UserKey,
                           relationship: Relationship) {
        val resolver = context.contentResolver
        val values = ContentValuesCreator.createCachedRelationship(relationship,
                accountKey, userKey)
        resolver.insert(CachedRelationships.CONTENT_URI, values)
    }

    fun useShareScreenshot(): Boolean {
        return java.lang.Boolean.parseBoolean("false")
    }

    private fun getMetadataDrawable(pm: PackageManager?, info: ActivityInfo?, key: String?): Drawable? {
        if (pm == null || info == null || info.metaData == null || key == null || !info.metaData.containsKey(key))
            return null
        return pm.getDrawable(info.packageName, info.metaData.getInt(key), info.applicationInfo)
    }

    private fun isErrorCodeMessageSupported(te: MicroBlogException?): Boolean {
        if (te == null) return false
        return StatusCodeMessageUtils.containsHttpStatus(te.statusCode) || StatusCodeMessageUtils.containsTwitterError(te.errorCode)
    }

    private fun isExtensionUseJSON(info: ResolveInfo?): Boolean {
        if (info == null || info.activityInfo == null) return false
        val activityInfo = info.activityInfo
        if (activityInfo.metaData != null && activityInfo.metaData.containsKey(METADATA_KEY_EXTENSION_USE_JSON))
            return activityInfo.metaData.getBoolean(METADATA_KEY_EXTENSION_USE_JSON)
        val appInfo = activityInfo.applicationInfo ?: return false
        return appInfo.metaData != null && appInfo.metaData.getBoolean(METADATA_KEY_EXTENSION_USE_JSON, false)
    }

    fun getActionBarHeight(actionBar: ActionBar?): Int {
        if (actionBar == null) return 0
        val context = actionBar.themedContext
        val tv = TypedValue()
        val height = actionBar.height
        if (height > 0) return height
        if (context.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, context.resources.displayMetrics)
        }
        return 0
    }

    fun getActionBarHeight(actionBar: android.support.v7.app.ActionBar?): Int {
        if (actionBar == null) return 0
        val context = actionBar.themedContext
        val tv = TypedValue()
        val height = actionBar.height
        if (height > 0) return height
        if (context.theme.resolveAttribute(R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, context.resources.displayMetrics)
        }
        return 0
    }

    fun makeListFragmentFitsSystemWindows(fragment: ListFragment) {
        val activity = fragment.activity
        if (activity !is SystemWindowsInsetsCallback) return
        val insets = Rect()
        if (activity.getSystemWindowsInsets(insets)) {
            makeListFragmentFitsSystemWindows(fragment, insets)
        }
    }


    fun makeListFragmentFitsSystemWindows(fragment: ListFragment, insets: Rect) {
        val listView = fragment.listView
        listView.setPadding(insets.left, insets.top, insets.right, insets.bottom)
        listView.clipToPadding = false
    }

    fun getUserForConversation(context: Context,
                               accountKey: UserKey,
                               conversationId: String): ParcelableUser? {
        val cr = context.contentResolver
        val where = Expression.and(Expression.equalsArgs(ConversationEntries.ACCOUNT_KEY),
                Expression.equalsArgs(ConversationEntries.CONVERSATION_ID))
        val whereArgs = arrayOf(accountKey.toString(), conversationId)
        val c = cr.query(ConversationEntries.CONTENT_URI, null, where.sql, whereArgs,
                null) ?: return null
        try {
            if (c.moveToFirst())
                return ParcelableUserUtils.fromDirectMessageConversationEntry(c)
        } finally {
            c.close()
        }
        return null
    }

    @SafeVarargs
    fun makeSceneTransitionOption(activity: Activity,
                                  vararg sharedElements: Pair<View, String>): Bundle? {
        if (ThemeUtils.isTransparentBackground(activity)) return null
        return ActivityOptionsCompat.makeSceneTransitionAnimation(activity, *sharedElements).toBundle()
    }


    fun setSharedElementTransition(context: Context, window: Window, transitionRes: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
        UtilsL.setSharedElementTransition(context, window, transitionRes)
    }

    fun <T> findFieldOfTypes(obj: T, cls: Class<out T>, vararg checkTypes: Class<*>): Any? {
        labelField@ for (field in cls.declaredFields) {
            field.isAccessible = true
            val fieldObj: Any?
            try {
                fieldObj = field.get(obj)
            } catch (ignore: Exception) {
                continue
            }

            if (fieldObj != null) {
                val type = fieldObj.javaClass
                for (checkType in checkTypes) {
                    if (!checkType.isAssignableFrom(type)) continue@labelField
                }
                return fieldObj
            }
        }
        return null
    }

    fun isCustomConsumerKeySecret(consumerKey: String?, consumerSecret: String?): Boolean {
        if (TextUtils.isEmpty(consumerKey) || TextUtils.isEmpty(consumerSecret)) return false
        return TWITTER_CONSUMER_KEY != consumerKey && TWITTER_CONSUMER_SECRET != consumerKey
                && TWITTER_CONSUMER_KEY_LEGACY != consumerKey && TWITTER_CONSUMER_SECRET_LEGACY != consumerSecret
    }

    val isStreamingEnabled: Boolean
        get() = java.lang.Boolean.parseBoolean("false")

    fun getErrorNo(t: Throwable): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return 0
        return UtilsL.getErrorNo(t)
    }

    fun isOutOfMemory(ex: Throwable?): Boolean {
        if (ex == null) return false
        val cause = ex.cause
        if (cause == null || cause === ex) return false
        if (cause is OutOfMemoryError) return true
        return isOutOfMemory(cause)
    }

    fun logOpenNotificationFromUri(context: Context, uri: Uri) {
        if (!uri.getBooleanQueryParameter(QUERY_PARAM_FROM_NOTIFICATION, false)) return
        val type = uri.getQueryParameter(QUERY_PARAM_NOTIFICATION_TYPE)
        val accountKey = uri.getQueryParameter(QUERY_PARAM_ACCOUNT_KEY)?.convert(UserKey::valueOf)
        val itemId = NumberUtils.toLong(UriExtraUtils.getExtra(uri, "item_id"), -1)
        val itemUserId = NumberUtils.toLong(UriExtraUtils.getExtra(uri, "item_user_id"), -1)
        val itemUserFollowing = java.lang.Boolean.parseBoolean(UriExtraUtils.getExtra(uri, "item_user_following"))
        val timestamp = NumberUtils.toLong(uri.getQueryParameter(QUERY_PARAM_TIMESTAMP), -1)
        if (!NotificationEvent.isSupported(type) || accountKey == null || itemId < 0 || timestamp < 0)
            return
        val event = NotificationEvent.open(context, timestamp, type,
                accountKey.id, itemId, itemUserId, itemUserFollowing)
        HotMobiLogger.getInstance(context).log(accountKey, event)
    }

    fun hasOfficialAPIAccess(context: Context, account: ParcelableCredentials): Boolean {
        if (ParcelableAccount.Type.TWITTER == account.account_type) {
            val extra = JsonSerializer.parse(account.account_extras, TwitterAccountExtra::class.java)
            if (extra != null) {
                return extra.isOfficialCredentials
            }
        }
        val isOAuth = ParcelableCredentialsUtils.isOAuth(account.auth_type)
        val consumerKey = account.consumer_key
        val consumerSecret = account.consumer_secret
        return isOAuth && TwitterContentUtils.isOfficialKey(context, consumerKey, consumerSecret)
    }

    fun getNotificationId(baseId: Int, accountId: UserKey?): Int {
        var result = baseId
        result = 31 * result + if (accountId != null) accountId.hashCode() else 0
        return result
    }

    @SuppressLint("InlinedApi")
    fun isCharging(context: Context): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return false
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        return plugged == BatteryManager.BATTERY_PLUGGED_AC
                || plugged == BatteryManager.BATTERY_PLUGGED_USB
                || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
    }

    fun isMediaPreviewEnabled(context: Context, preferences: KPreferences): Boolean {
        if (!preferences[mediaPreviewKey]) return false
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return !ConnectivityManagerCompat.isActiveNetworkMetered(cm) || !preferences[bandwidthSavingModeKey]
    }

    /**
     * Send Notifications to Pebble smartwatches

     * @param context Context
     * *
     * @param message String
     */
    fun sendPebbleNotification(context: Context, message: String) {
        sendPebbleNotification(context, null, message)
    }

    /**
     * Send Notifications to Pebble smartwatches

     * @param context Context
     * *
     * @param title   String
     * *
     * @param message String
     */
    fun sendPebbleNotification(context: Context, title: String?, message: String) {
        val appName: String

        if (title == null) {
            appName = context.getString(R.string.app_name)
        } else {
            appName = context.getString(R.string.app_name) + " - " + title
        }

        if (TextUtils.isEmpty(message)) return
        val prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

        if (prefs.getBoolean(KEY_PEBBLE_NOTIFICATIONS, false)) {

            val messages = ArrayList<PebbleMessage>()
            messages.add(PebbleMessage(appName, message))

            val intent = Intent(INTENT_ACTION_PEBBLE_NOTIFICATION)
            intent.putExtra("messageType", "PEBBLE_ALERT")
            intent.putExtra("sender", appName)
            intent.putExtra("notificationData", JsonSerializer.serialize(messages, PebbleMessage::class.java))

            context.applicationContext.sendBroadcast(intent)
        }

    }

    fun getCachedGeoLocation(context: Context): GeoLocation? {
        val location = getCachedLocation(context) ?: return null
        return GeoLocation(location.latitude, location.longitude)
    }

    fun getCachedLocation(context: Context): Location? {
        if (BuildConfig.DEBUG) {
            Log.v(LOGTAG, "Fetching cached location", Exception())
        }
        var location: Location? = null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager ?: return null
        try {
            location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (ignore: SecurityException) {

        }

        if (location != null) return location
        try {
            location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (ignore: SecurityException) {

        }

        return location
    }

    fun checkDeviceCompatible(): Boolean {
        try {
            Menu::class.java.isAssignableFrom(MenuBuilder::class.java)
        } catch (e: Error) {
            TwidereBugReporter.logException(e)
            return false
        }

        return true
    }
}
