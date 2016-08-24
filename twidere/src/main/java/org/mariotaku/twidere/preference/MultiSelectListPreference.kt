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
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.content.DialogInterface.OnMultiChoiceClickListener
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.preference.DialogPreference
import android.support.v7.preference.PreferenceDialogFragmentCompat
import android.support.v7.preference.PreferenceFragmentCompat
import android.util.AttributeSet

import org.mariotaku.twidere.R
import org.mariotaku.twidere.preference.iface.IDialogPreference

internal abstract class MultiSelectListPreference @JvmOverloads protected constructor(context: Context, attrs: AttributeSet = null, defStyle: Int = R.attr.dialogPreferenceStyle) : DialogPreference(context, attrs, defStyle), IDialogPreference {


    override fun displayDialog(fragment: PreferenceFragmentCompat) {
        val df = MultiSelectListDialogFragment.newInstance(key)
        df.setTargetFragment(fragment, 0)
        df.show(fragment.childFragmentManager, key)
    }

    protected abstract val defaults: BooleanArray

    protected val defaultSharedPreferences: SharedPreferences
        get() = sharedPreferences

    protected abstract val keys: Array<String>

    protected abstract val names: Array<String>

    class MultiSelectListDialogFragment : PreferenceDialogFragmentCompat(), OnMultiChoiceClickListener, OnClickListener {

        private var mValues: BooleanArray? = null
        private var mDefaultValues: BooleanArray? = null
        private var mPreferences: SharedPreferences? = null
        private var mNames: Array<String>? = null
        private var mKeys: Array<String>? = null

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val preference = preference as MultiSelectListPreference
            mNames = preference.names
            mKeys = preference.keys
            mDefaultValues = preference.defaults

            val length = mKeys!!.size
            if (length != mNames!!.size || length != mDefaultValues!!.size)
                throw IllegalArgumentException()
            mValues = BooleanArray(length)
            mPreferences = preference.defaultSharedPreferences
            val builder = AlertDialog.Builder(context)
            for (i in 0..length - 1) {
                mValues[i] = mPreferences!!.getBoolean(mKeys!![i], mDefaultValues!![i])
            }
            builder.setTitle(preference.dialogTitle)
            builder.setPositiveButton(android.R.string.ok, this)
            builder.setNegativeButton(android.R.string.cancel, null)
            builder.setMultiChoiceItems(mNames, mValues, this)
            return builder.create()
        }

        override fun onDialogClosed(positive: Boolean) {
            if (mPreferences == null || !positive) return
            val editor = mPreferences!!.edit()
            val length = mKeys!!.size
            for (i in 0..length - 1) {
                editor.putBoolean(mKeys!![i], mValues!![i])
            }
            editor.apply()
        }

        override fun onClick(dialog: DialogInterface, which: Int, isChecked: Boolean) {
            mValues[which] = isChecked
        }

        companion object {

            fun newInstance(key: String): MultiSelectListDialogFragment {
                val df = MultiSelectListDialogFragment()
                val args = Bundle()
                args.putString(PreferenceDialogFragmentCompat.ARG_KEY, key)
                df.arguments = args
                return df
            }
        }
    }

}
