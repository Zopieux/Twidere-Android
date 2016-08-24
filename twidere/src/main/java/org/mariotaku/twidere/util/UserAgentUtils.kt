/*
 * Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
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

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.support.annotation.UiThread
import android.support.annotation.WorkerThread
import android.webkit.WebSettings
import android.webkit.WebView

import java.lang.reflect.Constructor

/**
 * Created by mariotaku on 15/4/12.
 */
object UserAgentUtils {

    // You may uncomment next line if using Android Annotations library, otherwise just be sure to run it in on the UI thread
    @UiThread
    fun getDefaultUserAgentString(context: Context): String {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw IllegalStateException("User-Agent cannot be fetched from worker thread")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                return NewApiWrapper.getDefaultUserAgent(context)
            }
            val constructor = WebSettings::class.java.getDeclaredConstructor(Context::class.java, WebView::class.java)
            constructor.isAccessible = true
            try {
                val settings = constructor.newInstance(context, null)
                return settings.userAgentString
            } finally {
                constructor.isAccessible = false
            }
        } catch (e: Exception) {
            var webView: WebView? = null
            try {
                webView = WebView(context)
                return webView.settings.userAgentString
            } catch (e2: Exception) {
                return System.getProperty("http.agent")
            } finally {
                if (webView != null) {
                    webView.destroy()
                }
            }
        }

    }

    @WorkerThread
    fun getDefaultUserAgentStringSafe(context: Context): String? {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            //noinspection ResourceType
            return getDefaultUserAgentString(context)
        }
        val handler = Handler(Looper.getMainLooper())
        try {
            val runnable = FetchUserAgentRunnable(context)
            handler.post(runnable)
            runnable.waitForExecution()
            return runnable.userAgent
        } finally {
            handler.removeCallbacksAndMessages(null)
        }
    }

    private class FetchUserAgentRunnable(private val context: Context) : Runnable {
        var userAgent: String? = null
            private set
        private var userAgentSet: Boolean = false

        override fun run() {
            userAgent = getDefaultUserAgentString(context)
            userAgentSet = true
        }

        fun waitForExecution() {
            //noinspection StatementWithEmptyBody
            while (!userAgentSet)
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    internal object NewApiWrapper {

        @UiThread
        fun getDefaultUserAgent(context: Context): String {
            return WebSettings.getDefaultUserAgent(context)
        }
    }
}
