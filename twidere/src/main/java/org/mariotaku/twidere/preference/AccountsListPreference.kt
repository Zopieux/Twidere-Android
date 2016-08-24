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
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.support.v7.internal.widget.PreferenceImageView
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceCategory
import android.support.v7.preference.PreferenceManager
import android.support.v7.preference.PreferenceViewHolder
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TextView

import com.nostra13.universalimageloader.core.assist.FailReason
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener

import org.mariotaku.twidere.Constants
import org.mariotaku.twidere.R
import org.mariotaku.twidere.model.ParcelableAccount
import org.mariotaku.twidere.util.BitmapUtils
import org.mariotaku.twidere.util.DataStoreUtils
import org.mariotaku.twidere.util.MediaLoaderWrapper
import org.mariotaku.twidere.util.dagger.GeneralComponentHelper

import javax.inject.Inject

import org.mariotaku.twidere.TwidereConstants.ACCOUNT_PREFERENCES_NAME_PREFIX

abstract class AccountsListPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet = null, defStyle: Int = R.attr.preferenceCategoryStyle) : PreferenceCategory(context, attrs, defStyle), Constants {

    private val mSwitchKey: String?
    private val mSwitchDefault: Boolean

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.AccountsListPreference)
        mSwitchKey = a.getString(R.styleable.AccountsListPreference_switchKey)
        mSwitchDefault = a.getBoolean(R.styleable.AccountsListPreference_switchDefault, false)
        a.recycle()
    }

    fun setAccountsData(accounts: List<ParcelableAccount>) {
        removeAll()
        for (account in accounts) {
            val preference = AccountItemPreference(context, account,
                    mSwitchKey, mSwitchDefault)
            setupPreference(preference, account)
            addPreference(preference)
        }
        val preference = Preference(context)
        preference.layoutResource = R.layout.settings_layout_click_to_config
        addPreference(preference)
    }

    override fun onAttachedToHierarchy(preferenceManager: PreferenceManager) {
        super.onAttachedToHierarchy(preferenceManager)
        if (preferenceCount > 0) return
        setAccountsData(DataStoreUtils.getAccountsList(context, false))
    }

    protected abstract fun setupPreference(preference: AccountItemPreference, account: ParcelableAccount)

    class AccountItemPreference(context: Context, private val mAccount: ParcelableAccount, switchKey: String,
                                switchDefault: Boolean) : Preference(context), ImageLoadingListener, OnSharedPreferenceChangeListener {
        private val mSwitchPreference: SharedPreferences

        @Inject
        internal var mImageLoader: MediaLoaderWrapper? = null

        init {
            GeneralComponentHelper.build(context).inject(this)
            val switchPreferenceName = ACCOUNT_PREFERENCES_NAME_PREFIX + mAccount.account_key
            mSwitchPreference = context.getSharedPreferences(switchPreferenceName, Context.MODE_PRIVATE)
            mSwitchPreference.registerOnSharedPreferenceChangeListener(this)
            title = mAccount.name
            summary = String.format("@%s", mAccount.screen_name)
            mImageLoader!!.loadProfileImage(mAccount, this)
        }

        override fun onLoadingCancelled(imageUri: String, view: View) {
            //            setIcon(R.drawable.ic_profile_image_default);
        }

        override fun onLoadingComplete(imageUri: String, view: View, loadedImage: Bitmap) {
            val roundedBitmap = BitmapUtils.getCircleBitmap(loadedImage)
            val icon = BitmapDrawable(context.resources, roundedBitmap)
            icon.gravity = Gravity.FILL
            setIcon(icon)
        }

        override fun onLoadingFailed(imageUri: String, view: View, failReason: FailReason) {
            //            setIcon(R.drawable.ic_profile_image_default);
        }

        override fun onLoadingStarted(imageUri: String, view: View) {
            //            setIcon(R.drawable.ic_profile_image_default);
        }

        override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String) {
            notifyChanged()
        }


        override fun onBindViewHolder(holder: PreferenceViewHolder) {
            super.onBindViewHolder(holder)
            val iconView = holder.findViewById(android.R.id.icon)
            if (iconView is PreferenceImageView) {
                val maxSize = context.resources.getDimensionPixelSize(R.dimen.element_size_normal)
                iconView.minimumWidth = maxSize
                iconView.minimumHeight = maxSize
                iconView.maxWidth = maxSize
                iconView.maxHeight = maxSize
                iconView.scaleType = ImageView.ScaleType.CENTER_CROP
            }
            val titleView = holder.findViewById(android.R.id.title)
            if (titleView is TextView) {
                titleView.setSingleLine(true)
            }
            val summaryView = holder.findViewById(android.R.id.summary)
            if (summaryView is TextView) {
                summaryView.setSingleLine(true)
            }
        }
    }

}
