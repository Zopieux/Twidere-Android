package org.mariotaku.twidere.preference

import android.content.Context
import android.util.AttributeSet

/**
 * Created by mariotaku on 16/3/22.
 */
open class EntrySummaryListPreference : ThemedListPreference {
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    constructor(context: Context) : super(context) {
    }

    override fun getSummary(): CharSequence {
        return entry
    }
}
