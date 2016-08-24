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

import android.annotation.TargetApi
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.text.Spanned
import android.text.style.ImageSpan

object ClipboardUtils {

    fun setText(context: Context?, text: CharSequence): Boolean {
        if (context == null) return false
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.primaryClip = ClipData.newPlainText(text, text)
        return true
    }

    fun getImageUrl(context: Context): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) return null
        return ClipboardUtilsAPI16.getImageUrl(context)
    }

    private object ClipboardUtilsAPI16 {

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        fun getImageUrl(context: Context): String? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) return null
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val primaryClip = cm.primaryClip
            if (primaryClip.itemCount > 0) {
                val item = primaryClip.getItemAt(0)
                val styledText = item.coerceToStyledText(context)
                if (styledText is Spanned) {
                    val imageSpans = styledText.getSpans(0, styledText.length, ImageSpan::class.java)
                    if (imageSpans.size == 1) return imageSpans[0].source
                }
            }
            return null
        }
    }
}
