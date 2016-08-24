package org.mariotaku.twidere.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.support.v4.util.SparseArrayCompat
import android.text.TextUtils
import android.view.KeyEvent
import org.mariotaku.twidere.R
import org.mariotaku.twidere.TwidereConstants.KEYBOARD_SHORTCUTS_PREFERENCES_NAME
import org.mariotaku.twidere.activity.ComposeActivity
import org.mariotaku.twidere.activity.QuickSearchBarActivity
import org.mariotaku.twidere.constant.IntentConstants.INTENT_ACTION_COMPOSE
import org.mariotaku.twidere.constant.IntentConstants.INTENT_ACTION_QUICK_SEARCH
import org.mariotaku.twidere.constant.KeyboardShortcutConstants
import org.mariotaku.twidere.constant.KeyboardShortcutConstants.*
import java.util.*
import javax.inject.Singleton

@Singleton
class KeyboardShortcutsHandler(context: Context) {

    private val preferences: SharedPreferencesWrapper

    init {
        preferences = SharedPreferencesWrapper.getInstance(context,
                KEYBOARD_SHORTCUTS_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    fun findAction(spec: KeyboardShortcutSpec): String {
        return preferences.getString(spec.rawKey, null)
    }

    fun findKey(action: String): KeyboardShortcutSpec? {
        for ((key, value) in preferences.all) {
            if (action == value) {
                val spec = KeyboardShortcutSpec(key, action)
                if (spec.isValid) return spec
            }
        }
        return null
    }

    fun getKeyAction(contextTag: String, keyCode: Int, event: KeyEvent, metaState: Int): String? {
        if (!isValidForHotkey(keyCode, event)) return null
        val key = getKeyEventKey(contextTag, keyCode, event, metaState)
        return preferences.getString(key, null)
    }

    fun handleKey(context: Context, contextTag: String, keyCode: Int, event: KeyEvent, metaState: Int): Boolean {
        val action = getKeyAction(contextTag, keyCode, event, metaState) ?: return false
        when (action) {
            ACTION_COMPOSE -> {
                context.startActivity(Intent(context, ComposeActivity::class.java).setAction(INTENT_ACTION_COMPOSE))
                return true
            }
            ACTION_SEARCH -> {
                context.startActivity(Intent(context, QuickSearchBarActivity::class.java).setAction(INTENT_ACTION_QUICK_SEARCH))
                return true
            }
            ACTION_MESSAGE -> {
                IntentUtils.openMessageConversation(context, null, null)
                return true
            }
        }
        return false
    }

    fun register(spec: KeyboardShortcutSpec, action: String) {
        unregister(action)
        preferences.edit().putString(spec.rawKey, action).apply()
    }

    fun registerOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener) {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun reset() {
        val editor = preferences.edit()
        editor.clear()
        editor.putString("n", ACTION_COMPOSE)
        editor.putString("m", ACTION_MESSAGE)
        editor.putString("slash", ACTION_SEARCH)
        editor.putString("home.q", ACTION_HOME_ACCOUNTS_DASHBOARD)
        editor.putString("navigation.period", ACTION_NAVIGATION_REFRESH)
        editor.putString("navigation.j", ACTION_NAVIGATION_NEXT)
        editor.putString("navigation.k", ACTION_NAVIGATION_PREVIOUS)
        editor.putString("navigation.h", ACTION_NAVIGATION_PREVIOUS_TAB)
        editor.putString("navigation.l", ACTION_NAVIGATION_NEXT_TAB)
        editor.putString("navigation.u", ACTION_NAVIGATION_TOP)
        editor.putString("status.f", ACTION_STATUS_FAVORITE)
        editor.putString("status.r", ACTION_STATUS_REPLY)
        editor.putString("status.t", ACTION_STATUS_RETWEET)
        editor.apply()
    }

    fun unregister(action: String) {
        val editor = preferences.edit()
        for ((key, value) in preferences.all) {
            if (action == value) {
                val spec = KeyboardShortcutSpec(key, action)
                if (spec.isValid) {
                    editor.remove(spec.rawKey)
                }
            }
        }
        editor.apply()
    }

    fun unregisterOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    interface KeyboardShortcutCallback : KeyboardShortcutConstants {

        fun handleKeyboardShortcutRepeat(handler: KeyboardShortcutsHandler, keyCode: Int,
                                         repeatCount: Int, event: KeyEvent, metaState: Int): Boolean

        fun handleKeyboardShortcutSingle(handler: KeyboardShortcutsHandler, keyCode: Int,
                                         event: KeyEvent, metaState: Int): Boolean

        fun isKeyboardShortcutHandled(handler: KeyboardShortcutsHandler, keyCode: Int,
                                      event: KeyEvent, metaState: Int): Boolean
    }

    interface TakeAllKeyboardShortcut

    /**
     * Created by mariotaku on 15/4/11.
     */
    class KeyboardShortcutSpec {

        var action: String? = null
            private set
        var contextTag: String? = null
        var keyMeta: Int = 0
            private set
        var keyName: String? = null
            private set

        constructor(contextTag: String, keyMeta: Int, keyName: String, action: String) {
            this.contextTag = contextTag
            this.keyMeta = keyMeta
            this.keyName = keyName
            this.action = action
        }

        constructor(key: String, action: String) {
            val contextDotIdx = key.indexOf('.')
            if (contextDotIdx != -1) {
                contextTag = key.substring(0, contextDotIdx)
            }
            var idx = contextDotIdx
            var previousIdx = idx
            while ((idx = key.indexOf('+', idx + 1)) != -1) {
                keyMeta = keyMeta or getKeyEventMeta(key.substring(previousIdx + 1, idx))
                previousIdx = idx
            }
            keyName = key.substring(previousIdx + 1)
            this.action = action
        }

        fun copy(): KeyboardShortcutSpec {
            return KeyboardShortcutSpec(contextTag, keyMeta, keyName, action)
        }

        val rawKey: String
            get() = getKeyEventKey(contextTag, keyMeta, keyName)

        fun getValueName(context: Context): String {
            return getActionLabel(context, action)
        }

        val isValid: Boolean
            get() = keyName != null

        fun toKeyString(): String {
            return metaToFriendlyString(keyMeta) + keyToFriendlyString(keyName)!!
        }

        override fun toString(): String {
            return "KeyboardShortcutSpec{" +
                    "action='" + action + '\'' +
                    ", contextTag='" + contextTag + '\'' +
                    ", keyMeta=" + keyMeta +
                    ", keyName='" + keyName + '\'' +
                    '}'
        }

        private fun keyToFriendlyString(keyName: String?): String? {
            if (keyName == null) return null
            val upperName = keyName.toUpperCase(Locale.US)
            val keyCode = KeyEvent.keyCodeFromString(KEYCODE_STRING_PREFIX + upperName)
            if (keyCode == KeyEvent.KEYCODE_UNKNOWN) return upperName
            if (keyCode == KeyEvent.KEYCODE_DEL) return "Backspace"
            if (keyCode == KeyEvent.KEYCODE_FORWARD_DEL) return "Delete"
            if (keyCode == KeyEvent.KEYCODE_SPACE) return "Space"
            val displayLabel = KeyEvent(KeyEvent.ACTION_DOWN, keyCode).displayLabel
            if (displayLabel.toInt() == 0) return keyName.toUpperCase(Locale.US)
            return displayLabel.toString()
        }
    }

    companion object {

        val MODIFIER_FLAG_CTRL = 0x00000001
        val MODIFIER_FLAG_SHIFT = 0x00000002
        val MODIFIER_FLAG_ALT = 0x00000004
        val MODIFIER_FLAG_META = 0x00000008
        val MODIFIER_FLAG_FN = 0x000000010

        private val KEYCODE_STRING_PREFIX = "KEYCODE_"

        private val sActionLabelMap = HashMap<String, Int>()
        private val sMetaNameMap = SparseArrayCompat<String>()

        init {
            sActionLabelMap.put(ACTION_COMPOSE, R.string.compose)
            sActionLabelMap.put(ACTION_SEARCH, R.string.search)
            sActionLabelMap.put(ACTION_MESSAGE, R.string.new_direct_message)
            sActionLabelMap.put(ACTION_HOME_ACCOUNTS_DASHBOARD, R.string.open_accounts_dashboard)
            sActionLabelMap.put(ACTION_STATUS_REPLY, R.string.reply)
            sActionLabelMap.put(ACTION_STATUS_RETWEET, R.string.retweet)
            sActionLabelMap.put(ACTION_STATUS_FAVORITE, R.string.like)
            sActionLabelMap.put(ACTION_NAVIGATION_PREVIOUS, R.string.previous_item)
            sActionLabelMap.put(ACTION_NAVIGATION_NEXT, R.string.next_item)
            sActionLabelMap.put(ACTION_NAVIGATION_PAGE_DOWN, R.string.page_down)
            sActionLabelMap.put(ACTION_NAVIGATION_PAGE_UP, R.string.page_up)
            sActionLabelMap.put(ACTION_NAVIGATION_TOP, R.string.jump_to_top)
            sActionLabelMap.put(ACTION_NAVIGATION_REFRESH, R.string.refresh)
            sActionLabelMap.put(ACTION_NAVIGATION_PREVIOUS_TAB, R.string.previous_tab)
            sActionLabelMap.put(ACTION_NAVIGATION_NEXT_TAB, R.string.next_tab)
            sActionLabelMap.put(ACTION_NAVIGATION_BACK, R.string.keyboard_shortcut_back)

            sMetaNameMap.put(KeyEvent.META_FUNCTION_ON, "fn")
            sMetaNameMap.put(KeyEvent.META_META_ON, "meta")
            sMetaNameMap.put(KeyEvent.META_CTRL_ON, "ctrl")
            sMetaNameMap.put(KeyEvent.META_ALT_ON, "alt")
            sMetaNameMap.put(KeyEvent.META_SHIFT_ON, "shift")
        }

        fun getActionLabel(context: Context, action: String): String? {
            if (!sActionLabelMap.containsKey(action)) return null
            val labelRes = sActionLabelMap[action]
            return context.getString(labelRes)
        }

        fun getKeyEventKey(contextTag: String, keyCode: Int, event: KeyEvent, metaState: Int): String? {
            if (!isValidForHotkey(keyCode, event)) return null
            val keyNameBuilder = StringBuilder()
            if (!TextUtils.isEmpty(contextTag)) {
                keyNameBuilder.append(contextTag)
                keyNameBuilder.append(".")
            }
            val normalizedMetaState = KeyEvent.normalizeMetaState(metaState or event.metaState)

            var i = 0
            val j = sMetaNameMap.size()
            while (i < j) {
                if (sMetaNameMap.keyAt(i) and normalizedMetaState != 0) {
                    keyNameBuilder.append(sMetaNameMap.valueAt(i))
                    keyNameBuilder.append("+")
                }
                i++
            }
            val keyCodeString = KeyEvent.keyCodeToString(keyCode)
            if (keyCodeString.startsWith(KEYCODE_STRING_PREFIX)) {
                keyNameBuilder.append(keyCodeString.substring(KEYCODE_STRING_PREFIX.length).toLowerCase(Locale.US))
            }
            return keyNameBuilder.toString()
        }

        fun getKeyEventKey(contextTag: String, metaState: Int, keyName: String): String {
            val keyNameBuilder = StringBuilder()
            if (!TextUtils.isEmpty(contextTag)) {
                keyNameBuilder.append(contextTag)
                keyNameBuilder.append(".")
            }

            var i = 0
            val j = sMetaNameMap.size()
            while (i < j) {
                if (sMetaNameMap.keyAt(i) and metaState != 0) {
                    keyNameBuilder.append(sMetaNameMap.valueAt(i))
                    keyNameBuilder.append("+")
                }
                i++
            }
            keyNameBuilder.append(keyName)
            return keyNameBuilder.toString()
        }

        fun getKeyEventMeta(name: String): Int {
            var i = 0
            val j = sMetaNameMap.size()
            while (i < j) {
                if (sMetaNameMap.valueAt(i).equals(name, ignoreCase = true)) return sMetaNameMap.keyAt(i)
                i++
            }
            return 0
        }

        fun getKeyboardShortcutSpec(contextTag: String, keyCode: Int, event: KeyEvent, metaState: Int): KeyboardShortcutSpec? {
            if (!isValidForHotkey(keyCode, event)) return null
            var metaStateNormalized = 0
            var i = 0
            val j = sMetaNameMap.size()
            while (i < j) {
                if (sMetaNameMap.keyAt(i) and metaState != 0) {
                    metaStateNormalized = metaStateNormalized or sMetaNameMap.keyAt(i)
                }
                i++
            }
            val keyCodeString = KeyEvent.keyCodeToString(keyCode)
            if (keyCodeString.startsWith(KEYCODE_STRING_PREFIX)) {
                val keyName = keyCodeString.substring(KEYCODE_STRING_PREFIX.length).toLowerCase(Locale.US)
                return KeyboardShortcutSpec(contextTag, metaStateNormalized, keyName, null)
            }
            return null
        }

        fun isValidForHotkey(keyCode: Int, event: KeyEvent): Boolean {
            // These keys must use with modifiers
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_TAB -> {
                    if (event.hasNoModifiers()) return false
                }
            }
            return !isNavigationKey(keyCode) && !KeyEvent.isModifierKey(keyCode) && keyCode != KeyEvent.KEYCODE_UNKNOWN
        }

        private fun isNavigationKey(keyCode: Int): Boolean {
            return keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME
                    || keyCode == KeyEvent.KEYCODE_MENU
        }

        fun metaToFriendlyString(metaState: Int): String {
            val keyNameBuilder = StringBuilder()
            var i = 0
            val j = sMetaNameMap.size()
            while (i < j) {
                if (sMetaNameMap.keyAt(i) and metaState != 0) {
                    val value = sMetaNameMap.valueAt(i)
                    keyNameBuilder.append(value.substring(0, 1).toUpperCase(Locale.US))
                    keyNameBuilder.append(value.substring(1))
                    keyNameBuilder.append("+")
                }
                i++
            }
            return keyNameBuilder.toString()
        }

        fun getMetaStateForKeyCode(keyCode: Int): Int {
            when (keyCode) {
                KeyEvent.KEYCODE_CTRL_LEFT -> return KeyEvent.META_CTRL_LEFT_ON
                KeyEvent.KEYCODE_CTRL_RIGHT -> return KeyEvent.META_CTRL_RIGHT_ON
                KeyEvent.KEYCODE_SHIFT_LEFT -> return KeyEvent.META_SHIFT_LEFT_ON
                KeyEvent.KEYCODE_SHIFT_RIGHT -> return KeyEvent.META_SHIFT_RIGHT_ON
                KeyEvent.KEYCODE_ALT_LEFT -> return KeyEvent.META_ALT_LEFT_ON
                KeyEvent.KEYCODE_ALT_RIGHT -> return KeyEvent.META_ALT_RIGHT_ON
                KeyEvent.KEYCODE_META_LEFT -> return KeyEvent.META_META_LEFT_ON
                KeyEvent.KEYCODE_META_RIGHT -> return KeyEvent.META_META_RIGHT_ON
                KeyEvent.KEYCODE_FUNCTION -> return KeyEvent.META_FUNCTION_ON
            }
            return 0
        }
    }
}
