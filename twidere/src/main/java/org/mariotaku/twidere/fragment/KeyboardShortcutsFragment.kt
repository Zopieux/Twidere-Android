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

package org.mariotaku.twidere.fragment

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.support.v7.preference.Preference
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem

import org.mariotaku.twidere.R
import org.mariotaku.twidere.activity.KeyboardShortcutPreferenceCompatActivity
import org.mariotaku.twidere.constant.KeyboardShortcutConstants
import org.mariotaku.twidere.util.KeyboardShortcutsHandler
import org.mariotaku.twidere.util.KeyboardShortcutsHandler.KeyboardShortcutSpec

/**
 * Created by mariotaku on 15/4/10.
 */
class KeyboardShortcutsFragment : BasePreferenceFragment(), KeyboardShortcutConstants {

    override fun onCreatePreferences(savedInstanceState: Bundle, rootKey: String) {
        addPreferencesFromResource(R.xml.preferences_keyboard_shortcuts)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.menu_keyboard_shortcuts, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.reset -> {
                val f = ResetKeyboardShortcutConfirmDialogFragment()
                f.show(fragmentManager, "reset_keyboard_shortcut_confirm")
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private class KeyboardShortcutPreferenceCompat(context: Context, private val mKeyboardShortcutHandler: KeyboardShortcutsHandler,
                                                   private val mContextTag: String?, private val mAction: String) : Preference(context) {
        private val mPreferencesChangeListener: OnSharedPreferenceChangeListener

        init {
            isPersistent = false
            title = KeyboardShortcutsHandler.getActionLabel(context, mAction)
            mPreferencesChangeListener = OnSharedPreferenceChangeListener { preferences, key -> updateSummary() }
            updateSummary()
        }

        override fun onClick() {
            val context = context
            val intent = Intent(context, KeyboardShortcutPreferenceCompatActivity::class.java)
            intent.putExtra(KeyboardShortcutPreferenceCompatActivity.EXTRA_CONTEXT_TAG, mContextTag)
            intent.putExtra(KeyboardShortcutPreferenceCompatActivity.EXTRA_KEY_ACTION, mAction)
            context.startActivity(intent)
        }

        private fun updateSummary() {
            val spec = mKeyboardShortcutHandler.findKey(mAction)
            summary = spec?.toKeyString()
        }

        override fun onAttached() {
            super.onAttached()
            mKeyboardShortcutHandler.registerOnSharedPreferenceChangeListener(mPreferencesChangeListener)
        }

        override fun onPrepareForRemoval() {
            mKeyboardShortcutHandler.unregisterOnSharedPreferenceChangeListener(mPreferencesChangeListener)
            super.onPrepareForRemoval()
        }


    }

    class ResetKeyboardShortcutConfirmDialogFragment : BaseDialogFragment(), OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    keyboardShortcutsHandler.reset()
                }
            }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(activity)
            builder.setMessage(R.string.reset_keyboard_shortcuts_confirm)
            builder.setPositiveButton(android.R.string.ok, this)
            builder.setNegativeButton(android.R.string.cancel, this)
            return builder.create()
        }
    }
}
