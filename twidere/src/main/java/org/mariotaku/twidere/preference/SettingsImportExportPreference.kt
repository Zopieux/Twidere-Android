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

package org.mariotaku.twidere.preference

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.preference.DialogPreference
import android.support.v7.preference.PreferenceDialogFragmentCompat
import android.support.v7.preference.PreferenceFragmentCompat
import android.util.AttributeSet

import org.mariotaku.twidere.R
import org.mariotaku.twidere.activity.DataExportActivity
import org.mariotaku.twidere.activity.DataImportActivity
import org.mariotaku.twidere.preference.iface.IDialogPreference

/**
 * Created by mariotaku on 15/3/19.
 */
class SettingsImportExportPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : DialogPreference(context, attrs), IDialogPreference {

    override fun displayDialog(fragment: PreferenceFragmentCompat) {
        val df = ImportExportDialogFragment.newInstance(key)
        df.setTargetFragment(fragment, 0)
        df.show(fragment.fragmentManager, key)
    }

    class ImportExportDialogFragment : PreferenceDialogFragmentCompat() {


        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(context)
            val context = context
            val entries = arrayOfNulls<String>(2)
            val values = arrayOfNulls<Intent>(2)
            entries[0] = context.getString(R.string.import_settings)
            entries[1] = context.getString(R.string.export_settings)
            values[0] = Intent(context, DataImportActivity::class.java)
            values[1] = Intent(context, DataExportActivity::class.java)
            builder.setItems(entries) { dialog, which -> startActivity(values[which]) }
            return builder.create()
        }

        override fun onDialogClosed(positive: Boolean) {

        }

        companion object {

            fun newInstance(key: String): ImportExportDialogFragment {
                val df = ImportExportDialogFragment()
                val args = Bundle()
                args.putString(PreferenceDialogFragmentCompat.ARG_KEY, key)
                df.arguments = args
                return df
            }
        }
    }

}
