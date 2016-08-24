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

package org.mariotaku.twidere.fragment

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.*
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import android.widget.TextView
import org.mariotaku.twidere.R
import org.mariotaku.twidere.adapter.ArrayAdapter
import org.mariotaku.twidere.constant.IntentConstants.*
import org.mariotaku.twidere.fragment.iface.ISupportDialogFragmentCallback
import org.mariotaku.twidere.util.DataImportExportUtils

class DataExportImportTypeSelectorDialogFragment : BaseDialogFragment(), OnMultiChoiceClickListener, OnClickListener, OnShowListener, OnItemClickListener {

    private var mAdapter: TypeAdapter? = null
    private var mListView: ListView? = null

    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)
        val a = activity
        if (a is Callback) {
            a.onCancelled(this)
        }
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> {
                val flags = checkedFlags
                onPositiveButtonClicked(flags)
            }
        }
    }

    override fun onClick(dialog: DialogInterface, which: Int, isChecked: Boolean) {
        updatePositiveButton(dialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = activity
        val flags = enabledFlags
        mAdapter = TypeAdapter(context, flags)
        mListView = ListView(context)
        mAdapter!!.add(Type(R.string.settings, DataImportExportUtils.FLAG_PREFERENCES))
        mAdapter!!.add(Type(R.string.nicknames, DataImportExportUtils.FLAG_NICKNAMES))
        mAdapter!!.add(Type(R.string.user_colors, DataImportExportUtils.FLAG_USER_COLORS))
        mAdapter!!.add(Type(R.string.custom_host_mapping, DataImportExportUtils.FLAG_HOST_MAPPING))
        mAdapter!!.add(Type(R.string.keyboard_shortcuts, DataImportExportUtils.FLAG_KEYBOARD_SHORTCUTS))
        mAdapter!!.add(Type(R.string.filters, DataImportExportUtils.FLAG_FILTERS))
        mAdapter!!.add(Type(R.string.tabs, DataImportExportUtils.FLAG_TABS))
        mListView!!.adapter = mAdapter
        mListView!!.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        mListView!!.onItemClickListener = this
        var i = 0
        val j = mAdapter!!.count
        while (i < j) {
            mListView!!.setItemChecked(i, mAdapter!!.isEnabled(i))
            i++
        }
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setView(mListView)
        builder.setPositiveButton(android.R.string.ok, this)
        builder.setNegativeButton(android.R.string.cancel, null)
        val dialog = builder.create()
        dialog.setOnShowListener(this)
        return dialog
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        val a = activity
        if (a is Callback) {
            a.onDismissed(this)
        }
    }

    override fun onItemClick(view: AdapterView<*>, child: View, position: Int, id: Long) {
        updatePositiveButton(dialog)
    }

    override fun onShow(dialog: DialogInterface) {
        updatePositiveButton(dialog)
    }

    private val checkedFlags: Int
        get() {
            val checked = mListView!!.checkedItemPositions
            var flags = 0
            var i = 0
            val j = checked.size()
            while (i < j) {
                val type = mListView!!.getItemAtPosition(i) as Type
                if (checked.valueAt(i)) {
                    flags = flags or type.flag
                }
                i++
            }
            return flags
        }

    private val enabledFlags: Int
        get() {
            val args = arguments ?: return DataImportExportUtils.FLAG_ALL
            return args.getInt(EXTRA_FLAGS, DataImportExportUtils.FLAG_ALL)
        }

    private val title: CharSequence?
        get() {
            val args = arguments ?: return null
            return args.getCharSequence(EXTRA_TITLE)
        }

    private fun onPositiveButtonClicked(flags: Int) {
        val a = activity
        val args = arguments ?: return
        val path = args.getString(EXTRA_PATH)
        if (a is Callback) {
            a.onPositiveButtonClicked(path, flags)
        }
    }

    private fun updatePositiveButton(dialog: DialogInterface) {
        if (dialog !is AlertDialog) return
        val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        positiveButton.isEnabled = checkedFlags != 0
    }

    interface Callback : ISupportDialogFragmentCallback {
        fun onPositiveButtonClicked(path: String, flags: Int)
    }

    private class Type internal constructor(internal val title: Int, internal val flag: Int)

    private class TypeAdapter(context: Context, private val mEnabledFlags: Int) : ArrayAdapter<Type>(context, android.R.layout.simple_list_item_multiple_choice) {

        override fun areAllItemsEnabled(): Boolean {
            return false
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            val text1 = view.findViewById(android.R.id.text1) as TextView
            text1.setText(getItem(position).title)
            view.isEnabled = isEnabled(position)
            return view
        }

        override fun isEnabled(position: Int): Boolean {
            return mEnabledFlags and getItem(position).flag != 0
        }

    }

}
