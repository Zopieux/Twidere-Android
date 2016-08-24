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
import android.content.DialogInterface
import android.content.res.TypedArray
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.preference.DialogPreference
import android.support.v7.preference.PreferenceDialogFragmentCompat
import android.support.v7.preference.PreferenceFragmentCompat
import android.util.AttributeSet

import org.mariotaku.twidere.Constants
import org.mariotaku.twidere.R
import org.mariotaku.twidere.fragment.ThemedPreferenceDialogFragmentCompat
import org.mariotaku.twidere.preference.iface.IDialogPreference

import org.mariotaku.twidere.constant.SharedPreferenceConstants.VALUE_NOTIFICATION_FLAG_LIGHT
import org.mariotaku.twidere.constant.SharedPreferenceConstants.VALUE_NOTIFICATION_FLAG_RINGTONE
import org.mariotaku.twidere.constant.SharedPreferenceConstants.VALUE_NOTIFICATION_FLAG_VIBRATION

class NotificationTypePreference @JvmOverloads constructor(context: Context, attrs: AttributeSet = null, defStyle: Int = R.attr.dialogPreferenceStyle) : DialogPreference(context, attrs, defStyle), Constants, IDialogPreference {

    val defaultValue: Int

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.NotificationTypePreference)
        defaultValue = a.getInteger(R.styleable.NotificationTypePreference_notificationType, 0)
        a.recycle()
    }

    private fun getCheckedItems(value: Int): BooleanArray {
        val flags = flags
        val checkedItems = BooleanArray(flags.size)
        var i = 0
        val j = flags.size
        while (i < j) {
            checkedItems[i] = value and flags[i] != 0
            i++
        }
        return checkedItems
    }

    private val entries: Array<String>
        get() {
            val context = context
            val entries = arrayOfNulls<String>(3)
            entries[0] = context.getString(R.string.ringtone)
            entries[1] = context.getString(R.string.vibration)
            entries[2] = context.getString(R.string.light)
            return entries
        }

    override fun getSummary(): CharSequence {
        val sb = StringBuilder()
        val entries = entries
        val states = getCheckedItems(getPersistedInt(defaultValue))
        var i = 0
        val j = entries.size
        while (i < j) {
            if (states[i]) {
                if (sb.length != 0) {
                    sb.append(", ")
                }
                sb.append(entries[i])
            }
            i++
        }
        return sb
    }

    private val flags: IntArray
        get() = intArrayOf(VALUE_NOTIFICATION_FLAG_RINGTONE, VALUE_NOTIFICATION_FLAG_VIBRATION, VALUE_NOTIFICATION_FLAG_LIGHT)

    override fun displayDialog(fragment: PreferenceFragmentCompat) {
        val df = NotificationTypeDialogFragment.newInstance(key)
        df.setTargetFragment(fragment, 0)
        df.show(fragment.fragmentManager, key)
    }

    class NotificationTypeDialogFragment : ThemedPreferenceDialogFragmentCompat(), DialogInterface.OnMultiChoiceClickListener {

        private var mCheckedItems: BooleanArray? = null

        override fun onPrepareDialogBuilder(builder: AlertDialog.Builder?) {
            val preference = preference as NotificationTypePreference
            val value = preference.getPersistedInt(preference.defaultValue)
            mCheckedItems = preference.getCheckedItems(value)
            builder!!.setMultiChoiceItems(preference.entries, mCheckedItems, this)
        }

        override fun onDialogClosed(positive: Boolean) {
            if (!positive || mCheckedItems == null) return
            val preference = preference as NotificationTypePreference
            var value = 0
            val flags = preference.flags
            var i = 0
            val j = flags.size
            while (i < j) {
                if (mCheckedItems!![i]) {
                    value = value or flags[i]
                }
                i++
            }
            preference.persistInt(value)
            preference.callChangeListener(value)
            preference.notifyChanged()
        }

        override fun onClick(dialog: DialogInterface, which: Int, isChecked: Boolean) {
            mCheckedItems[which] = isChecked
        }

        companion object {

            fun newInstance(key: String): NotificationTypeDialogFragment {
                val df = NotificationTypeDialogFragment()
                val args = Bundle()
                args.putString(PreferenceDialogFragmentCompat.ARG_KEY, key)
                df.arguments = args
                return df
            }
        }

    }
}
