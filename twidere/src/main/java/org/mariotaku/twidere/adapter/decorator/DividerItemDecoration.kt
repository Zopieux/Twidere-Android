/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mariotaku.twidere.adapter.decorator

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.support.v4.view.ViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.Adapter
import android.support.v7.widget.RecyclerView.State
import android.view.View

open class DividerItemDecoration(context: Context, orientation: Int) : RecyclerView.ItemDecoration() {

    private val mPaddingRect = Rect()
    private val mDivider: Drawable?

    private var mOrientation: Int = 0
    private var mPadding: Padding? = null
    private var decorationStart = -1
        set
    private var mDecorationEnd = -1
    private var mDecorationEndOffset: Int = 0

    init {
        val a = context.obtainStyledAttributes(ATTRS)
        mDivider = a.getDrawable(0)
        a.recycle()
        setOrientation(orientation)
    }

    fun setDecorationEnd(end: Int) {
        mDecorationEnd = end
        mDecorationEndOffset = -1
    }

    fun setDecorationEndOffset(endOffset: Int) {
        mDecorationEndOffset = endOffset
        mDecorationEnd = -1
    }

    fun setOrientation(orientation: Int) {
        if (orientation != HORIZONTAL_LIST && orientation != VERTICAL_LIST) {
            throw IllegalArgumentException("invalid orientation")
        }
        mOrientation = orientation
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: State?) {
        if (mOrientation == VERTICAL_LIST) {
            drawVertical(c, parent)
        } else {
            drawHorizontal(c, parent)
        }
    }

    fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        mPadding = object : Padding {
            override operator fun get(position: Int, rect: Rect): Boolean {
                rect.set(left, top, right, bottom)
                return true
            }
        }
    }

    fun setPadding(padding: Padding) {
        mPadding = padding
    }

    fun drawVertical(c: Canvas, parent: RecyclerView) {
        if (mDivider == null) return
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight

        val childCount = parent.childCount
        for (i in 0..childCount - 1) {
            val child = parent.getChildAt(i)
            val childPos = parent.getChildAdapterPosition(child)
            if (!isDividerEnabled(childPos)) continue
            val start = decorationStart
            val end = getDecorationEnd(parent)
            if (start >= 0 && childPos < start || end >= 0 && childPos > end) continue
            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin +
                    Math.round(ViewCompat.getTranslationY(child))
            val bottom = top + mDivider.intrinsicHeight
            if (mPadding != null && mPadding!![childPos, mPaddingRect]) {
                mDivider.setBounds(left + mPaddingRect.left, top + mPaddingRect.top, right - mPaddingRect.right,
                        bottom - mPaddingRect.bottom)
            } else {
                mDivider.setBounds(left, top, right, bottom)
            }
            mDivider.draw(c)
        }
    }

    protected open fun isDividerEnabled(childPos: Int): Boolean {
        return true
    }

    fun drawHorizontal(c: Canvas, parent: RecyclerView) {
        if (mDivider == null) return
        val top = parent.paddingTop
        val bottom = parent.height - parent.paddingBottom

        val childCount = parent.childCount
        for (i in 0..childCount - 1) {
            val child = parent.getChildAt(i)
            val childPos = parent.getChildAdapterPosition(child)
            val start = decorationStart
            val end = getDecorationEnd(parent)
            if (!isDividerEnabled(childPos)) continue
            if (start >= 0 && childPos < start || end >= 0 && childPos > end) continue
            val params = child.layoutParams as RecyclerView.LayoutParams
            val left = child.right + params.rightMargin +
                    Math.round(ViewCompat.getTranslationX(child))
            val right = left + mDivider.intrinsicHeight

            mDivider.setBounds(left + mPaddingRect.left, top + mPaddingRect.top, right - mPaddingRect.right,
                    bottom - mPaddingRect.bottom)
            mDivider.draw(c)
        }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State?) {
        if (mDivider == null) return
        val childPos = parent.getChildAdapterPosition(view)
        if (!isDividerEnabled(childPos)) return
        val start = decorationStart
        val end = getDecorationEnd(parent)
        if (start >= 0 && childPos < start || end >= 0 && childPos > end) {
            outRect.setEmpty()
            return
        }
        if (mOrientation == VERTICAL_LIST) {
            outRect.set(0, 0, 0, mDivider.intrinsicHeight)
        } else {
            outRect.set(0, 0, mDivider.intrinsicWidth, 0)
        }
    }

    private fun getDecorationEnd(parent: RecyclerView): Int {
        if (mDecorationEnd != -1) return mDecorationEnd
        if (mDecorationEndOffset != -1) {
            val adapter = parent.adapter
            return adapter.itemCount - 1 - mDecorationEndOffset
        }
        return -1
    }

    interface Padding {
        operator fun get(position: Int, rect: Rect): Boolean
    }

    companion object {

        private val ATTRS = intArrayOf(android.R.attr.listDivider)

        val HORIZONTAL_LIST = LinearLayoutManager.HORIZONTAL

        val VERTICAL_LIST = LinearLayoutManager.VERTICAL
    }
}
