/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package org.mariotaku.twidere.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.preference.ListPreference
import android.support.v7.preference.PreferenceDialogFragmentCompat

import org.mariotaku.twidere.util.TwidereArrayUtils

class ThemedListPreferenceDialogFragmentCompat : ThemedPreferenceDialogFragmentCompat() {
    private var mClickedDialogEntryIndex: Int = 0

    private val listPreference: ListPreference
        get() = preference as ListPreference

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        super.onPrepareDialogBuilder(builder)
        val preference = listPreference
        val entries = preference.entries?.map(Any::toString)?.toTypedArray()
        if (entries == null || preference.entryValues == null) {
            throw IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array.")
        }
        mClickedDialogEntryIndex = preference.findIndexOfValue(preference.value)
        builder.setSingleChoiceItems(entries, mClickedDialogEntryIndex,
                DialogInterface.OnClickListener { dialog, which ->
                    mClickedDialogEntryIndex = which
                    /*
                         * Clicking on an item simulates the positive button
                         * click, and dismisses the dialog.
                         */
                    this@ThemedListPreferenceDialogFragmentCompat.onClick(dialog,
                            DialogInterface.BUTTON_POSITIVE)
                    dialog.dismiss()
                })
        /*
         * The typical interaction for list-based dialogs is to have
         * click-on-an-item dismiss the dialog instead of the user having to
         * press 'Ok'.
         */
        //noinspection ConstantConditions
        builder.setPositiveButton(null, null)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        val preference = listPreference
        if (positiveResult && mClickedDialogEntryIndex >= 0 &&
                preference.entryValues != null) {
            val value = preference.entryValues[mClickedDialogEntryIndex].toString()
            if (preference.callChangeListener(value)) {
                preference.value = value
            }
        }
    }

    companion object {

        fun newInstance(key: String): ThemedListPreferenceDialogFragmentCompat {
            val fragment = ThemedListPreferenceDialogFragmentCompat()
            val b = Bundle(1)
            b.putString(PreferenceDialogFragmentCompat.ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }
}