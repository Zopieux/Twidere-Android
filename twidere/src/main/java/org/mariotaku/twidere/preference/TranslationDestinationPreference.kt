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
import android.content.DialogInterface
import android.content.DialogInterface.OnCancelListener
import android.content.DialogInterface.OnClickListener
import android.os.AsyncTask
import android.support.v7.app.AlertDialog
import android.support.v7.preference.Preference
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView

import org.mariotaku.microblog.library.MicroBlog
import org.mariotaku.microblog.library.MicroBlogException
import org.mariotaku.microblog.library.twitter.model.Language
import org.mariotaku.microblog.library.twitter.model.ResponseList
import org.mariotaku.twidere.R
import org.mariotaku.twidere.util.MicroBlogAPIFactory

import java.text.Collator
import java.util.Comparator

import org.mariotaku.twidere.TwidereConstants.LOGTAG

class TranslationDestinationPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet = null, defStyle: Int = R.attr.preferenceStyle) : Preference(context, attrs, defStyle), OnClickListener {

    private var mSelectedLanguageCode = "en"

    private var mGetAvailableTrendsTask: GetLanguagesTask? = null

    private val mAdapter: LanguagesAdapter

    private var mDialog: AlertDialog? = null

    init {
        mAdapter = LanguagesAdapter(context)
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val item = mAdapter.getItem(which)
        if (item != null) {
            persistString(item.code)
        }
        if (mDialog != null && mDialog!!.isShowing) {
            mDialog!!.dismiss()
        }
    }

    override fun onClick() {
        if (mGetAvailableTrendsTask != null) {
            mGetAvailableTrendsTask!!.cancel(false)
        }
        mGetAvailableTrendsTask = GetLanguagesTask(context)
        mGetAvailableTrendsTask!!.execute()
    }

    private class LanguageComparator internal constructor(context: Context) : Comparator<Language> {
        private val mCollator: Collator

        init {
            mCollator = Collator.getInstance(context.resources.configuration.locale)
        }

        override fun compare(object1: Language, object2: Language): Int {
            return mCollator.compare(object1.name, object2.name)
        }

    }

    private class LanguagesAdapter(private val mContext: Context) : ArrayAdapter<Language>(mContext, android.R.layout.simple_list_item_single_choice) {

        fun findItemPosition(code: String): Int {
            if (TextUtils.isEmpty(code)) return -1
            val count = count
            for (i in 0..count - 1) {
                val item = getItem(i)
                if (code.equals(item.code, ignoreCase = true)) return i
            }
            return -1
        }

        override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            val text = (if (view is TextView) view else view.findViewById(android.R.id.text1)) as TextView
            val item = getItem(position)
            if (item != null && text != null) {
                text.setSingleLine()
                text.text = item.name
            }
            return view
        }

        fun setData(data: List<Language>?) {
            clear()
            if (data != null) {
                addAll(data)
            }
            sort(LanguageComparator(mContext))
        }

    }

    internal inner class GetLanguagesTask(context: Context) : AsyncTask<Any, Any, ResponseList<Language>>(), OnCancelListener {

        private val mProgress: ProgressDialog

        init {
            mProgress = ProgressDialog(context)
        }

        override fun onCancel(dialog: DialogInterface) {
            cancel(true)
        }

        override fun doInBackground(vararg args: Any): ResponseList<Language>? {
            val twitter = MicroBlogAPIFactory.getDefaultTwitterInstance(context, false) ?: return null
            try {
                mSelectedLanguageCode = twitter.accountSettings.language
                return twitter.languages
            } catch (e: MicroBlogException) {
                Log.w(LOGTAG, e)
            }

            return null
        }

        override fun onPostExecute(result: ResponseList<Language>?) {
            if (mProgress.isShowing) {
                mProgress.dismiss()
            }
            mAdapter.setData(result)
            if (result == null) return
            val selectorBuilder = AlertDialog.Builder(context)
            selectorBuilder.setTitle(title)
            val value = getPersistedString(mSelectedLanguageCode)
            selectorBuilder.setSingleChoiceItems(mAdapter, mAdapter.findItemPosition(value),
                    this@TranslationDestinationPreference)
            selectorBuilder.setNegativeButton(android.R.string.cancel, null)
            mDialog = selectorBuilder.create()
            val lv = mDialog!!.listView
            if (lv != null) {
                lv.isFastScrollEnabled = true
            }
            mDialog!!.show()
        }

        override fun onPreExecute() {
            if (mProgress.isShowing) {
                mProgress.dismiss()
            }
            mProgress.setMessage(context.getString(R.string.please_wait))
            mProgress.setOnCancelListener(this)
            mProgress.show()
        }

    }
}
