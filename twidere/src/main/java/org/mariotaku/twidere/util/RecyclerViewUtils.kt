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

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewParent

/**
 * Created by mariotaku on 15/4/13.
 */
object RecyclerViewUtils {

    fun findRecyclerViewChild(recyclerView: RecyclerView, view: View?): View? {
        if (view == null) return null
        val parent = view.parent
        if (parent === recyclerView) return view
        if (parent is View) {
            return findRecyclerViewChild(recyclerView, parent)
        }
        return null
    }

    fun focusNavigate(recyclerView: RecyclerView, layoutManager: LinearLayoutManager, currentFocus: Int, direction: Int) {
        if (direction == 0) return
        if (currentFocus < 0) {
            focusFallback(layoutManager)
        } else {
            val view: View?
            if (direction > 0 && currentFocus == layoutManager.findLastVisibleItemPosition()) {
                view = recyclerView.focusSearch(recyclerView.focusedChild, View.FOCUS_DOWN)
            } else if (direction < 0 && currentFocus == layoutManager.findFirstVisibleItemPosition()) {
                view = recyclerView.focusSearch(recyclerView.focusedChild, View.FOCUS_UP)
            } else {
                view = null
            }
            val firstVisibleView = layoutManager.findViewByPosition(currentFocus + if (direction > 0) 1 else -1)
            val viewToFocus: View?
            if (firstVisibleView != null) {
                viewToFocus = firstVisibleView
            } else if (view != null) {
                viewToFocus = findRecyclerViewChild(recyclerView, view)
            } else {
                viewToFocus = null
            }
            if (viewToFocus == null) return
            val nextPos = layoutManager.getPosition(viewToFocus)
            if (nextPos < 0 || (nextPos - currentFocus) * direction < 0) {
                focusFallback(layoutManager)
                return
            }
            focus(viewToFocus)
        }
    }

    fun pageScroll(recyclerView: RecyclerView, layoutManager: LinearLayoutManager, direction: Int) {
        val contentHeight = layoutManager.height - layoutManager.paddingTop - layoutManager.paddingBottom
        recyclerView.smoothScrollBy(0, if (direction > 0) contentHeight else -contentHeight)
    }

    private fun focus(view: View) {
        if (view.isInTouchMode) {
            view.requestFocusFromTouch()
        } else {
            view.requestFocus()
        }
    }

    private fun focusFallback(layoutManager: LinearLayoutManager) {
        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
        val firstVisibleView = layoutManager.findViewByPosition(firstVisibleItemPosition) ?: return
        focus(firstVisibleView)
    }
}
