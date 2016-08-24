package org.mariotaku.twidere.preference

import android.content.Context
import android.content.SharedPreferences
import android.preference.ListPreference
import android.util.AttributeSet

open class AutoFixListPreference : ListPreference {

    constructor(context: Context) : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any) {
        try {
            super.onSetInitialValue(restoreValue, defaultValue)
        } catch (e: ClassCastException) {
            val prefs = sharedPreferences
            if (prefs != null) {
                prefs.edit().remove(key).apply()
            }
        }

    }

}
