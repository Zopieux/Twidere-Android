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

package org.mariotaku.twidere.preference

import android.content.Context
import android.util.AttributeSet
import android.util.Log

import org.mariotaku.twidere.R

import java.io.File

import org.mariotaku.twidere.TwidereConstants.LOGTAG

class ClearCachePreference @JvmOverloads constructor(context: Context, attrs: AttributeSet = null, defStyle: Int = R.attr.preferenceStyle) : AsyncTaskPreference(context, attrs, defStyle) {

    override fun doInBackground() {
        val context = context ?: return
        val externalCacheDir = context.externalCacheDir
        if (externalCacheDir != null) {
            val files = externalCacheDir.listFiles()
            if (files != null) {
                for (file in files) {
                    deleteRecursive(file)
                }
            }
        }
        val internalCacheDir = context.cacheDir
        if (internalCacheDir != null) {
            val files = internalCacheDir.listFiles()
            if (files != null) {
                for (file in files) {
                    deleteRecursive(file)
                }
            }
        }
    }

    private fun deleteRecursive(f: File) {
        if (f.isDirectory) {
            val files = f.listFiles() ?: return
            for (c in files) {
                deleteRecursive(c)
            }
        }
        if (!f.delete()) {
            Log.w(LOGTAG, String.format("Unable to delete %s", f))
        }
    }

}
