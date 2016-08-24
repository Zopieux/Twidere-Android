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

package org.mariotaku.twidere.preference

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.support.v7.preference.Preference
import android.util.AttributeSet

import org.mariotaku.twidere.R
import org.mariotaku.twidere.activity.NyanActivity

class AppVersionPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet = null, defStyle: Int = R.attr.preferenceStyle) : Preference(context, attrs, defStyle) {

    var mHandler = Handler()
    protected var mClickCount: Int = 0

    private val mResetCounterRunnable = Runnable { mClickCount = 0 }

    init {
        val pm = context.packageManager
        try {
            val info = pm.getPackageInfo(context.packageName, 0)
            title = info.applicationInfo.loadLabel(pm)
            summary = info.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            throw AssertionError(e)
        }

    }

    override fun onClick() {
        mHandler.removeCallbacks(mResetCounterRunnable)
        mClickCount++
        if (mClickCount >= 7) {
            val context = context
            if (context != null) {
                mClickCount = 0
                context.startActivity(Intent(context, NyanActivity::class.java))
            }
        }
        mHandler.postDelayed(mResetCounterRunnable, 3000)
    }

}
