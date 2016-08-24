/*
 * Twidere - Twitter client for Android
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

package org.mariotaku.twidere.view

import android.content.Context
import android.support.annotation.IntDef
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import org.apache.commons.lang3.ObjectUtils
import org.mariotaku.twidere.Constants
import org.mariotaku.twidere.R
import org.mariotaku.twidere.constant.SharedPreferenceConstants.*
import org.mariotaku.twidere.model.ParcelableMedia
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.model.util.ParcelableMediaUtils
import org.mariotaku.twidere.util.MediaLoaderWrapper
import org.mariotaku.twidere.util.MediaLoadingHandler
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.ref.WeakReference

/**
 * Dynamic layout for media preview
 * Created by mariotaku on 14/12/17.
 */
class CardMediaContainer @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ViewGroup(context, attrs, defStyleAttr), Constants {

    private val horizontalSpacing: Int
    private val verticalSpacing: Int
    private var tempIndices: IntArray? = null
    private var mediaPreviewStyle: Int = 0

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.CardMediaContainer)
        horizontalSpacing = a.getDimensionPixelSize(R.styleable.CardMediaContainer_android_horizontalSpacing, 0)
        verticalSpacing = a.getDimensionPixelSize(R.styleable.CardMediaContainer_android_verticalSpacing, 0)
        a.recycle()
    }


    fun displayMedia(vararg imageRes: Int) {
        var i = 0
        val j = childCount
        val k = imageRes.size
        while (i < j) {
            val child = getChildAt(i)
            val imageView = child.findViewById(R.id.mediaPreview) as ImageView
            val progress = child.findViewById(R.id.media_preview_progress)
            progress.visibility = View.GONE
            if (i < k) {
                imageView.setImageResource(imageRes[i])
            } else {
                imageView.setImageDrawable(null)
                child.visibility = View.GONE
            }
            i++
        }
    }

    fun displayMedia(mediaArray: Array<ParcelableMedia>?,
                     loader: MediaLoaderWrapper,
                     accountId: UserKey, extraId: Long,
                     mediaClickListener: OnMediaClickListener,
                     loadingHandler: MediaLoadingHandler) {
        displayMedia(loader, mediaClickListener, loadingHandler, mediaArray, accountId, extraId, false)
    }

    fun displayMedia(loader: MediaLoaderWrapper, mediaClickListener: OnMediaClickListener, loadingHandler: MediaLoadingHandler, mediaArray: Array<ParcelableMedia>?,
                     accountId: UserKey, extraId: Long, withCredentials: Boolean) {
        if (mediaArray == null || mediaPreviewStyle == VALUE_MEDIA_PREVIEW_STYLE_CODE_NONE) {
            var i = 0
            val j = childCount
            while (i < j) {
                val child = getChildAt(i)
                child.tag = null
                child.visibility = View.GONE
                i++
            }
            return
        }
        val clickListener = ImageGridClickListener(mediaClickListener,
                accountId, extraId)
        var i = 0
        val j = childCount
        val k = mediaArray.size
        while (i < j) {
            val child = getChildAt(i)
            child.setOnClickListener(clickListener)
            val imageView = child.findViewById(R.id.mediaPreview) as ImageView
            when (mediaPreviewStyle) {
                VALUE_MEDIA_PREVIEW_STYLE_CODE_CROP -> {
                    imageView.scaleType = ScaleType.CENTER_CROP
                }
                VALUE_MEDIA_PREVIEW_STYLE_CODE_SCALE -> {
                    imageView.scaleType = ScaleType.FIT_CENTER
                }
            }
            if (i < k) {
                val media = mediaArray[i]
                val url = if (TextUtils.isEmpty(media.preview_url)) media.media_url else media.preview_url
                if (ObjectUtils.notEqual(url, imageView.tag) || imageView.drawable == null) {
                    if (withCredentials) {
                        loader.displayPreviewImageWithCredentials(imageView, url, accountId, loadingHandler)
                    } else {
                        loader.displayPreviewImage(imageView, url, loadingHandler)
                    }
                }
                imageView.tag = url
                if (imageView is MediaPreviewImageView) {
                    imageView.setHasPlayIcon(ParcelableMediaUtils.hasPlayIcon(media.type))
                }
                if (TextUtils.isEmpty(media.alt_text)) {
                    child.contentDescription = context.getString(R.string.media)
                } else {
                    child.contentDescription = media.alt_text
                }
                child.tag = media
                child.visibility = View.VISIBLE
            } else {
                loader.cancelDisplayTask(imageView)
                imageView.tag = null
                child.visibility = View.GONE
            }
            i++
        }
    }

    fun setStyle(style: Int) {
        mediaPreviewStyle = style
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val childIndices = createChildIndices()
        val childCount = getChildIndicesInLayout(this, childIndices)
        if (childCount > 0) {
            if (childCount == 1) {
                layout1Media(childIndices)
            } else if (childCount == 3) {
                layout3Media(horizontalSpacing, verticalSpacing, childIndices)
            } else {
                layoutGridMedia(childCount, 2, horizontalSpacing, verticalSpacing, childIndices)
            }
        }
    }

    private fun measure1Media(contentWidth: Int, childIndices: IntArray) {
        val child = getChildAt(childIndices[0])
        val childHeight = Math.round(contentWidth * WIDTH_HEIGHT_RATIO)
        val widthSpec = View.MeasureSpec.makeMeasureSpec(contentWidth, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(childHeight, View.MeasureSpec.EXACTLY)
        child.measure(widthSpec, heightSpec)
    }

    private fun layout1Media(childIndices: IntArray) {
        val child = getChildAt(childIndices[0])
        val left = paddingLeft
        val top = paddingTop
        val right = left + child.measuredWidth
        val bottom = top + child.measuredHeight
        child.layout(left, top, right, bottom)
    }

    private fun measureGridMedia(childCount: Int, columnCount: Int, contentWidth: Int,
                                 widthHeightRatio: Float, horizontalSpacing: Int, verticalSpacing: Int,
                                 childIndices: IntArray): Int {
        val childWidth = (contentWidth - horizontalSpacing * (columnCount - 1)) / columnCount
        val childHeight = Math.round(childWidth * widthHeightRatio)
        val widthSpec = View.MeasureSpec.makeMeasureSpec(childWidth, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(childHeight, View.MeasureSpec.EXACTLY)
        for (i in 0..childCount - 1) {
            getChildAt(childIndices[i]).measure(widthSpec, heightSpec)
        }
        val rowsCount = Math.ceil(childCount / columnCount.toDouble()).toInt()
        return rowsCount * childHeight + (rowsCount - 1) * verticalSpacing
    }

    private fun layoutGridMedia(childCount: Int, columnCount: Int, horizontalSpacing: Int,
                                verticalSpacing: Int, childIndices: IntArray) {
        val initialLeft = paddingLeft
        var left = initialLeft
        var top = paddingTop
        for (i in 0..childCount - 1) {
            val colIdx = i % columnCount
            val child = getChildAt(childIndices[i])
            child.layout(left, top, left + child.measuredWidth, top + child.measuredHeight)
            if (colIdx == columnCount - 1) {
                // Last item in this row, set top of next row to last view bottom + verticalSpacing
                top = child.bottom + verticalSpacing
                // And reset left to initial left
                left = initialLeft
            } else {
                // The left of next item is right + horizontalSpacing of previous item
                left = child.right + horizontalSpacing
            }
        }
    }

    private fun measure3Media(contentWidth: Int, horizontalSpacing: Int, childIndices: IntArray) {
        val child0 = getChildAt(childIndices[0])
        val child1 = getChildAt(childIndices[1])
        val child2 = getChildAt(childIndices[2])
        val childWidth = (contentWidth - horizontalSpacing) / 2
        val sizeSpec = View.MeasureSpec.makeMeasureSpec(childWidth, View.MeasureSpec.EXACTLY)
        child0.measure(sizeSpec, sizeSpec)
        val childRightHeight = Math.round((childWidth - horizontalSpacing).toFloat()) / 2
        val heightSpec = View.MeasureSpec.makeMeasureSpec(childRightHeight, View.MeasureSpec.EXACTLY)
        child1.measure(sizeSpec, heightSpec)
        child2.measure(sizeSpec, heightSpec)
    }

    private fun layout3Media(horizontalSpacing: Int, verticalSpacing: Int, childIndices: IntArray) {
        val left = paddingLeft
        val top = paddingTop
        val child0 = getChildAt(childIndices[0])
        val child1 = getChildAt(childIndices[1])
        val child2 = getChildAt(childIndices[2])
        child0.layout(left, top, left + child0.measuredWidth, top + child0.measuredHeight)
        val rightColLeft = child0.right + horizontalSpacing
        child1.layout(rightColLeft, top, rightColLeft + child1.measuredWidth,
                top + child1.measuredHeight)
        val child2Top = child1.bottom + verticalSpacing
        child2.layout(rightColLeft, child2Top, rightColLeft + child2.measuredWidth,
                child2Top + child2.measuredHeight)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = View.resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        val contentWidth = measuredWidth - paddingLeft - paddingRight
        val childIndices = createChildIndices()
        val childCount = getChildIndicesInLayout(this, childIndices)
        var heightSum = 0
        if (childCount > 0) {
            if (childCount == 1) {
                measure1Media(contentWidth, childIndices)
                heightSum = Math.round(contentWidth * WIDTH_HEIGHT_RATIO)
            } else if (childCount == 2) {
                measureGridMedia(childCount, 2, contentWidth, 1f, horizontalSpacing, verticalSpacing,
                        childIndices)
                heightSum = Math.round(contentWidth * WIDTH_HEIGHT_RATIO)
            } else if (childCount == 3) {
                measure3Media(contentWidth, horizontalSpacing, childIndices)
                heightSum = Math.round(contentWidth * WIDTH_HEIGHT_RATIO)
            } else {
                heightSum = measureGridMedia(childCount, 2, contentWidth, WIDTH_HEIGHT_RATIO,
                        horizontalSpacing, verticalSpacing, childIndices)
            }
        }
        val height = heightSum + paddingTop + paddingBottom
        setMeasuredDimension(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
    }

    private fun createChildIndices(): IntArray {
        if (tempIndices == null || tempIndices!!.size < childCount) {
            tempIndices = IntArray(childCount)
            return tempIndices!!
        }
        return tempIndices!!
    }

    interface OnMediaClickListener {
        fun onMediaClick(view: View, media: ParcelableMedia, accountKey: UserKey, id: Long)
    }

    private class ImageGridClickListener internal constructor(listener: OnMediaClickListener, private val mAccountKey: UserKey,
                                                              private val mExtraId: Long) : View.OnClickListener {
        private val mListenerRef: WeakReference<OnMediaClickListener>

        init {
            mListenerRef = WeakReference(listener)
        }

        override fun onClick(v: View) {
            val listener = mListenerRef.get() ?: return
            listener.onMediaClick(v, v.tag as ParcelableMedia, mAccountKey, mExtraId)
        }

    }

    @IntDef(VALUE_MEDIA_PREVIEW_STYLE_CODE_SCALE.toLong(), VALUE_MEDIA_PREVIEW_STYLE_CODE_CROP.toLong())
    @Retention(RetentionPolicy.SOURCE)
    annotation class PreviewStyle

    companion object {

        private val WIDTH_HEIGHT_RATIO = 0.5f

        private fun getChildIndicesInLayout(viewGroup: ViewGroup, indices: IntArray): Int {
            val childCount = viewGroup.childCount
            var indicesCount = 0
            for (i in 0..childCount - 1) {
                if (viewGroup.getChildAt(i).visibility != View.GONE) {
                    indices[indicesCount++] = i
                }
            }
            return indicesCount
        }
    }
}
