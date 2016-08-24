package org.mariotaku.twidere.util

import android.support.annotation.UiThread
import android.view.View

/**
 * Created by mariotaku on 16/1/23.
 */
object TwidereViewUtils {

    @UiThread
    fun hitView(x: Float, y: Float, view: View): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return x in location[0].toFloat()..(location[0] + view.width).toFloat()
                && y in location[1].toFloat()..(location[1] + view.height).toFloat()
    }
}
