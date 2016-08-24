package org.mariotaku.twidere.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.text.TextUtils
import android.text.TextUtils.isEmpty
import org.mariotaku.twidere.BuildConfig
import org.mariotaku.twidere.R
import org.mariotaku.twidere.TwidereConstants.*
import org.mariotaku.twidere.activity.MediaViewerActivity
import org.mariotaku.twidere.annotation.Referral
import org.mariotaku.twidere.constant.SharedPreferenceConstants
import org.mariotaku.twidere.fragment.SensitiveContentWarningDialogFragment
import org.mariotaku.twidere.model.*
import org.mariotaku.twidere.model.util.ParcelableLocationUtils
import org.mariotaku.twidere.model.util.ParcelableMediaUtils
import java.util.*

/**
 * Created by mariotaku on 16/1/2.
 */
object IntentUtils {

    fun getStatusShareText(context: Context, status: ParcelableStatus): String {
        val link = LinkCreator.getStatusWebLink(status)
        return context.getString(R.string.status_share_text_format_with_link,
                status.text_plain, link.toString())
    }

    fun getStatusShareSubject(context: Context, status: ParcelableStatus): String {
        val timeString = Utils.formatToLongTimeString(context, status.timestamp)
        return context.getString(R.string.status_share_subject_format_with_time,
                status.user_name, status.user_screen_name, timeString)
    }

    fun openUserProfile(context: Context, user: ParcelableUser,
                        activityOptions: Bundle?, newDocument: Boolean,
                        @Referral referral: String?) {
        val extras = Bundle()
        extras.putParcelable(EXTRA_USER, user)
        if (user.extras != null) {
            extras.putString(EXTRA_PROFILE_URL, user.extras.statusnet_profile_url)
        }
        val uri = LinkCreator.getTwidereUserLink(user.account_key, user.key, user.screen_name)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setExtrasClassLoader(context.classLoader)
        intent.putExtras(extras)
        intent.putExtra(EXTRA_REFERRAL, referral)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && newDocument) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        }
        if (context is Activity) {
            ActivityCompat.startActivity(context, intent, activityOptions)
        } else {
            context.startActivity(intent)
        }
    }

    fun openUserProfile(context: Context, accountKey: UserKey?,
                        userKey: UserKey?, screenName: String?,
                        activityOptions: Bundle?, newDocument: Boolean,
                        @Referral referral: String?) {
        val intent = userProfile(accountKey, userKey, screenName, referral, null) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && newDocument) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        }
        if (context is Activity) {
            ActivityCompat.startActivity(context, intent, activityOptions)
        } else {
            context.startActivity(intent)
        }
    }

    fun userProfile(accountKey: UserKey?, userKey: UserKey?, screenName: String?,
                    @Referral referral: String?, profileUrl: String?): Intent? {
        if (userKey == null && isEmpty(screenName)) return null
        val uri = LinkCreator.getTwidereUserLink(accountKey, userKey, screenName)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.putExtra(EXTRA_REFERRAL, referral)
        intent.putExtra(EXTRA_PROFILE_URL, profileUrl)
        return intent
    }

    fun openItems(context: Context, items: List<Parcelable>?) {
        if (items == null) return
        val extras = Bundle()
        extras.putParcelableArrayList(EXTRA_ITEMS, ArrayList(items))
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_ITEMS)
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        intent.putExtras(extras)
        context.startActivity(intent)
    }

    fun openUserMentions(context: Context, accountKey: UserKey?,
                         screenName: String) {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_USER_MENTIONS)
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        }
        builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName)
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        context.startActivity(intent)
    }

    fun openMedia(context: Context, message: ParcelableDirectMessage,
                  current: ParcelableMedia?, options: Bundle?,
                  newDocument: Boolean) {
        openMedia(context, message.account_key, false, null, message, current, message.media,
                options, newDocument)
    }

    fun openMedia(context: Context, status: ParcelableStatus,
                  current: ParcelableMedia?, options: Bundle?,
                  newDocument: Boolean) {
        val media = ParcelableMediaUtils.getPrimaryMedia(status) ?: return
        openMedia(context, status.account_key, status.is_possibly_sensitive, status, null, current,
                media, options, newDocument)
    }

    fun openMedia(context: Context, accountKey: UserKey?, isPossiblySensitive: Boolean,
                  current: ParcelableMedia?, media: Array<ParcelableMedia>,
                  options: Bundle?, newDocument: Boolean) {
        openMedia(context, accountKey, isPossiblySensitive, null, null, current, media, options, newDocument)
    }

    fun openMedia(context: Context, accountKey: UserKey?, isPossiblySensitive: Boolean,
                  status: ParcelableStatus?, message: ParcelableDirectMessage?,
                  current: ParcelableMedia?, media: Array<ParcelableMedia>,
                  options: Bundle?, newDocument: Boolean) {
        val prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        if (context is FragmentActivity && isPossiblySensitive
                && !prefs.getBoolean(SharedPreferenceConstants.KEY_DISPLAY_SENSITIVE_CONTENTS, false)) {
            val fm = context.supportFragmentManager
            val fragment = SensitiveContentWarningDialogFragment()
            val args = Bundle()
            args.putParcelable(EXTRA_ACCOUNT_KEY, accountKey)
            args.putParcelable(EXTRA_CURRENT_MEDIA, current)
            if (status != null) {
                args.putParcelable(EXTRA_STATUS, status)
            }
            if (message != null) {
                args.putParcelable(EXTRA_MESSAGE, message)
            }
            args.putParcelableArray(EXTRA_MEDIA, media)
            args.putBundle(EXTRA_ACTIVITY_OPTIONS, options)
            args.putBundle(EXTRA_ACTIVITY_OPTIONS, options)
            args.putBoolean(EXTRA_NEW_DOCUMENT, newDocument)
            fragment.arguments = args
            fragment.show(fm, "sensitive_content_warning")
        } else {
            openMediaDirectly(context, accountKey, status, message, current, media, options,
                    newDocument)
        }
    }

    fun openMediaDirectly(context: Context, accountKey: UserKey?, status: ParcelableStatus,
                          current: ParcelableMedia?, options: Bundle?, newDocument: Boolean) {
        val media = ParcelableMediaUtils.getPrimaryMedia(status) ?: return
        openMediaDirectly(context, accountKey, status, null, current, media,
                options, newDocument)
    }

    fun getDefaultBrowserPackage(context: Context, uri: Uri, checkHandled: Boolean): String? {
        if (checkHandled && !isWebLinkHandled(context, uri)) {
            return null
        }
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        val testBuilder = Uri.Builder()
        testBuilder.scheme(SCHEME_HTTP)
        val sb = StringBuilder()
        val random = Random()
        val range = 'z' - 'a'
        for (i in 0..19) {
            sb.append(('a' + Math.abs(random.nextInt()) % range).toChar())
        }
        sb.append(".com")
        testBuilder.authority(sb.toString())
        intent.data = testBuilder.build()

        val componentName = intent.resolveActivity(context.packageManager)
        if (componentName == null || componentName.className == null) return null
        if (TextUtils.equals("android", componentName.packageName)) return null
        return componentName.packageName
    }

    fun isWebLinkHandled(context: Context, uri: Uri): Boolean {
        val filter = getWebLinkIntentFilter(context) ?: return false
        return filter.match(Intent.ACTION_VIEW, null, uri.scheme, uri,
                setOf(Intent.CATEGORY_BROWSABLE), LOGTAG) >= 0
    }

    fun getWebLinkIntentFilter(context: Context): IntentFilter? {
        val testIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/user_name"))
        testIntent.addCategory(Intent.CATEGORY_BROWSABLE)
        testIntent.`package` = context.packageName
        val resolveInfo = context.packageManager.resolveActivity(testIntent,
                PackageManager.GET_RESOLVED_FILTER)
        return resolveInfo?.filter
    }

    fun openMediaDirectly(context: Context,
                          accountKey: UserKey?,
                          message: ParcelableDirectMessage, current: ParcelableMedia,
                          media: Array<ParcelableMedia>, options: Bundle,
                          newDocument: Boolean) {
        openMediaDirectly(context, accountKey, null, message, current, media, options, newDocument)
    }

    fun openMediaDirectly(context: Context, accountKey: UserKey?,
                          status: ParcelableStatus?, message: ParcelableDirectMessage?,
                          current: ParcelableMedia?, media: Array<ParcelableMedia>,
                          options: Bundle?, newDocument: Boolean) {
        if (media == null) return
        val intent = Intent(context, MediaViewerActivity::class.java)
        intent.putExtra(EXTRA_ACCOUNT_KEY, accountKey)
        intent.putExtra(EXTRA_CURRENT_MEDIA, current)
        intent.putExtra(EXTRA_MEDIA, media)
        if (status != null) {
            intent.putExtra(EXTRA_STATUS, status)
            intent.data = getMediaViewerUri("status", status.id, accountKey)
        }
        if (message != null) {
            intent.putExtra(EXTRA_MESSAGE, message)
            intent.data = getMediaViewerUri("message", message.id.toString(), accountKey)
        }
        if (newDocument && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        }
        if (context is Activity) {
            ActivityCompat.startActivity(context, intent, options)
        } else {
            context.startActivity(intent)
        }
    }

    fun getMediaViewerUri(type: String, id: String,
                          accountKey: UserKey?): Uri {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority("media")
        builder.appendPath(type)
        builder.appendPath(id)
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        }
        return builder.build()
    }

    fun openMessageConversation(context: Context,
                                accountKey: UserKey?,
                                recipientId: String?) {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_DIRECT_MESSAGES_CONVERSATION)
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
            if (recipientId != null) {
                builder.appendQueryParameter(QUERY_PARAM_RECIPIENT_ID, recipientId)
            }
        }
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        intent.`package` = BuildConfig.APPLICATION_ID
        context.startActivity(intent)
    }

    fun openIncomingFriendships(context: Context,
                                accountKey: UserKey?) {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_INCOMING_FRIENDSHIPS)
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        }
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        intent.`package` = BuildConfig.APPLICATION_ID
        context.startActivity(intent)
    }

    fun openMap(context: Context, latitude: Double, longitude: Double) {
        if (!ParcelableLocationUtils.isValidLocation(latitude, longitude)) return
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_MAP)
        builder.appendQueryParameter(QUERY_PARAM_LAT, latitude.toString())
        builder.appendQueryParameter(QUERY_PARAM_LNG, longitude.toString())
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        intent.`package` = BuildConfig.APPLICATION_ID
        context.startActivity(intent)
    }

    fun openMutesUsers(context: Context,
                       accountKey: UserKey?) {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_MUTES_USERS)
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        }
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        intent.`package` = BuildConfig.APPLICATION_ID
        context.startActivity(intent)
    }

    fun openScheduledStatuses(context: Context,
                              accountKey: UserKey?) {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_SCHEDULED_STATUSES)
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        }
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        intent.`package` = BuildConfig.APPLICATION_ID
        context.startActivity(intent)
    }

    fun openSavedSearches(context: Context, accountKey: UserKey?) {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_SAVED_SEARCHES)
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        }
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        intent.`package` = BuildConfig.APPLICATION_ID
        context.startActivity(intent)
    }

    @JvmOverloads fun openSearch(context: Context, accountKey: UserKey?, query: String, type: String? = null) {
        val intent = Intent(Intent.ACTION_VIEW)
        // Some devices cannot process query parameter with hashes well, so add this intent extra
        intent.putExtra(EXTRA_QUERY, query)
        if (accountKey != null) {
            intent.putExtra(EXTRA_ACCOUNT_KEY, accountKey)
        }

        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_SEARCH)
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        }
        builder.appendQueryParameter(QUERY_PARAM_QUERY, query)
        if (!TextUtils.isEmpty(type)) {
            builder.appendQueryParameter(QUERY_PARAM_TYPE, type)
            intent.putExtra(EXTRA_TYPE, type)
        }
        intent.data = builder.build()

        context.startActivity(intent)
    }

    fun openStatus(context: Context, accountKey: UserKey?,
                   statusId: String) {
        val uri = LinkCreator.getTwidereStatusLink(accountKey, statusId)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }

    fun openStatus(context: Context, status: ParcelableStatus, activityOptions: Bundle?) {
        val extras = Bundle()
        extras.putParcelable(EXTRA_STATUS, status)
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_STATUS)
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, status.account_key.toString())
        builder.appendQueryParameter(QUERY_PARAM_STATUS_ID, status.id)
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        intent.setExtrasClassLoader(context.classLoader)
        intent.putExtras(extras)
        if (context is Activity) {
            ActivityCompat.startActivity(context, intent, activityOptions)
        } else {
            context.startActivity(intent)
        }
    }

    fun openStatusFavoriters(context: Context, accountKey: UserKey?,
                             statusId: String) {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_STATUS_FAVORITERS)
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        }
        builder.appendQueryParameter(QUERY_PARAM_STATUS_ID, statusId)
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        context.startActivity(intent)
    }

    fun openStatusRetweeters(context: Context, accountKey: UserKey?,
                             statusId: String) {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_STATUS_RETWEETERS)
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        }
        builder.appendQueryParameter(QUERY_PARAM_STATUS_ID, statusId)
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        context.startActivity(intent)
    }

    fun openTweetSearch(context: Context, accountKey: UserKey?,
                        query: String) {
        openSearch(context, accountKey, query, QUERY_PARAM_VALUE_TWEETS)
    }

    fun openUserBlocks(activity: Activity?, accountKey: UserKey) {
        if (activity == null) return
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_USER_BLOCKS)
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        activity.startActivity(intent)
    }

    fun openUserFavorites(context: Context,
                          accountKey: UserKey?,
                          userKey: UserKey?,
                          screenName: String?) {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_USER_FAVORITES)
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        }
        if (userKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_USER_KEY, userKey.toString())
        }
        if (screenName != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName)
        }
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        context.startActivity(intent)

    }

    fun openUserFollowers(context: Context,
                          accountKey: UserKey?,
                          userKey: UserKey?,
                          screenName: String?) {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_USER_FOLLOWERS)
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        }
        if (userKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_USER_KEY, userKey.toString())
        }
        if (screenName != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName)
        }
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        context.startActivity(intent)
    }

    fun openUserFriends(context: Context,
                        accountKey: UserKey?,
                        userKey: UserKey?,
                        screenName: String?) {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_USER_FRIENDS)
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        }
        if (userKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_USER_KEY, userKey.toString())
        }
        if (screenName != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName)
        }
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        context.startActivity(intent)

    }

    fun openUserListDetails(context: Context,
                            accountKey: UserKey?,
                            listId: String?,
                            userId: UserKey?,
                            screenName: String?, listName: String?) {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_USER_LIST)
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        }
        if (listId != null) {
            builder.appendQueryParameter(QUERY_PARAM_LIST_ID, listId)
        }
        if (userId != null) {
            builder.appendQueryParameter(QUERY_PARAM_USER_KEY, userId.toString())
        }
        if (screenName != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName)
        }
        if (listName != null) {
            builder.appendQueryParameter(QUERY_PARAM_LIST_NAME, listName)
        }
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        context.startActivity(intent)
    }

    fun openUserListDetails(context: Context,
                            userList: ParcelableUserList) {
        val userKey = userList.user_key
        val listId = userList.id
        val extras = Bundle()
        extras.putParcelable(EXTRA_USER_LIST, userList)
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_USER_LIST)
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, userList.account_key.toString())
        builder.appendQueryParameter(QUERY_PARAM_USER_KEY, userKey.toString())
        builder.appendQueryParameter(QUERY_PARAM_LIST_ID, listId)
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        intent.setExtrasClassLoader(context.classLoader)
        intent.putExtras(extras)
        context.startActivity(intent)
    }

    fun openGroupDetails(context: Context, group: ParcelableGroup) {
        val extras = Bundle()
        extras.putParcelable(EXTRA_GROUP, group)
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_GROUP)
        builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, group.account_key.toString())
        builder.appendQueryParameter(QUERY_PARAM_GROUP_ID, group.id)
        builder.appendQueryParameter(QUERY_PARAM_GROUP_NAME, group.nickname)
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        intent.setExtrasClassLoader(context.classLoader)
        intent.putExtras(extras)
        context.startActivity(intent)
    }

    fun openUserLists(context: Context,
                      accountKey: UserKey?,
                      userKey: UserKey?,
                      screenName: String?) {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_USER_LISTS)
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        }
        if (userKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_USER_KEY, userKey.toString())
        }
        if (screenName != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName)
        }
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        context.startActivity(intent)
    }


    fun openUserGroups(context: Context,
                       accountKey: UserKey?,
                       userId: UserKey?,
                       screenName: String?) {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_USER_GROUPS)
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        }
        if (userId != null) {
            builder.appendQueryParameter(QUERY_PARAM_USER_KEY, userId.toString())
        }
        if (screenName != null) {
            builder.appendQueryParameter(QUERY_PARAM_SCREEN_NAME, screenName)
        }
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        context.startActivity(intent)
    }

    fun openDirectMessages(context: Context, accountKey: UserKey?) {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_DIRECT_MESSAGES)
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        }
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        context.startActivity(intent)
    }

    fun openInteractions(context: Context, accountKey: UserKey?) {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_INTERACTIONS)
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        }
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        context.startActivity(intent)
    }

    fun openPublicTimeline(context: Context, accountKey: UserKey?) {
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_PUBLIC_TIMELINE)
        if (accountKey != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
        }
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        context.startActivity(intent)
    }

    fun openAccountsManager(context: Context) {
        val intent = Intent()
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_ACCOUNTS)
        intent.data = builder.build()
        intent.`package` = BuildConfig.APPLICATION_ID
        context.startActivity(intent)
    }

    fun openDrafts(context: Context) {
        val intent = Intent()
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_DRAFTS)
        intent.data = builder.build()
        intent.`package` = BuildConfig.APPLICATION_ID
        context.startActivity(intent)
    }

    fun openProfileEditor(context: Context, accountId: UserKey?) {
        val intent = Intent()
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_PROFILE_EDITOR)
        if (accountId != null) {
            builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountId.toString())
        }
        intent.data = builder.build()
        intent.`package` = BuildConfig.APPLICATION_ID
        context.startActivity(intent)
    }

    fun openFilters(context: Context) {
        val intent = Intent()
        val builder = Uri.Builder()
        builder.scheme(SCHEME_TWIDERE)
        builder.authority(AUTHORITY_FILTERS)
        intent.data = builder.build()
        intent.`package` = BuildConfig.APPLICATION_ID
        context.startActivity(intent)
    }

    fun applyNewDocument(intent: Intent, enable: Boolean) {
        if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        }
    }
}
