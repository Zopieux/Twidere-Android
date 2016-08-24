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

package org.mariotaku.twidere.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import org.mariotaku.microblog.library.twitter.model.ResponseList
import org.mariotaku.microblog.library.twitter.model.SavedSearch
import org.mariotaku.twidere.model.UserKey

/**
 * Created by mariotaku on 15/4/29.
 */
class SavedSearchesAdapter(context: Context) : BaseAdapter() {

    private var data: ResponseList<SavedSearch>? = null
    private val mInflater: LayoutInflater

    init {
        mInflater = LayoutInflater.from(context)
    }

    fun findItem(id: Long): SavedSearch? {
        for (i in 0 until count) {
            if (id != -1L && id == getItemId(i)) return getItem(i)
        }
        return null
    }

    override fun getCount(): Int {
        return if (data != null) data!!.size else 0
    }

    override fun getItem(position: Int): SavedSearch? {
        return if (data != null) data!![position] else null
    }

    override fun getItemId(position: Int): Long {
        return if (data != null) data!![position].id else -1
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: mInflater.inflate(android.R.layout.simple_list_item_1, parent, false)
        val text = view.findViewById(android.R.id.text1) as TextView
        text.text = getItem(position)!!.name
        return view
    }

    fun setData(data: ResponseList<SavedSearch>) {
        this.data = data
        notifyDataSetChanged()
    }

    fun removeItem(accountId: UserKey, searchId: Long): Boolean {
        if (data == null) return false
        var i = 0
        val mDataSize = data!!.size
        while (i < mDataSize) {
            val search = data!![i]
            if (search.id == searchId) {
                data!!.removeAt(i)
                notifyDataSetChanged()
                return true
            }
            i++
        }
        return false
    }
}
