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

import android.content.*
import android.util.Log
import org.mariotaku.twidere.TwidereConstants.LOGTAG
import java.util.*

object ServiceUtils {

    private val sConnectionMap = HashMap<Context, ServiceUtils.ServiceBinder>()

    @JvmOverloads fun bindToService(context: Context, intent: Intent,
                                    callback: ServiceConnection? = null): ServiceToken? {

        val cw = ContextWrapper(context)
        val cn = cw.startService(intent)
        if (cn != null) {
            val sb = ServiceBinder(callback)
            if (cw.bindService(intent, sb, 0)) {
                sConnectionMap.put(cw, sb)
                return ServiceToken(cw)
            }
        }
        Log.e(LOGTAG, "Failed to bind to service")
        return null
    }

    fun unbindFromService(token: ServiceToken) {
        val serviceBinder = sConnectionMap[token.wrappedContext] ?: return
        token.wrappedContext.unbindService(serviceBinder)
    }

    class ServiceToken internal constructor(internal val wrappedContext: ContextWrapper)

    internal class ServiceBinder(private val mCallback: ServiceConnection?) : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: android.os.IBinder) {
            mCallback?.onServiceConnected(className, service)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            mCallback?.onServiceDisconnected(className)
        }
    }
}
