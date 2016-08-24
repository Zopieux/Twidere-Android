package org.mariotaku.twidere.preference

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.res.TypedArray
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.preference.DialogPreference
import android.support.v7.preference.PreferenceFragmentCompat
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.TextView


import org.mariotaku.twidere.R
import org.mariotaku.twidere.fragment.ThemedPreferenceDialogFragmentCompat
import org.mariotaku.twidere.preference.iface.IDialogPreference
import org.mariotaku.twidere.util.KeyboardShortcutsHandler
import org.mariotaku.twidere.util.KeyboardShortcutsHandler.KeyboardShortcutSpec
import org.mariotaku.twidere.util.dagger.GeneralComponentHelper

import javax.inject.Inject

import org.mariotaku.twidere.TwidereConstants.LOGTAG

/**
 * Created by mariotaku on 16/3/15.
 */
class KeyboardShortcutPreference : DialogPreference, IDialogPreference {

    private var mPreferencesChangeListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    private var contextTag: String? = null
    var action: String? = null
        private set

    @Inject
    var keyboardShortcutsHandler: KeyboardShortcutsHandler? = null
        internal set

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    private fun init(context: Context, set: AttributeSet) {
        GeneralComponentHelper.build(context).inject(this)
        val a = context.obtainStyledAttributes(set, R.styleable.KeyboardShortcutPreference)
        contextTag = a.getString(R.styleable.KeyboardShortcutPreference_android_tag)
        action = a.getString(R.styleable.KeyboardShortcutPreference_android_action)
        a.recycle()

        if (TextUtils.isEmpty(action)) {
            throw IllegalArgumentException("android:action required")
        }
        key = action

        dialogLayoutResource = R.layout.dialog_keyboard_shortcut_input
        isPersistent = false
        dialogTitle = KeyboardShortcutsHandler.getActionLabel(context, action)
        title = KeyboardShortcutsHandler.getActionLabel(context, action)
        mPreferencesChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { preferences, key -> updateSummary() }
        updateSummary()
    }

    override fun onPrepareForRemoval() {
        keyboardShortcutsHandler!!.unregisterOnSharedPreferenceChangeListener(mPreferencesChangeListener!!)
        super.onPrepareForRemoval()
    }

    override fun onAttached() {
        super.onAttached()
        keyboardShortcutsHandler!!.registerOnSharedPreferenceChangeListener(mPreferencesChangeListener!!)
    }

    private fun updateSummary() {
        val spec = keyboardShortcutsHandler!!.findKey(action!!)
        summary = spec?.toKeyString()
    }

    override fun displayDialog(fragment: PreferenceFragmentCompat) {
        val df = KeyboardShortcutDialogFragment.newInstance(action)
        df.setTargetFragment(fragment, 0)
        df.show(fragment.fragmentManager, action)
    }

    class KeyboardShortcutDialogFragment : ThemedPreferenceDialogFragmentCompat(), DialogInterface.OnKeyListener {

        private var mKeysLabel: TextView? = null
        private var mConflictLabel: TextView? = null

        private var mKeySpec: KeyboardShortcutSpec? = null
        private var mModifierStates: Int = 0


        override fun onDialogClosed(positiveResult: Boolean) {

        }

        override fun onPrepareDialogBuilder(builder: AlertDialog.Builder?) {
            builder!!.setPositiveButton(android.R.string.ok, this)
            builder.setNegativeButton(android.R.string.cancel, this)
            builder.setNeutralButton(R.string.clear, this)
            builder.setOnKeyListener(this)
        }

        override fun onKey(dialog: DialogInterface, keyCode: Int, event: KeyEvent): Boolean {
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (KeyEvent.isModifierKey(keyCode)) {
                    mModifierStates = mModifierStates or KeyboardShortcutsHandler.getMetaStateForKeyCode(keyCode)
                }
            } else if (event.action != KeyEvent.ACTION_UP) {
                return false
            }
            if (KeyEvent.isModifierKey(keyCode)) {
                mModifierStates = mModifierStates and KeyboardShortcutsHandler.getMetaStateForKeyCode(keyCode).inv()
            }
            val preference = preference as KeyboardShortcutPreference
            val action = preference.action
            val contextTag = preference.contextTag
            val handler = preference.keyboardShortcutsHandler

            val spec = KeyboardShortcutsHandler.getKeyboardShortcutSpec(contextTag,
                    keyCode, event, KeyEvent.normalizeMetaState(mModifierStates or event.metaState))
            if (spec == null || !spec.isValid) {
                Log.d(LOGTAG, String.format("Invalid key %s", event), Exception())
                return false
            }
            mKeySpec = spec
            mKeysLabel!!.text = spec.toKeyString()
            val oldAction = handler.findAction(spec)
            val context = context
            if (action == oldAction || TextUtils.isEmpty(oldAction)) {
                mConflictLabel!!.visibility = View.GONE
                if (dialog is AlertDialog) {
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setText(android.R.string.ok)
                }
            } else {
                mConflictLabel!!.visibility = View.VISIBLE
                val label = KeyboardShortcutsHandler.getActionLabel(context, oldAction)
                mConflictLabel!!.text = context.getString(R.string.conflicts_with_name, label)
                if (dialog is AlertDialog) {
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setText(R.string.overwrite)
                }
            }
            return true
        }

        override fun onBindDialogView(view: View) {
            super.onBindDialogView(view)
            mKeysLabel = view.findViewById(R.id.keys_label) as TextView
            mConflictLabel = view.findViewById(R.id.conflict_label) as TextView
            mConflictLabel!!.visibility = View.GONE
        }


        override fun onClick(dialog: DialogInterface?, which: Int) {
            val preference = preference as KeyboardShortcutPreference
            val action = preference.action
            val handler = preference.keyboardShortcutsHandler
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    if (mKeySpec == null) return
                    handler.register(mKeySpec!!, action)
                }
                DialogInterface.BUTTON_NEUTRAL -> {
                    handler.unregister(action)
                }
            }
        }

        companion object {

            fun newInstance(key: String): KeyboardShortcutDialogFragment {
                val df = KeyboardShortcutDialogFragment()
                val args = Bundle()
                args.putString(PreferenceDialogFragmentCompat.ARG_KEY, key)
                df.arguments = args
                return df
            }
        }

    }
}
