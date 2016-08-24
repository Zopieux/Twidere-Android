package org.mariotaku.twidere.preference

import android.content.Context
import android.support.v7.preference.EditTextPreference
import android.support.v7.preference.PreferenceFragmentCompat
import android.util.AttributeSet

import org.mariotaku.twidere.fragment.ThemedEditTextPreferenceDialogFragmentCompat
import org.mariotaku.twidere.preference.iface.IDialogPreference

/**
 * Created by mariotaku on 16/3/15.
 */
class ThemedEditTextPreference : EditTextPreference, IDialogPreference {
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    constructor(context: Context) : super(context) {
    }

    override fun displayDialog(fragment: PreferenceFragmentCompat) {
        val df = ThemedEditTextPreferenceDialogFragmentCompat.newInstance(key)
        df.setTargetFragment(fragment, 0)
        df.show(fragment.fragmentManager, key)
    }
}
