package org.mariotaku.twidere.preference

import android.content.Context
import android.graphics.PorterDuff.Mode
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceViewHolder
import android.util.AttributeSet
import android.widget.ImageView

import org.mariotaku.twidere.R
import org.mariotaku.twidere.util.ThemeUtils

/**
 * Created by mariotaku on 14-7-28.
 */
class ForegroundColorIconPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet = null, defStyle: Int = R.attr.preferenceStyle) : Preference(context, attrs, defStyle) {

    override fun onBindViewHolder(view: PreferenceViewHolder) {
        super.onBindViewHolder(view)
        val fgColor = ThemeUtils.getThemeForegroundColor(context)
        (view.findViewById(android.R.id.icon) as ImageView).setColorFilter(fgColor, Mode.SRC_ATOP)
    }
}
