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

package org.mariotaku.twidere.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PorterDuff.Mode
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.annotation.ColorInt
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.ColorUtils
import android.support.v7.app.AppCompatDelegate
import android.support.v7.view.menu.ActionMenuItemView
import android.support.v7.widget.ActionMenuView
import android.support.v7.widget.Toolbar
import android.support.v7.widget.TwidereToolbar
import android.text.TextUtils
import android.util.TypedValue
import android.view.*
import android.view.View.OnLongClickListener
import android.widget.FrameLayout
import com.afollestad.appthemeengine.Config
import com.afollestad.appthemeengine.util.ATEUtil
import org.apache.commons.lang3.ArrayUtils
import org.mariotaku.twidere.R
import org.mariotaku.twidere.TwidereConstants.SHARED_PREFERENCES_NAME
import org.mariotaku.twidere.constant.SharedPreferenceConstants.*
import org.mariotaku.twidere.graphic.ActionIconDrawable
import org.mariotaku.twidere.graphic.WindowBackgroundDrawable
import org.mariotaku.twidere.graphic.iface.DoNotWrapDrawable
import org.mariotaku.twidere.preference.ThemeBackgroundPreference
import org.mariotaku.twidere.util.menu.TwidereMenuInfo
import org.mariotaku.twidere.util.support.ViewSupport

object ThemeUtils {


    val ACCENT_COLOR_THRESHOLD = 192
    val DARK_COLOR_THRESHOLD = 128

    val ATTRS_TEXT_COLOR_PRIMARY = intArrayOf(android.R.attr.textColorPrimary)


    fun applyColorFilterToMenuIcon(menu: Menu, @ColorInt color: Int,
                                   @ColorInt popupColor: Int,
                                   @ColorInt highlightColor: Int, mode: Mode,
                                   vararg excludedGroups: Int) {
        var i = 0
        val j = menu.size()
        while (i < j) {
            val item = menu.getItem(i)
            val icon = item.icon
            val info = item.menuInfo
            if (icon != null && !ArrayUtils.contains(excludedGroups, item.groupId)) {
                icon.mutate()
                if (info is TwidereMenuInfo) {
                    val stateColor = if (info.isHighlight) info.getHighlightColor(highlightColor) else color
                    if (stateColor != 0) {
                        icon.setColorFilter(stateColor, mode)
                    }
                } else if (color != 0) {
                    icon.setColorFilter(color, mode)
                }
            }
            if (item.hasSubMenu()) {
                // SubMenu item is always in popup
                applyColorFilterToMenuIcon(item.subMenu, popupColor, popupColor, highlightColor, mode, *excludedGroups)
            }
            i++
        }
    }

    fun applyWindowBackground(context: Context, window: Window, option: String, alpha: Int) {
        if (isWindowFloating(context)) {
            window.setBackgroundDrawable(getWindowBackground(context))
        } else if (VALUE_THEME_BACKGROUND_TRANSPARENT == option) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            window.setBackgroundDrawable(getWindowBackgroundFromThemeApplyAlpha(context, alpha))
        } else if (VALUE_THEME_BACKGROUND_SOLID == option) {
            window.setBackgroundDrawable(ColorDrawable(if (isLightTheme(context)) Color.WHITE else Color.BLACK))
        } else {
            window.setBackgroundDrawable(getWindowBackground(context))
        }
    }


    fun getCardBackgroundColor(context: Context, backgroundOption: String, themeAlpha: Int): Int {
        val a = context.obtainStyledAttributes(intArrayOf(R.attr.cardItemBackgroundColor))
        val color = a.getColor(0, Color.TRANSPARENT)
        a.recycle()
        if (isTransparentBackground(backgroundOption)) {
            return themeAlpha shl 24 or (0x00FFFFFF and color)
        } else if (isSolidBackground(backgroundOption)) {
            return TwidereColorUtils.getContrastYIQ(color, Color.WHITE, Color.BLACK)
        } else {
            return color
        }
    }


    fun getContrastColor(color: Int, darkColor: Int, lightColor: Int): Int {
        if (TwidereColorUtils.getYIQLuminance(color) <= ACCENT_COLOR_THRESHOLD) {
            //return light text color
            return lightColor
        }
        //return dark text color
        return darkColor
    }

    fun getContrastActionBarItemColor(context: Context): Int {
        return getColorFromAttribute(context, android.R.attr.colorForeground, 0)
    }

    fun getImageHighlightDrawable(context: Context): Drawable? {
        val d = getSelectableItemBackgroundDrawable(context)
        if (d != null) {
            d.alpha = 0x80
        }
        return d
    }

    fun getOptimalAccentColor(accentColor: Int, foregroundColor: Int): Int {
        val yiq = IntArray(3)
        TwidereColorUtils.colorToYIQ(foregroundColor, yiq)
        val foregroundColorY = yiq[0]
        TwidereColorUtils.colorToYIQ(accentColor, yiq)
        if (foregroundColorY < DARK_COLOR_THRESHOLD && yiq[0] <= ACCENT_COLOR_THRESHOLD) {
            return accentColor
        } else if (foregroundColorY > ACCENT_COLOR_THRESHOLD && yiq[0] > DARK_COLOR_THRESHOLD) {
            return accentColor
        }
        yiq[0] = yiq[0] + (foregroundColorY - yiq[0]) / 2
        return TwidereColorUtils.YIQToColor(Color.alpha(accentColor), yiq)
    }

    fun getResources(context: Context): Resources {
        return context.resources
    }

    fun getSelectableItemBackgroundDrawable(context: Context): Drawable? {
        val a = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
        try {
            return a.getDrawable(0)
        } finally {
            a.recycle()
        }
    }

    fun getSupportActionBarElevation(context: Context): Float {
        @SuppressWarnings("ConstantConditions")
        val a = context.obtainStyledAttributes(null, intArrayOf(R.attr.elevation), R.attr.actionBarStyle, 0)
        try {
            return a.getDimension(0, 0f)
        } finally {
            a.recycle()
        }
    }

    fun getColorFromAttribute(context: Context, attr: Int, def: Int): Int {
        val a = context.obtainStyledAttributes(intArrayOf(attr))
        try {
            return a.getColor(0, def)
        } finally {
            a.recycle()
        }
    }


    fun getTextColorPrimary(context: Context): Int {
        val a = context.obtainStyledAttributes(ATTRS_TEXT_COLOR_PRIMARY)
        try {
            return a.getColor(0, Color.TRANSPARENT)
        } finally {
            a.recycle()
        }
    }


    fun getThemeBackgroundColor(context: Context): Int {
        val a = context.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground))
        try {
            return a.getColor(0, 0)
        } finally {
            a.recycle()
        }
    }

    fun getThemeBackgroundColor(context: Context, themeRes: Int): Int {
        if (themeRes == 0) {
            return getThemeBackgroundColor(context)
        }
        @SuppressWarnings("ConstantConditions")
        val a = context.obtainStyledAttributes(null, intArrayOf(android.R.attr.colorBackground),
                0, themeRes)
        try {
            return a.getColor(0, 0)
        } finally {
            a.recycle()
        }
    }

    fun getThemeBackgroundOption(context: Context?): String {
        if (context == null) return VALUE_THEME_BACKGROUND_DEFAULT
        val pref = getSharedPreferencesWrapper(context)
        return pref.getString(KEY_THEME_BACKGROUND, VALUE_THEME_BACKGROUND_DEFAULT)!!
    }

    fun getThemeFontFamily(pref: SharedPreferences): String {
        val fontFamily = pref.getString(KEY_THEME_FONT_FAMILY, VALUE_THEME_FONT_FAMILY_REGULAR)
        if (!TextUtils.isEmpty(fontFamily)) return fontFamily
        return VALUE_THEME_FONT_FAMILY_REGULAR
    }

    @JvmOverloads fun getThemeForegroundColor(context: Context, themeRes: Int = 0): Int {
        @SuppressWarnings("ConstantConditions")
        val value = TypedValue()
        val theme: Resources.Theme
        if (themeRes != 0) {
            theme = context.resources.newTheme()
            theme.applyStyle(themeRes, false)
        } else {
            theme = context.theme
        }
        if (!theme.resolveAttribute(android.R.attr.colorForeground, value, true)) {
            return 0
        }
        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            // windowBackground is a color
            return value.data
        }
        return 0
    }

    fun getTitleTextAppearance(context: Context): Int {
        val a = context.obtainStyledAttributes(null, intArrayOf(android.R.attr.titleTextStyle),
                android.R.attr.actionBarStyle, android.R.style.Widget_Holo_ActionBar)
        val textAppearance = a.getResourceId(0, android.R.style.TextAppearance_Holo)
        a.recycle()
        return textAppearance
    }

    fun getUserAccentColor(context: Context): Int {
        val pref = getSharedPreferencesWrapper(context)
        val def = ContextCompat.getColor(context, R.color.branding_color)
        return pref.getInt(KEY_THEME_COLOR, def)
    }

    fun getUserThemeBackgroundAlpha(context: Context?): Int {
        if (context == null) return DEFAULT_THEME_BACKGROUND_ALPHA
        val pref = getSharedPreferencesWrapper(context)
        return TwidereMathUtils.clamp(pref.getInt(KEY_THEME_BACKGROUND_ALPHA, DEFAULT_THEME_BACKGROUND_ALPHA),
                ThemeBackgroundPreference.MIN_ALPHA, ThemeBackgroundPreference.MAX_ALPHA)
    }


    fun getActionBarAlpha(alpha: Int): Int {
        val normalizedAlpha = TwidereMathUtils.clamp(alpha, 0, 0xFF)
        val delta = ThemeBackgroundPreference.MAX_ALPHA - normalizedAlpha
        return TwidereMathUtils.clamp(ThemeBackgroundPreference.MAX_ALPHA - delta / 2,
                ThemeBackgroundPreference.MIN_ALPHA, ThemeBackgroundPreference.MAX_ALPHA)
    }

    fun getUserTypeface(context: Context?, fontFamily: String, defTypeface: Typeface?): Typeface {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
            return Typeface.DEFAULT
        val fontStyle = if (defTypeface != null) defTypeface.style else Typeface.NORMAL
        val tf = Typeface.create(fontFamily, fontStyle)
        if (tf != null) return tf
        return Typeface.create(Typeface.DEFAULT, fontStyle)
    }

    fun getWindowBackground(context: Context): Drawable {
        val a = context.obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
        try {
            return a.getDrawable(0)
        } finally {
            a.recycle()
        }
    }

    fun getColorBackground(context: Context): Int {
        val a = context.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground))
        try {
            return a.getColor(0, Color.TRANSPARENT)
        } finally {
            a.recycle()
        }
    }

    fun getWindowBackgroundFromThemeApplyAlpha(context: Context, alpha: Int): Drawable {
        var backgroundColor: Int
        val d = getWindowBackground(context)
        if (d is ColorDrawable) {
            backgroundColor = d.color
        } else {
            backgroundColor = getColorBackground(context)
        }
        backgroundColor = backgroundColor and 0x00FFFFFF
        backgroundColor = backgroundColor or (TwidereMathUtils.clamp(alpha, ThemeBackgroundPreference.MIN_ALPHA,
                ThemeBackgroundPreference.MAX_ALPHA) shl 24)
        return WindowBackgroundDrawable(backgroundColor)
    }

    fun isLightTheme(context: Context): Boolean {
        val a = context.obtainStyledAttributes(intArrayOf(R.attr.isLightTheme))
        try {
            return a.getBoolean(0, false)
        } finally {
            a.recycle()
        }
    }

    fun isSolidBackground(option: String): Boolean {
        return VALUE_THEME_BACKGROUND_SOLID == option
    }

    fun isTransparentBackground(context: Context): Boolean {
        return isTransparentBackground(getThemeBackgroundOption(context))
    }

    fun isTransparentBackground(option: String): Boolean {
        return VALUE_THEME_BACKGROUND_TRANSPARENT == option
    }

    fun isWindowFloating(context: Context): Boolean {
        val a = context.obtainStyledAttributes(intArrayOf(android.R.attr.windowIsFloating))
        try {
            return a.getBoolean(0, false)
        } finally {
            a.recycle()
        }
    }

    fun resetCheatSheet(menuView: ActionMenuView) {
        val listener = OnLongClickListener { v ->
            if ((v as ActionMenuItemView).hasText()) return@OnLongClickListener false
            val menuItem = v.itemData
            Utils.showMenuItemToast(v, menuItem.title, true)
            true
        }
        loop@for (i in 0 until menuView.childCount) {
            val child = menuView.getChildAt(i)
            if (child !is ActionMenuItemView) {
                continue@loop
            }
            val menuItem = child.itemData
            if (menuItem.hasSubMenu()) {
                continue@loop
            }
            child.setOnLongClickListener(listener)
        }
    }

    fun setActionBarOverflowColor(toolbar: Toolbar?, itemColor: Int) {
        if (toolbar == null) return
        if (toolbar is TwidereToolbar) {
            toolbar.setItemColor(itemColor)
        }
        val overflowIcon = toolbar.overflowIcon
        if (overflowIcon != null) {
            overflowIcon.setColorFilter(itemColor, Mode.SRC_ATOP)
            toolbar.overflowIcon = overflowIcon
        }
    }

    fun setCompatContentViewOverlay(window: Window, overlay: Drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) return
        var contentLayout: View? = window.findViewById(android.support.v7.appcompat.R.id.action_bar_activity_content)
        if (contentLayout == null) {
            contentLayout = window.findViewById(android.R.id.content)
        }
        if (contentLayout is FrameLayout) {
            ViewSupport.setForeground(contentLayout, overlay)
        }
    }

    fun wrapMenuIcon(menu: Menu, itemColor: Int, subItemColor: Int, vararg excludeGroups: Int) {
        var i = 0
        val j = menu.size()
        while (i < j) {
            val item = menu.getItem(i)
            wrapMenuItemIcon(item, itemColor, *excludeGroups)
            if (item.hasSubMenu()) {
                wrapMenuIcon(item.subMenu, subItemColor, subItemColor, *excludeGroups)
            }
            i++
        }
    }

    fun wrapMenuIconDefaultColor(view: ActionMenuView, vararg excludeGroups: Int) {
        val context = view.context
        val colorDark = ContextCompat.getColor(context, R.color.action_icon_dark)
        val colorLight = ContextCompat.getColor(context, R.color.action_icon_light)
        wrapMenuIcon(view, colorDark, colorLight, *excludeGroups)
    }


    fun getActionIconColor(context: Context): Int {
        val colorDark = ContextCompat.getColor(context, R.color.action_icon_dark)
        val colorLight = ContextCompat.getColor(context, R.color.action_icon_light)
        val itemBackgroundColor = getThemeBackgroundColor(context)
        return TwidereColorUtils.getContrastYIQ(itemBackgroundColor, colorDark, colorLight)
    }

    fun getActionIconColor(context: Context, backgroundColor: Int): Int {
        val colorDark = ContextCompat.getColor(context, R.color.action_icon_dark)
        val colorLight = ContextCompat.getColor(context, R.color.action_icon_light)
        return if (isLightColor(backgroundColor)) colorDark else colorLight
    }

    fun setLightStatusBar(window: Window, lightStatusBar: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val decorView = window.decorView

        val systemUiVisibility = decorView.systemUiVisibility
        if (lightStatusBar) {
            decorView.systemUiVisibility = systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            decorView.systemUiVisibility = systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
    }

    fun wrapMenuIcon(view: ActionMenuView, colorDark: Int, colorLight: Int, vararg excludeGroups: Int) {
        val context = view.context
        val itemBackgroundColor = getThemeBackgroundColor(context)
        val popupItemBackgroundColor = getThemeBackgroundColor(context, view.popupTheme)
        val itemColor = TwidereColorUtils.getContrastYIQ(itemBackgroundColor, colorDark, colorLight)
        val popupItemColor = TwidereColorUtils.getContrastYIQ(popupItemBackgroundColor, colorDark, colorLight)
        val menu = view.menu
        val childCount = view.childCount
        var i = 0
        val j = menu.size()
        var k = 0
        while (i < j) {
            val item = menu.getItem(i)
            wrapMenuItemIcon(item, itemColor, *excludeGroups)
            if (item.hasSubMenu()) {
                wrapMenuIcon(item.subMenu, popupItemColor, popupItemColor, *excludeGroups)
            }
            if (item.isVisible) {
                k++
            }
            i++
        }
    }


    fun wrapMenuItemIcon(item: MenuItem, itemColor: Int, vararg excludeGroups: Int) {
        if (ArrayUtils.contains(excludeGroups, item.groupId)) return
        val icon = item.icon
        if (icon == null || icon is DoNotWrapDrawable) return
        if (icon is ActionIconDrawable) {
            icon.defaultColor = itemColor
            item.icon = icon
            return
        }
        icon.mutate()
        val callback = icon.callback
        val newIcon = ActionIconDrawable(icon, itemColor)
        newIcon.callback = callback
        item.icon = newIcon
    }

    fun wrapToolbarMenuIcon(view: ActionMenuView?, itemColor: Int, popupItemColor: Int, vararg excludeGroups: Int) {
        if (view == null) return
        val menu = view.menu
        val childCount = view.childCount
        var i = 0
        val j = menu.size()
        var k = 0
        while (i < j) {
            val item = menu.getItem(i)
            wrapMenuItemIcon(item, itemColor, *excludeGroups)
            if (item.hasSubMenu()) {
                wrapMenuIcon(item.subMenu, popupItemColor, popupItemColor, *excludeGroups)
            }
            if (item.isVisible) {
                k++
            }
            i++
        }
    }

    private fun getSharedPreferencesWrapper(context: Context): SharedPreferencesWrapper {
        val appContext = context.applicationContext
        return SharedPreferencesWrapper.getInstance(appContext, SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE)
    }

    fun getActionBarHeight(context: Context): Int {
        val tv = TypedValue()
        val theme = context.theme
        val attr = R.attr.actionBarSize
        if (theme.resolveAttribute(attr, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, context.resources.displayMetrics)
        }
        return 0
    }

    fun applyToolbarItemColor(context: Context, toolbar: Toolbar?, toolbarColor: Int) {
        if (toolbar == null) {
            return
        }
        val contrastForegroundColor = getColorDependent(toolbarColor)
        toolbar.setTitleTextColor(contrastForegroundColor)
        toolbar.setSubtitleTextColor(contrastForegroundColor)
        val popupItemColor: Int
        val popupTheme = toolbar.popupTheme
        if (popupTheme != 0) {
            popupItemColor = getThemeForegroundColor(context, popupTheme)
        } else {
            popupItemColor = getThemeForegroundColor(context)
        }
        val navigationIcon = toolbar.navigationIcon
        if (navigationIcon != null) {
            navigationIcon.setColorFilter(contrastForegroundColor, Mode.SRC_ATOP)
            toolbar.navigationIcon = navigationIcon
        }
        getThemeForegroundColor(context)
        setActionBarOverflowColor(toolbar, contrastForegroundColor)
        wrapToolbarMenuIcon(ViewSupport.findViewByType(toolbar, ActionMenuView::class.java),
                contrastForegroundColor, popupItemColor)
        if (toolbar is TwidereToolbar) {
            toolbar.setItemColor(contrastForegroundColor)
        }
    }

    fun getLocalNightMode(preferences: SharedPreferences): Int {
        when (Utils.getNonEmptyString(preferences, KEY_THEME, VALUE_THEME_NAME_LIGHT)) {
            VALUE_THEME_NAME_DARK -> {
                return AppCompatDelegate.MODE_NIGHT_YES
            }
            VALUE_THEME_NAME_AUTO -> {
                return AppCompatDelegate.MODE_NIGHT_AUTO
            }
        }
        return AppCompatDelegate.MODE_NIGHT_NO
    }

    fun applyDayNight(preferences: SharedPreferences, delegate: AppCompatDelegate) {
        when (getLocalNightMode(preferences)) {
            AppCompatDelegate.MODE_NIGHT_AUTO -> {
                delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_AUTO)
            }
            AppCompatDelegate.MODE_NIGHT_YES -> {
                delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            else -> {
                delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    fun fixNightMode(resources: Resources, newConfig: Configuration) {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES)
            newConfig.uiMode = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv() or Configuration.UI_MODE_NIGHT_YES
    }

    fun getATEKey(context: Context): String {
        val value = TypedValue()
        if (!context.theme.resolveAttribute(R.attr.ateThemeKey, value, true)) {
            return "dark"
        }
        if (TextUtils.isEmpty(value.string)) return "dark"
        return value.string.toString()
    }

    fun getColorDependent(color: Int): Int {
        val isDark = !isLightColor(color)
        return if (isDark) Color.WHITE else Color.BLACK
    }


    @Config.LightStatusBarMode
    fun getLightStatusBarMode(statusBarColor: Int): Int {
        if (isLightColor(statusBarColor)) {
            return Config.LIGHT_STATUS_BAR_ON
        }
        return Config.LIGHT_STATUS_BAR_OFF
    }

    @Config.LightToolbarMode
    fun getLightToolbarMode(themeColor: Int): Int {
        if (isLightColor(themeColor)) {
            return Config.LIGHT_TOOLBAR_ON
        }
        return Config.LIGHT_TOOLBAR_OFF
    }

    fun isLightColor(color: Int): Boolean {
        return ColorUtils.calculateLuminance(color) * 0xFF > ACCENT_COLOR_THRESHOLD
    }

    fun getOptimalAccentColor(themeColor: Int): Int {
        return getOptimalAccentColor(themeColor, getContrastColor(themeColor, Color.BLACK,
                Color.WHITE))
    }

    fun computeDarkColor(color: Int): Int {
        return ATEUtil.darkenColor(color)
    }
}
