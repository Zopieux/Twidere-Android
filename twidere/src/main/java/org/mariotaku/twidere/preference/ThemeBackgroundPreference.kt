package org.mariotaku.twidere.preference

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.preference.DialogPreference
import android.support.v7.preference.PreferenceDialogFragmentCompat
import android.support.v7.preference.PreferenceFragmentCompat
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SeekBar

import org.mariotaku.twidere.Constants
import org.mariotaku.twidere.R
import org.mariotaku.twidere.preference.iface.IDialogPreference

import org.mariotaku.twidere.constant.SharedPreferenceConstants.DEFAULT_THEME_BACKGROUND_ALPHA
import org.mariotaku.twidere.constant.SharedPreferenceConstants.KEY_THEME_BACKGROUND
import org.mariotaku.twidere.constant.SharedPreferenceConstants.KEY_THEME_BACKGROUND_ALPHA
import org.mariotaku.twidere.constant.SharedPreferenceConstants.VALUE_THEME_BACKGROUND_TRANSPARENT

/**
 * Created by mariotaku on 14/11/8.
 */
class ThemeBackgroundPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : DialogPreference(context, attrs), Constants, IDialogPreference {

    private val mBackgroundEntries: Array<String>
    private val mBackgroundValues: Array<String>?
    var value: String? = null
        private set
    private var mDefaultValue: String? = null

    init {
        key = KEY_THEME_BACKGROUND
        val resources = context.resources
        mBackgroundEntries = resources.getStringArray(R.array.entries_theme_background)
        mBackgroundValues = resources.getStringArray(R.array.values_theme_background)
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        mDefaultValue = defaultValue as String?
        value = if (restorePersistedValue) getPersistedString(null) else mDefaultValue
        updateSummary()
    }

    private fun updateSummary() {
        val valueIndex = valueIndex
        summary = if (valueIndex != -1) mBackgroundEntries[valueIndex] else null
    }

    private fun persistValue(value: String) {
        // Always persist/notify the first time.
        if (!TextUtils.equals(getPersistedString(null), value)) {
            persistString(value)
            callChangeListener(value)
            notifyChanged()
        }
        updateSummary()
    }

    private val valueIndex: Int
        get() = findIndexOfValue(value)

    fun findIndexOfValue(value: String?): Int {
        if (value != null && mBackgroundValues != null) {
            for (i in mBackgroundValues.indices.reversed()) {
                if (mBackgroundValues[i] == value) {
                    return i
                }
            }
        }
        return -1
    }

    override fun displayDialog(fragment: PreferenceFragmentCompat) {
        val df = InternalDialogFragment.newInstance(key)
        df.setTargetFragment(fragment, 0)
        df.show(fragment.fragmentManager, key)
    }

    private fun saveValue() {
        persistValue(value)
    }

    private fun setSelectedOption(which: Int) {
        if (which < 0) {
            value = mDefaultValue
        } else {
            value = mBackgroundValues!![which]
        }
    }

    class InternalDialogFragment : PreferenceDialogFragmentCompat() {

        private var mAlphaContainer: View? = null
        private var mAlphaSlider: SeekBar? = null

        private val sliderAlpha: Int
            get() = mAlphaSlider!!.progress + MIN_ALPHA

        override fun onDialogClosed(positive: Boolean) {
            if (!positive) return
            val preference = preference as ThemeBackgroundPreference
            val preferences = preference.sharedPreferences
            val editor = preferences.edit()
            editor.putInt(KEY_THEME_BACKGROUND_ALPHA, sliderAlpha)
            editor.apply()
            preference.saveValue()
        }

        private fun updateAlphaVisibility() {
            if (mAlphaContainer == null) return
            val preference = preference as ThemeBackgroundPreference
            val isTransparent = VALUE_THEME_BACKGROUND_TRANSPARENT == preference.value
            mAlphaContainer!!.visibility = if (isTransparent) View.VISIBLE else View.GONE
        }


        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(context)
            val preference = preference as ThemeBackgroundPreference
            val preferences = preference.sharedPreferences
            preference.value = preference.getPersistedString(null)
            builder.setTitle(preference.dialogTitle)
            builder.setSingleChoiceItems(preference.mBackgroundEntries, preference.valueIndex) { dialog, which ->
                preference.setSelectedOption(which)
                updateAlphaVisibility()
            }
            builder.setPositiveButton(android.R.string.ok, this)
            builder.setNegativeButton(android.R.string.cancel, this)
            val dialog = builder.create()
            dialog.setOnShowListener { dialog ->
                if (preferences != null) {
                    val materialDialog = dialog as AlertDialog
                    val inflater = materialDialog.layoutInflater
                    val listView = materialDialog.listView!!
                    val listViewParent = listView.parent as ViewGroup
                    listViewParent.removeView(listView)
                    val view = inflater.inflate(R.layout.dialog_theme_background_preference, listViewParent)
                    (view.findViewById(R.id.list_container) as ViewGroup).addView(listView)
                    mAlphaContainer = view.findViewById(R.id.alpha_container)
                    mAlphaSlider = view.findViewById(R.id.alpha_slider) as SeekBar
                    mAlphaSlider!!.max = MAX_ALPHA - MIN_ALPHA
                    mAlphaSlider!!.progress = preferences.getInt(KEY_THEME_BACKGROUND_ALPHA, DEFAULT_THEME_BACKGROUND_ALPHA) - MIN_ALPHA
                    listView.onItemClickListener = { parent, view, position, id ->
                        preference.setSelectedOption(position)
                        updateAlphaVisibility()
                    }
                    updateAlphaVisibility()
                }
            }
            return dialog
        }

        companion object {

            fun newInstance(key: String): InternalDialogFragment {
                val df = InternalDialogFragment()
                val args = Bundle()
                args.putString(PreferenceDialogFragmentCompat.ARG_KEY, key)
                df.arguments = args
                return df
            }
        }
    }

    companion object {

        val MAX_ALPHA = 0xFF
        val MIN_ALPHA = 0x40
    }
}
