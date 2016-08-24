/*
 *                 Twidere - Twitter client for Android
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

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by mariotaku on 15/11/13.
 */
class NotificationManagerWrapper(context: Context) {
    private val notificationManager: NotificationManager
    private val notifications = CopyOnWriteArrayList<PostedNotification>()

    init {
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun notify(tag: String, id: Int, notification: Notification) {
        notificationManager.notify(tag, id, notification)
        notifications.add(PostedNotification(tag, id))
    }

    fun notify(id: Int, notification: Notification) {
        notificationManager.notify(id, notification)
        notifications.add(PostedNotification(null, id))
    }

    fun cancel(tag: String, id: Int) {
        notificationManager.cancel(tag, id)
        notifications.removeAll(find(tag, id))
    }

    private fun find(tag: String?, id: Int): List<PostedNotification> {
        val result = ArrayList<PostedNotification>()
        for (notification in notifications) {
            if (notification.equals(tag, id)) {
                result.add(notification)
            }
        }
        return result
    }

    private fun findByTag(tag: String?): List<PostedNotification> {
        val result = ArrayList<PostedNotification>()
        for (notification in notifications) {
            if (if (tag != null) tag == notification.tag else null == notification.tag) {
                result.add(notification)
            }
        }
        return result
    }

    private fun findById(id: Int): List<PostedNotification> {
        val result = ArrayList<PostedNotification>()
        for (notification in notifications) {
            if (id == notification.id) {
                result.add(notification)
            }
        }
        return result
    }

    fun cancel(id: Int) {
        notificationManager.cancel(id)
        notifications.removeAll(find(null, id))
    }

    fun cancelById(id: Int) {
        val collection = findById(id)
        for (notification in collection) {
            notificationManager.cancel(notification.tag, notification.id)
        }
        notificationManager.cancel(id)
        notifications.removeAll(collection)
    }

    fun cancelByTag(tag: String) {
        val collection = findByTag(tag)
        for (notification in collection) {
            notificationManager.cancel(notification.tag, notification.id)
        }
        notifications.removeAll(collection)
    }

    fun cancelAll() {
        notificationManager.cancelAll()
    }

    fun cancelByTag() {

    }

    internal class PostedNotification(internal val tag: String?, internal val id: Int) {

        fun equals(tag: String?, id: Int): Boolean {
            return id == this.id && if (tag != null) tag == this.tag else this.tag == null
        }

        override fun hashCode(): Int {
            var result = if (tag != null) tag.hashCode() else 0
            result = 31 * result + id
            return result
        }
    }
}
