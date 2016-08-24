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

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnCancelListener
import android.os.AsyncTask
import android.support.v4.util.LongSparseArray
import android.support.v4.util.SimpleArrayMap
import android.support.v7.app.AlertDialog
import android.support.v7.preference.Preference
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.TextView

import org.mariotaku.microblog.library.MicroBlog
import org.mariotaku.microblog.library.MicroBlogException
import org.mariotaku.microblog.library.twitter.model.Location
import org.mariotaku.twidere.BuildConfig
import org.mariotaku.twidere.R
import org.mariotaku.twidere.util.MicroBlogAPIFactory

import java.text.Collator
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.Locale

import org.mariotaku.twidere.TwidereConstants.LOGTAG

class TrendsLocationPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet = null, defStyle: Int = R.attr.preferenceStyle) : Preference(context, attrs, defStyle) {
    private val mAdapter: ExpandableTrendLocationsListAdapter
    private var mGetAvailableTrendsTask: GetAvailableTrendsTask? = null
    private var mDialog: AlertDialog? = null

    init {
        mAdapter = ExpandableTrendLocationsListAdapter(context)
    }

    override fun onClick() {
        if (mGetAvailableTrendsTask != null) {
            mGetAvailableTrendsTask!!.cancel(false)
        }
        mGetAvailableTrendsTask = GetAvailableTrendsTask(context)
        mGetAvailableTrendsTask!!.execute()
    }

    internal class ExpandableTrendLocationsListAdapter(context: Context) : BaseExpandableListAdapter() {

        private val mInflater: LayoutInflater
        var mData: SimpleArrayMap<Location, List<Location>>? = null

        init {
            mInflater = LayoutInflater.from(context)
        }

        override fun getGroupCount(): Int {
            if (mData == null) return 0
            return mData!!.size()
        }

        override fun getChildrenCount(groupPosition: Int): Int {
            if (mData == null) return 0
            return mData!!.valueAt(groupPosition).size
        }

        override fun getGroup(groupPosition: Int): Location {
            assert(mData != null)
            return mData!!.keyAt(groupPosition)
        }

        override fun getChild(groupPosition: Int, childPosition: Int): Location {
            assert(mData != null)
            return mData!!.valueAt(groupPosition)[childPosition]
        }

        override fun getGroupId(groupPosition: Int): Long {
            return getGroup(groupPosition).woeid.toLong()
        }

        override fun getChildId(groupPosition: Int, childPosition: Int): Long {
            return getChild(groupPosition, childPosition).woeid.toLong()
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup): View {
            val view: View
            if (convertView != null) {
                view = convertView
            } else {
                view = mInflater.inflate(android.R.layout.simple_expandable_list_item_1, parent, false)
            }
            (view.findViewById(android.R.id.text1) as TextView).text = getGroup(groupPosition).name
            return view
        }

        override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup): View {
            val view: View
            if (convertView != null) {
                view = convertView
            } else {
                view = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false)
            }
            val location = getChild(groupPosition, childPosition)
            val text1 = view.findViewById(android.R.id.text1) as TextView
            if (location.parentId == 1) {
                text1.setText(R.string.location_countrywide)
            } else {
                text1.text = location.name
            }
            return view
        }

        override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
            return true
        }

        fun setData(data: SimpleArrayMap<Location, List<Location>>?) {
            mData = data
            notifyDataSetChanged()
        }
    }

    internal class LocationsMap(locale: Locale) {

        val map = LongSparseArray<List<Location>>()
        val parents = LongSparseArray<Location>()
        private val comparator: LocationComparator

        init {
            comparator = LocationComparator(Collator.getInstance(locale))
        }

        fun put(location: Location) {
            val parentId = location.parentId
            if (parentId == EMPTY || parentId == WORLDWIDE) {
                putParent(location)
            } else {
                putChild(parentId, location)
            }
        }

        fun putParent(location: Location) {
            val woeid = location.woeid.toLong()
            parents.put(woeid, location)
            val list = getList(woeid)
            // Don't add child for 'worldwide'
            if (woeid != WORLDWIDE) {
                addToList(list, location)
            }
        }

        fun putChild(parentId: Long, location: Location) {
            addToList(getList(parentId), location)
        }

        fun getList(parentId: Long): MutableList<Location> {
            var list: List<Location>? = map.get(parentId)
            if (list == null) {
                list = ArrayList<Location>()
                map.put(parentId, list)
            }
            return list
        }

        fun addToList(list: MutableList<Location>, location: Location) {
            val loc = Collections.binarySearch(list, location, comparator)
            if (loc < 0) {
                list.add(-(loc + 1), location)
            }
        }

        fun pack(): SimpleArrayMap<Location, List<Location>> {
            val result = SimpleArrayMap<Location, List<Location>>(map.size())
            var i = 0
            val j = map.size()
            while (i < j) {
                val parent = parents.get(map.keyAt(i))
                if (parent == null) {
                    i++
                    continue
                }
                result.put(parent, map.valueAt(i))
                i++
            }
            return result
        }
    }

    private class LocationComparator(private val collator: Collator) : Comparator<Location> {

        private fun isCountryOrWorldwide(location: Location): Boolean {
            val parentId = location.parentId
            return parentId == 0 || parentId == 1
        }

        override fun compare(lhs: Location, rhs: Location): Int {
            if (isCountryOrWorldwide(lhs)) return Integer.MIN_VALUE
            if (isCountryOrWorldwide(rhs)) return Integer.MAX_VALUE
            return collator.compare(lhs.name, rhs.name)
        }
    }

    internal inner class GetAvailableTrendsTask(context: Context) : AsyncTask<Any, Any, SimpleArrayMap<Location, List<Location>>>(), OnCancelListener {
        private val mProgress: ProgressDialog

        init {
            mProgress = ProgressDialog(context)
        }

        override fun onCancel(dialog: DialogInterface) {
            cancel(true)
        }

        override fun doInBackground(vararg args: Any): SimpleArrayMap<Location, List<Location>>? {
            val twitter = MicroBlogAPIFactory.getDefaultTwitterInstance(context, false) ?: return null
            try {
                val map = LocationsMap(Locale.getDefault())
                for (location in twitter.availableTrends) {
                    map.put(location)
                }
                return map.pack()
            } catch (e: MicroBlogException) {
                if (BuildConfig.DEBUG) {
                    Log.w(LOGTAG, e)
                }
            }

            return null
        }

        override fun onPostExecute(result: SimpleArrayMap<Location, List<Location>>?) {
            if (mProgress.isShowing) {
                mProgress.dismiss()
            }
            mAdapter.setData(result)
            if (result == null) return
            val selectorBuilder = AlertDialog.Builder(context)
            selectorBuilder.setTitle(title)
            selectorBuilder.setView(R.layout.dialog_trends_location_selector)
            selectorBuilder.setNegativeButton(android.R.string.cancel, null)
            mDialog = selectorBuilder.create()
            mDialog!!.setOnShowListener { dialogInterface ->
                val dialog = dialogInterface as Dialog
                val listView = dialog.findViewById(R.id.expandable_list) as ExpandableListView
                listView.setAdapter(mAdapter)
                listView.setOnGroupClickListener(ExpandableListView.OnGroupClickListener { parent, v, groupPosition, id ->
                    val group = mAdapter.getGroup(groupPosition)
                    if (group.woeid.toLong() == WORLDWIDE) {
                        persistInt(group.woeid)
                        dialog.dismiss()
                        return@OnGroupClickListener true
                    }
                    false
                })
                listView.setOnChildClickListener { parent, v, groupPosition, childPosition, id ->
                    val child = mAdapter.getChild(groupPosition, childPosition)
                    persistInt(child.woeid)
                    dialog.dismiss()
                    true
                }
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

    companion object {

        private val EMPTY: Long = 0
        private val WORLDWIDE: Long = 1
    }
}
