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

import android.app.ProgressDialog
import android.content.Context
import android.os.AsyncTask
import android.os.AsyncTask.Status
import android.support.v7.preference.Preference
import android.util.AttributeSet

import org.mariotaku.twidere.R

abstract class AsyncTaskPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet = null, defStyle: Int = R.attr.preferenceStyle) : Preference(context, attrs, defStyle) {

    private var mTask: Task? = null

    override fun onClick() {
        if (mTask == null || mTask!!.status != Status.RUNNING) {
            mTask = Task(this)
            mTask!!.execute()
        }
    }

    protected abstract fun doInBackground()

    private class Task(private val mPreference: AsyncTaskPreference) : AsyncTask<Any, Any, Any>() {
        private val mContext: Context
        private val mProgress: ProgressDialog

        init {
            mContext = mPreference.context
            mProgress = ProgressDialog(mContext)
        }

        override fun doInBackground(vararg args: Any): Any? {
            mPreference.doInBackground()
            return null
        }

        override fun onPostExecute(result: Any) {
            if (mProgress.isShowing) {
                mProgress.dismiss()
            }
        }

        override fun onPreExecute() {
            if (mProgress.isShowing) {
                mProgress.dismiss()
            }
            mProgress.setMessage(mContext.getString(R.string.please_wait))
            mProgress.setCancelable(false)
            mProgress.show()
        }

    }

}
