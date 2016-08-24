/*
 * Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
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

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.TypeEvaluator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View

/**
 * Static utility methods for Transitions.

 * @hide
 */
object TransitionUtils {
    private val MAX_IMAGE_SIZE = 1024 * 1024

    internal fun mergeAnimators(animator1: Animator?, animator2: Animator?): Animator? {
        if (animator1 == null) {
            return animator2
        } else if (animator2 == null) {
            return animator1
        } else {
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(animator1, animator2)
            return animatorSet
        }
    }

    /**
     * Get a copy of bitmap of given drawable, return null if intrinsic size is zero
     */
    fun createDrawableBitmap(drawable: Drawable): Bitmap? {
        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight
        if (width <= 0 || height <= 0) {
            return null
        }
        val scale = Math.min(1f, MAX_IMAGE_SIZE.toFloat() / (width * height))
        if (drawable is BitmapDrawable && scale == 1f) {
            // return same bitmap if scale down not needed
            return drawable.bitmap
        }
        val bitmapWidth = (width * scale).toInt()
        val bitmapHeight = (height * scale).toInt()
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val existingBounds = drawable.bounds
        val left = existingBounds.left
        val top = existingBounds.top
        val right = existingBounds.right
        val bottom = existingBounds.bottom
        drawable.setBounds(0, 0, bitmapWidth, bitmapHeight)
        drawable.draw(canvas)
        drawable.setBounds(left, top, right, bottom)
        return bitmap
    }

    /**
     * Creates a Bitmap of the given view, using the Matrix matrix to transform to the local
     * coordinates. `matrix` will be modified during the bitmap creation.
     *
     *
     *
     * If the bitmap is large, it will be scaled uniformly down to at most 1MB size.

     * @param view   The view to create a bitmap for.
     * *
     * @param matrix The matrix converting the view local coordinates to the coordinates that
     * *               the bitmap will be displayed in. `matrix` will be modified before
     * *               returning.
     * *
     * @param bounds The bounds of the bitmap in the destination coordinate system (where the
     * *               view should be presented. Typically, this is matrix.mapRect(viewBounds);
     * *
     * @return A bitmap of the given view or null if bounds has no width or height.
     */
    fun createViewBitmap(view: View, matrix: Matrix, bounds: RectF): Bitmap? {
        var bitmap: Bitmap? = null
        var bitmapWidth = Math.round(bounds.width())
        var bitmapHeight = Math.round(bounds.height())
        if (bitmapWidth > 0 && bitmapHeight > 0) {
            val scale = Math.min(1f, MAX_IMAGE_SIZE.toFloat() / (bitmapWidth * bitmapHeight))
            bitmapWidth *= scale.toInt()
            bitmapHeight *= scale.toInt()
            matrix.postTranslate(-bounds.left, -bounds.top)
            matrix.postScale(scale, scale)
            bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap!!)
            canvas.concat(matrix)
            view.draw(canvas)
        }
        return bitmap
    }

    class MatrixEvaluator : TypeEvaluator<Matrix> {

        internal var mTempStartValues = FloatArray(9)

        internal var mTempEndValues = FloatArray(9)

        internal var mTempMatrix = Matrix()

        override fun evaluate(fraction: Float, startValue: Matrix, endValue: Matrix): Matrix {
            startValue.getValues(mTempStartValues)
            endValue.getValues(mTempEndValues)
            for (i in 0..8) {
                val diff = mTempEndValues[i] - mTempStartValues[i]
                mTempEndValues[i] = mTempStartValues[i] + fraction * diff
            }
            mTempMatrix.setValues(mTempEndValues)
            return mTempMatrix
        }
    }
}
