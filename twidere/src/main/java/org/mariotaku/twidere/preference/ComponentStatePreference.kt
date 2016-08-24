package org.mariotaku.twidere.preference

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.TypedArray
import android.support.v7.preference.SwitchPreferenceCompat
import android.util.AttributeSet


open class ComponentStatePreference : SwitchPreferenceCompat {

    private val mPackageManager: PackageManager
    private val mComponentName: ComponentName

    constructor(context: Context) : super(context) {
        mPackageManager = context.packageManager
        mComponentName = getComponentName(context, null)
        setDefaultValue(isComponentEnabled)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        mPackageManager = context.packageManager
        mComponentName = getComponentName(context, attrs)
        setDefaultValue(isComponentEnabled)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        mPackageManager = context.packageManager
        mComponentName = getComponentName(context, attrs)
        setDefaultValue(isComponentEnabled)
    }

    override fun shouldDisableDependents(): Boolean {
        return disableDependentsState || !isComponentAvailable
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Boolean {
        return isComponentEnabled
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        isChecked = getPersistedBoolean(true)
    }

    protected open fun getComponentName(context: Context, attrs: AttributeSet?): ComponentName {
        val a = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.name))
        val name = a.getString(0)
        a.recycle()
        if (name == null) throw NullPointerException()
        return ComponentName(context.packageName, name)
    }

    protected open val isComponentAvailable: Boolean
        get() = true

    override fun shouldPersist(): Boolean {
        return true
    }

    override fun persistBoolean(value: Boolean): Boolean {
        val newState = if (value)
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        mPackageManager.setComponentEnabledSetting(mComponentName, newState, PackageManager.DONT_KILL_APP)
        return true
    }

    override fun getPersistedBoolean(defaultReturnValue: Boolean): Boolean {
        return isComponentEnabled
    }

    private // Seems this will thrown on older devices
    val isComponentEnabled: Boolean
        @SuppressLint("InlinedApi")
        get() {
            try {
                val state = mPackageManager.getComponentEnabledSetting(mComponentName)
                return state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        && state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                        && state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
            } catch (e: NullPointerException) {
                return false
            }

        }


}
