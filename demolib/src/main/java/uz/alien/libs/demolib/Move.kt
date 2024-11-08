package uz.alien.libs.demolib

import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.ScaleAnimation

abstract class Move {

    abstract val background: ViewGroup
    abstract val foreground: ViewGroup

    abstract val move: ViewGroup

    open fun onStart() {}

    open fun onPause() : Boolean {
        return true
    }

    companion object {

        fun floatBackInFastIn(duration: Long) = AnimationSet(true).apply {
            addAnimation(
                AlphaAnimation(0f, 1f).apply {
                    this.duration = duration
                    this.interpolator = AccelerateInterpolator()
                }
            )
        }

        fun floatBackOutFastOut(duration: Long) = AnimationSet(true).apply {
            addAnimation(
                AlphaAnimation(1f, 0f).apply {
                    this.duration = duration
                    this.interpolator = DecelerateInterpolator()
                }
            )
        }


        fun floatForeIn(duration: Long) = AnimationSet(true).apply {
            addAnimation(
                ScaleAnimation(
                    0.0f, 1.0f,
                    0.0f, 1.0f,
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
                    this.duration = duration / 2
                    this.interpolator = DecelerateInterpolator()
                }
            )
        }

        fun floatForeOut(duration: Long) = AnimationSet(true).apply {
            addAnimation(
                ScaleAnimation(
                    1.0f, 0.0f,
                    1.0f, 0.0f,
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
                    this.startOffset = duration - duration / 2
                    this.duration = duration / 2
                    this.interpolator = AccelerateInterpolator()
                }
            )
        }
    }
}