package org.mariotaku.twidere.activity.iface

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.ObjectAnimator
import android.util.Property
import android.view.animation.DecelerateInterpolator

/**
 * Created by mariotaku on 14/10/21.
 */
interface IControlBarActivity {

    fun setControlBarVisibleAnimate(visible: Boolean)

    fun setControlBarVisibleAnimate(visible: Boolean, listener: ControlBarShowHideHelper.ControlBarAnimationListener)

    /**
     * @param offset 0: invisible, 1: visible
     */
    var controlBarOffset: Float

    val controlBarHeight: Int

    fun notifyControlBarOffsetChanged()

    fun registerControlBarOffsetListener(listener: ControlBarOffsetListener)

    fun unregisterControlBarOffsetListener(listener: ControlBarOffsetListener)

    interface ControlBarOffsetListener {
        fun onControlBarOffsetChanged(activity: IControlBarActivity, offset: Float)
    }

    class ControlBarShowHideHelper(private val activity: IControlBarActivity) {
        private var controlAnimationDirection: Int = 0
        private var currentControlAnimation: ObjectAnimator? = null

        private class ControlBarOffsetProperty : Property<IControlBarActivity, Float>(Float::class.javaObjectType, null) {

            override fun set(`object`: IControlBarActivity, value: Float) {
                `object`.controlBarOffset = value
            }

            override fun get(`object`: IControlBarActivity): Float {
                return `object`.controlBarOffset
            }

            companion object {
                val SINGLETON = ControlBarOffsetProperty()
            }
        }

        interface ControlBarAnimationListener {
            fun onControlBarVisibleAnimationFinish(visible: Boolean)
        }

        @JvmOverloads fun setControlBarVisibleAnimate(visible: Boolean, listener: ControlBarAnimationListener? = null) {
            val newDirection = if (visible) 1 else -1
            if (controlAnimationDirection == newDirection) return
            if (currentControlAnimation != null && controlAnimationDirection != 0) {
                currentControlAnimation!!.cancel()
                currentControlAnimation = null
                controlAnimationDirection = newDirection
            }
            val animator: ObjectAnimator
            val offset = activity.controlBarOffset
            if (visible) {
                if (offset >= 1) return
                animator = ObjectAnimator.ofFloat<IControlBarActivity>(activity, ControlBarOffsetProperty.SINGLETON, offset, 1f)
            } else {
                if (offset <= 0) return
                animator = ObjectAnimator.ofFloat<IControlBarActivity>(activity, ControlBarOffsetProperty.SINGLETON, offset, 0f)
            }
            animator.interpolator = DecelerateInterpolator()
            animator.addListener(object : AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                }

                override fun onAnimationEnd(animation: Animator) {
                    controlAnimationDirection = 0
                    currentControlAnimation = null
                    listener?.onControlBarVisibleAnimationFinish(visible)
                }

                override fun onAnimationCancel(animation: Animator) {
                    controlAnimationDirection = 0
                    currentControlAnimation = null
                }

                override fun onAnimationRepeat(animation: Animator) {

                }
            })
            animator.duration = DURATION
            animator.start()
            currentControlAnimation = animator
            controlAnimationDirection = newDirection
        }

        companion object {

            private val DURATION = 200L
        }
    }
}
