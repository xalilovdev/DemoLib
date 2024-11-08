package uz.alien.libs.demolib

import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.ScaleAnimation

abstract class Page {

    lateinit var handler: Handler

    abstract val background: ViewGroup
    abstract val foreground: ViewGroup

    open var isBackable = true

    open var isCustomBackAnimate = false

    open fun init() {
        handler = Handler(Looper.getMainLooper())
    }

    open fun onStart() = true

    open fun onPause() = true

    companion object {

        fun pageBackInFastOut(duration: Long) = AnimationSet(true).apply {
            addAnimation(
                AlphaAnimation(0f, 1f).apply {
                    this.duration = duration
                    this.interpolator = DecelerateInterpolator()
                }
            )
        }

        fun pageBackOutFastIn(duration: Long) = AnimationSet(true).apply {
            addAnimation(
                AlphaAnimation(1f, 0f).apply {
                    this.duration = duration
                    this.interpolator = AccelerateInterpolator()
                }
            )
        }


        fun pageForeInZoomIn(duration: Long) = AnimationSet(true).apply {
            addAnimation(
                ScaleAnimation(
                    0.9f, 1.0f,
                    0.9f, 1.0f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f
                ).apply {
                    this.duration = duration
                    this.interpolator = AccelerateInterpolator()
                }
            )
            addAnimation(
                AlphaAnimation(0f, 1f).apply {
                    this.duration = duration
                    this.interpolator = DecelerateInterpolator()
                }
            )
        }

        fun pageForeInZoomOut(duration: Long) = AnimationSet(true).apply {
            addAnimation(
                ScaleAnimation(
                    1.111111f, 1.0f,
                    1.111111f, 1.0f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f
                ).apply {
                    this.duration = duration
                    this.interpolator = DecelerateInterpolator()
                }
            )
            addAnimation(
                AlphaAnimation(0f, 1f).apply {
                    this.duration = duration
                    this.interpolator = DecelerateInterpolator()
                }
            )
        }

        fun pageForeOutZoomIn(duration: Long) = AnimationSet(true).apply {
            addAnimation(
                ScaleAnimation(
                    1.0f, 0.9f,
                    1.0f, 0.9f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f
                ).apply {
                    this.duration = duration
                    this.interpolator = DecelerateInterpolator()
                }
            )
            addAnimation(
                AlphaAnimation(1f, 0f).apply {
                    this.duration = duration
                    this.interpolator = AccelerateInterpolator()
                }
            )
        }

        fun pageForeOutZoomOut(duration: Long) = AnimationSet(true).apply {
            addAnimation(
                ScaleAnimation(
                    1f, 1.111111f,
                    1f, 1.111111f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f
                ).apply {
                    this.duration = duration
                    this.interpolator = AccelerateInterpolator()
                }
            )
            addAnimation(
                AlphaAnimation(1f, 0f).apply {
                    this.duration = duration
                    this.interpolator = AccelerateInterpolator()
                }
            )
        }
    }
}