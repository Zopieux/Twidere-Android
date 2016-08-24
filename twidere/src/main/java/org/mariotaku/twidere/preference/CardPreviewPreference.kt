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
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceViewHolder
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet

import org.mariotaku.twidere.R
import org.mariotaku.twidere.adapter.DummyItemAdapter
import org.mariotaku.twidere.graphic.like.LikeAnimationDrawable
import org.mariotaku.twidere.view.holder.StatusViewHolder
import org.mariotaku.twidere.view.holder.iface.IStatusViewHolder

import org.mariotaku.twidere.TwidereConstants.SHARED_PREFERENCES_NAME

class CardPreviewPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet = null, defStyle: Int = 0) : Preference(context, attrs, defStyle), OnSharedPreferenceChangeListener {

    private var mHolder: StatusViewHolder? = null
    private val mAdapter: DummyItemAdapter

    init {
        val preferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE)
        layoutResource = R.layout.layout_preferences_card_preview_compact
        preferences.registerOnSharedPreferenceChangeListener(this)
        mAdapter = DummyItemAdapter(context)
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String) {
        if (mHolder == null) return
        mAdapter.updateOptions()
        notifyChanged()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        if (mHolder == null) {
            mHolder = StatusViewHolder(mAdapter, holder.itemView)
        }
        mHolder!!.setupViewOptions()
        mHolder!!.displaySampleStatus()
        mHolder!!.setStatusClickListener(object : IStatusViewHolder.SimpleStatusClickListener() {
            override fun onItemActionClick(holder: RecyclerView.ViewHolder, id: Int, position: Int) {
                if (id == R.id.favorite) {
                    (holder as StatusViewHolder).playLikeAnimation(LikeAnimationDrawable.OnLikedListener { false })
                }
            }
        })
        super.onBindViewHolder(holder)
    }

}
