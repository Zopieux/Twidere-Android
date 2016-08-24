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

package org.mariotaku.twidere.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import edu.tsinghua.hotmobi.model.BatteryRecord
import edu.tsinghua.hotmobi.model.ScreenEvent
import org.apache.commons.lang3.math.NumberUtils
import org.mariotaku.twidere.BuildConfig
import org.mariotaku.twidere.Constants
import org.mariotaku.twidere.TwidereConstants.LOGTAG
import org.mariotaku.twidere.constant.IntentConstants.*
import org.mariotaku.twidere.constant.SharedPreferenceConstants.*
import org.mariotaku.twidere.model.AccountPreferences
import org.mariotaku.twidere.model.SimpleRefreshTaskParam
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.provider.TwidereDataStore.*
import org.mariotaku.twidere.receiver.PowerStateReceiver
import org.mariotaku.twidere.util.AsyncTwitterWrapper
import org.mariotaku.twidere.util.DataStoreUtils
import org.mariotaku.twidere.util.SharedPreferencesWrapper
import org.mariotaku.twidere.util.Utils
import org.mariotaku.twidere.util.dagger.GeneralComponentHelper
import java.util.*
import javax.inject.Inject

class RefreshService : Service(), Constants {

    @Inject
    lateinit var preferences: SharedPreferencesWrapper
    @Inject
    lateinit var twitterWrapper: AsyncTwitterWrapper

    private var alarmManager: AlarmManager? = null

    private var mPendingRefreshHomeTimelineIntent: PendingIntent? = null
    private var mPendingRefreshMentionsIntent: PendingIntent? = null
    private var mPendingRefreshDirectMessagesIntent: PendingIntent? = null
    private var mPendingRefreshTrendsIntent: PendingIntent? = null

    private val mStateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BuildConfig.DEBUG) {
                Log.d(LOGTAG, String.format("Refresh service received action %s", action))
            }
            when (action) {
                BROADCAST_RESCHEDULE_HOME_TIMELINE_REFRESHING -> {
                    rescheduleHomeTimelineRefreshing()
                }
                BROADCAST_RESCHEDULE_MENTIONS_REFRESHING -> {
                    rescheduleMentionsRefreshing()
                }
                BROADCAST_RESCHEDULE_DIRECT_MESSAGES_REFRESHING -> {
                    rescheduleDirectMessagesRefreshing()
                }
                BROADCAST_RESCHEDULE_TRENDS_REFRESHING -> {
                    rescheduleTrendsRefreshing()
                }
                BROADCAST_REFRESH_HOME_TIMELINE -> {
                    if (isAutoRefreshAllowed && !isHomeTimelineRefreshing) {
                        twitterWrapper.getHomeTimelineAsync(object : SimpleRefreshTaskParam() {

                            private val accountIds: Array<UserKey> by lazy {
                                val prefs = AccountPreferences.getAccountPreferences(context,
                                        DataStoreUtils.getAccountKeys(context))
                                return@lazy getRefreshableIds(prefs, HomeRefreshableFilter.INSTANCE)
                            }

                            override fun getAccountKeysWorker(): Array<UserKey> {
                                return accountIds
                            }


                            override fun getSinceIds(): Array<String>? {
                                return DataStoreUtils.getNewestStatusIds(context,
                                        Statuses.CONTENT_URI, accountKeys)
                            }
                        })
                    }
                }
                BROADCAST_REFRESH_NOTIFICATIONS -> {
                    if (isAutoRefreshAllowed) {
                        twitterWrapper.getActivitiesAboutMeAsync(object : SimpleRefreshTaskParam() {

                            private val accountIds: Array<UserKey> by lazy {
                                val prefs = AccountPreferences.getAccountPreferences(context,
                                        DataStoreUtils.getAccountKeys(context))
                                return@lazy getRefreshableIds(prefs, MentionsRefreshableFilter.INSTANCE)
                            }

                            override fun getAccountKeysWorker(): Array<UserKey> {
                                return accountIds
                            }

                            override fun getSinceIds(): Array<String>? {
                                return DataStoreUtils.getNewestActivityMaxPositions(context,
                                        Activities.AboutMe.CONTENT_URI, accountKeys)
                            }
                        })
                    }
                }
                BROADCAST_REFRESH_DIRECT_MESSAGES -> {
                    if (isAutoRefreshAllowed && !isReceivedDirectMessagesRefreshing) {
                        twitterWrapper.getReceivedDirectMessagesAsync(object : SimpleRefreshTaskParam() {

                            private val accountIds: Array<UserKey> by lazy {
                                val prefs = AccountPreferences.getAccountPreferences(context,
                                        DataStoreUtils.getAccountKeys(context))
                                return@lazy getRefreshableIds(prefs, MessagesRefreshableFilter.INSTANCE)
                            }

                            override fun getAccountKeysWorker(): Array<UserKey> {
                                return accountIds
                            }

                            override fun getSinceIds(): Array<String>? {
                                return DataStoreUtils.getNewestMessageIds(context,
                                        DirectMessages.Inbox.CONTENT_URI, accountKeys)
                            }
                        })
                    }
                }
                BROADCAST_REFRESH_TRENDS -> {
                    if (isAutoRefreshAllowed) {
                        val prefs = AccountPreferences.getAccountPreferences(context,
                                DataStoreUtils.getAccountKeys(context))
                        val refreshIds = getRefreshableIds(prefs, TrendsRefreshableFilter.INSTANCE)
                        if (BuildConfig.DEBUG) {
                            Log.d(LOGTAG, String.format("Auto refreshing trends for %s", Arrays.toString(refreshIds)))
                        }
                        getLocalTrends(refreshIds)
                    }
                }
            }
        }

    }

    private val mPowerStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    BatteryRecord.log(context, intent)
                }
                else -> {
                    BatteryRecord.log(context)
                }
            }
        }
    }

    private val mScreenStateReceiver = object : BroadcastReceiver() {
        var mPresentTime: Long = -1

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    ScreenEvent.log(context, ScreenEvent.Action.ON, presentDuration)
                }
                Intent.ACTION_SCREEN_OFF -> {
                    ScreenEvent.log(context, ScreenEvent.Action.OFF, presentDuration)
                    mPresentTime = -1
                }
                Intent.ACTION_USER_PRESENT -> {
                    mPresentTime = SystemClock.elapsedRealtime()
                    ScreenEvent.log(context, ScreenEvent.Action.PRESENT, -1)
                }
            }
        }

        private val presentDuration: Long
            get() {
                if (mPresentTime < 0) return -1
                return SystemClock.elapsedRealtime() - mPresentTime
            }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        GeneralComponentHelper.build(this).inject(this)
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        mPendingRefreshHomeTimelineIntent = PendingIntent.getBroadcast(this, 0, Intent(
                BROADCAST_REFRESH_HOME_TIMELINE), 0)
        mPendingRefreshMentionsIntent = PendingIntent.getBroadcast(this, 0, Intent(BROADCAST_REFRESH_NOTIFICATIONS), 0)
        mPendingRefreshDirectMessagesIntent = PendingIntent.getBroadcast(this, 0, Intent(
                BROADCAST_REFRESH_DIRECT_MESSAGES), 0)
        mPendingRefreshTrendsIntent = PendingIntent.getBroadcast(this, 0, Intent(BROADCAST_REFRESH_TRENDS), 0)
        val refreshFilter = IntentFilter(BROADCAST_NOTIFICATION_DELETED)
        refreshFilter.addAction(BROADCAST_REFRESH_HOME_TIMELINE)
        refreshFilter.addAction(BROADCAST_REFRESH_NOTIFICATIONS)
        refreshFilter.addAction(BROADCAST_REFRESH_DIRECT_MESSAGES)
        refreshFilter.addAction(BROADCAST_RESCHEDULE_HOME_TIMELINE_REFRESHING)
        refreshFilter.addAction(BROADCAST_RESCHEDULE_MENTIONS_REFRESHING)
        refreshFilter.addAction(BROADCAST_RESCHEDULE_DIRECT_MESSAGES_REFRESHING)
        registerReceiver(mStateReceiver, refreshFilter)
        val batteryFilter = IntentFilter()
        batteryFilter.addAction(Intent.ACTION_BATTERY_CHANGED)
        batteryFilter.addAction(Intent.ACTION_BATTERY_OKAY)
        batteryFilter.addAction(Intent.ACTION_BATTERY_LOW)
        batteryFilter.addAction(Intent.ACTION_POWER_CONNECTED)
        batteryFilter.addAction(Intent.ACTION_POWER_DISCONNECTED)
        val screenFilter = IntentFilter()
        screenFilter.addAction(Intent.ACTION_SCREEN_ON)
        screenFilter.addAction(Intent.ACTION_SCREEN_OFF)
        screenFilter.addAction(Intent.ACTION_USER_PRESENT)
        registerReceiver(mPowerStateReceiver, batteryFilter)
        registerReceiver(mScreenStateReceiver, screenFilter)
        PowerStateReceiver.setServiceReceiverStarted(true)
        if (Utils.hasAutoRefreshAccounts(this)) {
            startAutoRefresh()
        } else {
            stopSelf()
        }
    }

    override fun onDestroy() {
        PowerStateReceiver.setServiceReceiverStarted(false)
        unregisterReceiver(mScreenStateReceiver)
        unregisterReceiver(mPowerStateReceiver)
        unregisterReceiver(mStateReceiver)
        if (Utils.hasAutoRefreshAccounts(this)) {
            // Auto refresh enabled, so I will try to start service after it was
            // stopped.
            startService(Intent(this, javaClass))
        }
        super.onDestroy()
    }

    protected val isAutoRefreshAllowed: Boolean
        get() = Utils.isNetworkAvailable(this) && (Utils.isBatteryOkay(this) || !Utils.shouldStopAutoRefreshOnBatteryLow(this))

    private fun getLocalTrends(accountIds: Array<UserKey>) {
        val account_id = Utils.getDefaultAccountKey(this)
        val woeid = preferences.getInt(KEY_LOCAL_TRENDS_WOEID, 1)
        twitterWrapper.getLocalTrendsAsync(account_id!!, woeid)
    }

    private fun getRefreshableIds(prefs: Array<AccountPreferences>, filter: RefreshableAccountFilter): Array<UserKey> {
        return prefs.filter {
            it.isAutoRefreshEnabled && filter.isRefreshable(it)
        }.map {
            it.accountKey
        }.toTypedArray()
    }

    private val refreshInterval: Long
        get() {
            val prefValue = NumberUtils.toInt(preferences.getString(KEY_REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL), -1)
            return Math.max(prefValue, 3) * 60 * 1000.toLong()
        }

    private val isHomeTimelineRefreshing: Boolean
        get() = twitterWrapper.isHomeTimelineRefreshing

    private val isReceivedDirectMessagesRefreshing: Boolean
        get() = twitterWrapper.isReceivedDirectMessagesRefreshing

    private fun rescheduleDirectMessagesRefreshing() {
        alarmManager!!.cancel(mPendingRefreshDirectMessagesIntent)
        val refreshInterval = refreshInterval
        if (refreshInterval > 0) {
            alarmManager!!.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + refreshInterval,
                    refreshInterval, mPendingRefreshDirectMessagesIntent)
        }
    }

    private fun rescheduleHomeTimelineRefreshing() {
        alarmManager!!.cancel(mPendingRefreshHomeTimelineIntent)
        val refreshInterval = refreshInterval
        if (refreshInterval > 0) {
            alarmManager!!.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + refreshInterval,
                    refreshInterval, mPendingRefreshHomeTimelineIntent)
        }
    }

    private fun rescheduleMentionsRefreshing() {
        alarmManager!!.cancel(mPendingRefreshMentionsIntent)
        val refreshInterval = refreshInterval
        if (refreshInterval > 0) {
            alarmManager!!.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + refreshInterval,
                    refreshInterval, mPendingRefreshMentionsIntent)
        }
    }

    private fun rescheduleTrendsRefreshing() {
        alarmManager!!.cancel(mPendingRefreshTrendsIntent)
        val refreshInterval = refreshInterval
        if (refreshInterval > 0) {
            alarmManager!!.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + refreshInterval,
                    refreshInterval, mPendingRefreshTrendsIntent)
        }
    }

    private fun startAutoRefresh(): Boolean {
        stopAutoRefresh()
        val refreshInterval = refreshInterval
        if (refreshInterval <= 0) return false
        rescheduleHomeTimelineRefreshing()
        rescheduleMentionsRefreshing()
        rescheduleDirectMessagesRefreshing()
        rescheduleTrendsRefreshing()
        return true
    }

    private fun stopAutoRefresh() {
        alarmManager!!.cancel(mPendingRefreshHomeTimelineIntent)
        alarmManager!!.cancel(mPendingRefreshMentionsIntent)
        alarmManager!!.cancel(mPendingRefreshDirectMessagesIntent)
        alarmManager!!.cancel(mPendingRefreshTrendsIntent)
    }

    private interface RefreshableAccountFilter {
        fun isRefreshable(pref: AccountPreferences): Boolean
    }

    private class HomeRefreshableFilter : RefreshableAccountFilter {

        override fun isRefreshable(pref: AccountPreferences): Boolean {
            return pref.isAutoRefreshHomeTimelineEnabled
        }

        companion object {
            val INSTANCE: RefreshableAccountFilter = HomeRefreshableFilter()
        }
    }

    private class MentionsRefreshableFilter : RefreshableAccountFilter {

        override fun isRefreshable(pref: AccountPreferences): Boolean {
            return pref.isAutoRefreshMentionsEnabled
        }

        companion object {

            internal val INSTANCE: RefreshableAccountFilter = MentionsRefreshableFilter()
        }

    }

    private class MessagesRefreshableFilter : RefreshableAccountFilter {

        override fun isRefreshable(pref: AccountPreferences): Boolean {
            return pref.isAutoRefreshDirectMessagesEnabled
        }

        companion object {
            val INSTANCE: RefreshableAccountFilter = MentionsRefreshableFilter()
        }
    }

    private class TrendsRefreshableFilter : RefreshableAccountFilter {

        override fun isRefreshable(pref: AccountPreferences): Boolean {
            return pref.isAutoRefreshTrendsEnabled
        }

        companion object {
            val INSTANCE: RefreshableAccountFilter = TrendsRefreshableFilter()
        }
    }
}
