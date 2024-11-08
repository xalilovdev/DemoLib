package uz.alien.libs.demolib

import android.animation.FloatEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import androidx.core.os.postDelayed
import kotlin.math.abs

abstract class Side(open val pager: Pager) {

    lateinit var handler: Handler

    lateinit var latencyHandler: Handler

    private var isHidden = true

    enum class Direction {
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }

    abstract val background: ViewGroup
    abstract val foreground: ViewGroup

    abstract val back: ViewGroup
    abstract val side: ViewGroup

    abstract val target: CardView

    open val direction: Direction = Direction.LEFT

    var width = 0.0f
    var height = 0.0f

    open var size = 0.0f
    open val occupied = 0.82f

    open var thresholdPercent = 0.05f
    open var threshold = 0.0f

    open fun onStart() {}

    open fun onPause() : Boolean {
        return true
    }

    private fun drawerLocking(root: ViewGroup) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            root.setOnApplyWindowInsetsListener { _, insets ->
                val rect = when (direction) {
                    Direction.LEFT -> Rect(0, 0, insets.systemGestureInsets.left, root.height)
                    Direction.RIGHT -> Rect((width - insets.systemGestureInsets.right).toInt(), 0, width.toInt(), root.height)
                    Direction.TOP -> Rect(0, 0, 0, 0)
                    Direction.BOTTOM -> Rect(0, 0, 0, 0)
                }
                root.systemGestureExclusionRects = listOf(rect)
                insets
            }
        }
    }

    fun sidePreparer(runnable: Runnable) {

        back.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {

            override fun onGlobalLayout() {

                back.viewTreeObserver.removeOnGlobalLayoutListener(this)

                width = back.width.toFloat()
                height = back.height.toFloat()

                when (direction) {

                    Direction.LEFT -> {

                        size = back.width * occupied

                        threshold = size * thresholdPercent

                        val layoutParams = FrameLayout.LayoutParams(
                            size.toInt(),
                            back.height
                        )

                        side.layoutParams = layoutParams

                        side.x = -size
                    }

                    Direction.RIGHT -> {

                        size = back.width * occupied

                        threshold = size * thresholdPercent

                        val layoutParams = FrameLayout.LayoutParams(
                            size.toInt(),
                            back.height
                        )

                        side.layoutParams = layoutParams

                        side.x = back.width.toFloat()
                    }

                    Direction.TOP -> {

                        size = back.height * occupied

                        threshold = size * thresholdPercent

                        val layoutParams = FrameLayout.LayoutParams(
                            back.width,
                            size.toInt()
                        )

                        side.layoutParams = layoutParams

                        side.y = -size
                    }
                    Direction.BOTTOM -> {

                        size = back.height * occupied

                        threshold = size * thresholdPercent

                        val layoutParams = FrameLayout.LayoutParams(
                            back.width,
                            size.toInt()
                        )

                        side.layoutParams = layoutParams

                        side.y = back.height.toFloat()
                    }
                }

                runnable.run()
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    fun sideSwipePreparer() {

        val swipeThreshold = 10 * pager.density
        var startTime = 0L
        var oldPos = side.x
        var startPos = 0.0f
        var cursorX = 0.0f

        when (direction) {

            Direction.LEFT -> {

                side.setOnTouchListener { _, event ->

                    when (event.action) {

                        MotionEvent.ACTION_DOWN -> {

                            startTime = System.nanoTime()

                            oldPos = side.x

                            startPos = event.rawX

                            cursorX = 0.0f
                        }

                        MotionEvent.ACTION_MOVE -> {

                            cursorX += ((event.rawX - startPos) - cursorX) * 0.2f

                            val pos = oldPos + cursorX

                            latencyHandler.postDelayed(pager.duration) {

                                side.x = pos

                                if (side.x > 0.0f) side.x = 0.0f
                                if (side.x < -size) side.x = -size

                                val posMin = -size
                                val posMax = 0.0f
                                val alphaMin = 0.0f
                                val alphaMax = 1.0f

                                val alpha = ((side.x - posMin) / (posMax - posMin)) * (alphaMax - alphaMin) + alphaMin

                                val scale = 1.0f - (0.08f * alpha)
                                target.scaleX = scale
                                target.scaleY = scale

                                val radius = (35.0f * pager.density) * alpha
                                target.radius = radius

                                background.alpha = alpha
                            }
                        }

                        MotionEvent.ACTION_UP -> {

                            latencyHandler.removeCallbacksAndMessages(null)

                            val delta = startPos - event.rawX

                            val elapsedTime = startTime - System.nanoTime()

                            val countStep = size / abs(delta)

//                            val duration = abs(countStep * elapsedTime) / 1_000_000.0f
                            val duration = pager.duration * 3

                            if (abs(delta) > swipeThreshold) {
                                if (cursorX < (event.rawX - startPos)) {
                                    pager.showSide(this, true)
                                } else {
                                    pager.hideSide(duration.toLong(), this)
                                }
                            } else {
                                pager.hideSide(duration.toLong(), this)
                            }
//                            if (delta > threshold) {
//                                pager.hideSide(pager.duration * 3, this)
//                            } else {
//                                pager.showSide(this, true)
//                            }
                        }
                    }
                    true
                }
            }

            Direction.RIGHT -> {

                side.setOnTouchListener { _, event ->

                    when (event.action) {

                        MotionEvent.ACTION_DOWN -> {

                            startTime = System.nanoTime()

                            startPos = event.rawX
                        }

                        MotionEvent.ACTION_MOVE -> {

                            side.x = width - size + event.rawX - startPos

                            if (side.x > width) side.x = width
                            if (side.x < width - size) side.x = width - size

                            val posMin = width - size
                            val posMax = width
                            val alphaMin = 0.0f
                            val alphaMax = 1.0f

                            val alpha = ((side.x - posMax) / (posMin - posMax)) * (alphaMax - alphaMin) + alphaMin

                            val scale = 1.0f - (0.08f * alpha)
                            target.scaleX = scale
                            target.scaleY = scale

                            background.alpha = alpha
                        }

                        MotionEvent.ACTION_UP -> {

                            val delta = startPos - event.rawX

                            val elapsedTime = startTime - System.nanoTime()

                            val countStep = size / abs(delta)

                            val duration = abs(countStep * elapsedTime) / 1_000_000.0f

                            if (delta < -threshold) {
                                pager.hideSide(duration.toLong(), this)
                            } else {
                                pager.showSide(this, true)
                            }
                        }
                    }
                    true
                }
            }

            Direction.TOP -> {

                side.setOnTouchListener { _, event ->

                    when (event.action) {

                        MotionEvent.ACTION_DOWN -> {

                            startTime = System.nanoTime()

                            startPos = event.rawY
                        }

                        MotionEvent.ACTION_MOVE -> {

                            side.y = event.rawY - startPos

                            if (side.y > 0.0f) side.y = 0.0f
                            if (side.y < -size) side.y = -size

                            val posMin = -size
                            val posMax = 0.0f
                            val alphaMin = 0.0f
                            val alphaMax = 1.0f

                            val alpha = ((side.y - posMin) / (posMax - posMin)) * (alphaMax - alphaMin) + alphaMin

                            val scale = 1.0f - (0.08f * alpha)
                            target.scaleX = scale
                            target.scaleY = scale

                            background.alpha = alpha
                        }

                        MotionEvent.ACTION_UP -> {

                            val delta = startPos - event.rawY

                            val elapsedTime = startTime - System.nanoTime()

                            val countStep = size / abs(delta)

                            val duration = abs(countStep * elapsedTime) / 1_000_000.0f

                            if (delta > threshold) {
                                pager.hideSide(duration.toLong(), this)
                            } else {
                                pager.showSide(this, true)
                            }
                        }
                    }
                    true
                }
            }

            Direction.BOTTOM -> {

                side.setOnTouchListener { _, event ->

                    when (event.action) {

                        MotionEvent.ACTION_DOWN -> {

                            startTime = System.nanoTime()

                            startPos = event.rawY
                        }

                        MotionEvent.ACTION_MOVE -> {

                            side.y = height - size + event.rawY - startPos

                            if (side.y > height) side.y = height
                            if (side.y < height - size) side.y = height - size

                            val posMin = height - size
                            val posMax = height
                            val alphaMin = 0.0f
                            val alphaMax = 1.0f

                            val alpha = ((side.y - posMax) / (posMin - posMax)) * (alphaMax - alphaMin) + alphaMin

                            val scale = 1.0f - (0.08f * alpha)
                            target.scaleX = scale
                            target.scaleY = scale

                            background.alpha = alpha
                        }

                        MotionEvent.ACTION_UP -> {

                            val delta = startPos - event.rawY

                            val elapsedTime = startTime - System.nanoTime()

                            val countStep = size / abs(delta)

                            val duration = abs(countStep * elapsedTime) / 1_000_000.0f

                            if (delta < -threshold) {
                                pager.hideSide(duration.toLong(), this)
                            } else {
                                pager.showSide(this, true)
                            }
                        }
                    }
                    true
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setBackHider() {

        foreground.setOnTouchListener { _, event ->

            if (event.action == MotionEvent.ACTION_UP) {

                pager.hideSide(pager.duration * 2, this)
            }

            !isHidden
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setBackSwiper(rect: Rect) {

        drawerLocking(pager.root)

        val startSwipeThreshold = 64 * pager.density
        val swipeThreshold = 10 * pager.density
        var swipePanel = false
        var oldPos = side.x
        var cursorX = 0.0f
        var delta = 0.0f

        var startTime = 0L

        var startPos = 0.0f

        when (direction) {

            Direction.LEFT -> {

                foreground.setOnTouchListener { _, event ->

                    when (event.action) {

                        MotionEvent.ACTION_DOWN -> {

                            if (isHidden) {

                                if (event.x > rect.left && event.x < rect.right && event.y > rect.top && event.y < rect.bottom) {

                                    oldPos = side.x

                                    startTime = System.nanoTime()

                                    startPos = event.x

                                    swipePanel = true

                                    cursorX = 0.0f

                                    pager.tryShowingSide = this

                                } else {

                                    swipePanel = false
                                }

                            } else {

                                swipePanel = true
                            }
                        }

                        MotionEvent.ACTION_MOVE -> {

                            if (isHidden) {

                                if (swipePanel) {

                                    delta = event.x - cursorX
                                    cursorX += (event.x - cursorX) * 0.2f

                                    val pos = oldPos + cursorX

                                    latencyHandler.postDelayed(pager.duration) {

                                        side.x = pos

                                        if (side.x > 0.0f) side.x = 0.0f
                                        if (side.x < -size) side.x = -size

                                        val posMin = -size
                                        val posMax = 0.0f
                                        val alphaMin = 0.0f
                                        val alphaMax = 1.0f

                                        val alpha = ((side.x - posMin) / (posMax - posMin)) * (alphaMax - alphaMin) + alphaMin

                                        val scale = 1.0f - (0.08f * alpha)
                                        target.scaleX = scale
                                        target.scaleY = scale

                                        val radius = (35.0f * pager.density) * alpha

                                        target.radius = radius

                                        background.alpha = alpha
                                    }
                                }
                            }
                        }

                        MotionEvent.ACTION_UP -> {

                            if (isHidden) {

//                                latencyHandler.postDelayed(pager.duration / 2) {

                                    latencyHandler.removeCallbacksAndMessages(null)

                                    val delta = startPos - event.rawX

                                    val elapsedTime = startTime - System.nanoTime()

                                    val countStep = size / abs(delta)

//                                val duration = abs(countStep * elapsedTime) / 1_000_000.0f
                                    val duration = pager.duration * 3

                                    if (abs(delta) > swipeThreshold) {
                                        if (cursorX < event.x) {
                                            pager.showSide(this)
                                        } else {
                                            pager.hideSide(duration.toLong(), this)
                                        }
                                    } else {
                                        pager.hideSide(duration.toLong(), this)
                                    }
//
//                                    if (delta > 0) {
//
//                                        pager.hideSide(duration.toLong(), this)
//
//                                    } else {
//
//                                        pager.showSide(this)
//                                    }

                                    swipePanel = false

//                                }

                            } else {

                                pager.hideSide(pager.duration * 2, this)
                            }
                        }
                    }
                    swipePanel
                }
            }

            Direction.RIGHT -> {

                foreground.setOnTouchListener { _, event ->

                    when (event.action) {

                        MotionEvent.ACTION_DOWN -> {

                            if (isHidden) {

                                if (event.x > width - startSwipeThreshold) {

                                    oldPos = side.x

                                    startTime = System.nanoTime()

                                    startPos = event.x

                                    swipePanel = true

                                    pager.tryShowingSide = this

                                } else {

                                    swipePanel = false
                                }

                            } else {

                                swipePanel = true
                            }
                        }

                        MotionEvent.ACTION_MOVE -> {

                            if (isHidden) {

                                if (swipePanel) {

                                    side.x = oldPos + event.x - startPos

                                    if (side.x > width) side.x = width
                                    if (side.x < width - size) side.x = width - size

                                    val posMin = width - size
                                    val posMax = width
                                    val alphaMin = 0.0f
                                    val alphaMax = 1.0f

                                    val alpha = ((side.x - posMax) / (posMin - posMax)) * (alphaMax - alphaMin) + alphaMin

                                    val scale = 1.0f - (0.08f * alpha)
                                    target.scaleX = scale
                                    target.scaleY = scale

                                    background.alpha = alpha
                                }
                            }
                        }

                        MotionEvent.ACTION_UP -> {

                            if (isHidden) {

                                val delta = startPos - event.rawX

                                val elapsedTime = startTime - System.nanoTime()

                                val countStep = size / abs(delta)

                                val duration = abs(countStep * elapsedTime) / 1_000_000.0f

                                if (abs(delta) > swipeThreshold) {

                                    if (delta < 0) {

                                        pager.hideSide(duration.toLong(), this)

                                    } else {

                                        pager.showSide(this)
                                    }

                                } else {

                                    pager.hideSide(duration.toLong(), this)
                                }

                                swipePanel = false

                            } else {

                                pager.hideSide(pager.duration * 2, this)
                            }
                        }
                    }
                    swipePanel
                }
            }

            Direction.TOP -> {

                foreground.setOnTouchListener { _, event ->

                    when (event.action) {

                        MotionEvent.ACTION_DOWN -> {

                            if (isHidden) {

                                if (event.y < startSwipeThreshold) {

                                    oldPos = side.y

                                    startTime = System.nanoTime()

                                    startPos = event.y

                                    swipePanel = true

                                    pager.tryShowingSide = this

                                } else {

                                    swipePanel = false
                                }

                            } else {

                                swipePanel = true
                            }
                        }

                        MotionEvent.ACTION_MOVE -> {

                            if (isHidden) {

                                if (swipePanel) {

                                    side.y = oldPos + event.y - startPos

                                    if (side.y > 0.0f) side.y = 0.0f
                                    if (side.y < -size) side.y = -size

                                    val posMin = -size
                                    val posMax = 0.0f
                                    val alphaMin = 0.0f
                                    val alphaMax = 1.0f

                                    val alpha = ((side.y - posMin) / (posMax - posMin)) * (alphaMax - alphaMin) + alphaMin

                                    val scale = 1.0f - (0.08f * alpha)
                                    target.scaleX = scale
                                    target.scaleY = scale

                                    background.alpha = alpha
                                }
                            }
                        }

                        MotionEvent.ACTION_UP -> {

                            if (isHidden) {

                                val delta = startPos - event.rawY

                                val elapsedTime = startTime - System.nanoTime()

                                val countStep = size / abs(delta)

                                val duration = abs(countStep * elapsedTime) / 1_000_000.0f

                                if (abs(delta) > swipeThreshold) {

                                    if (delta > 0) {

                                        pager.hideSide(duration.toLong(), this)

                                    } else {

                                        pager.showSide(this)
                                    }

                                } else {

                                    pager.hideSide(duration.toLong(), this)
                                }

                                swipePanel = false

                            } else {

                                pager.hideSide(pager.duration * 2, this)
                            }
                        }
                    }
                    swipePanel
                }
            }

            Direction.BOTTOM -> {

                foreground.setOnTouchListener { _, event ->

                    when (event.action) {

                        MotionEvent.ACTION_DOWN -> {

                            if (isHidden) {

                                if (event.y > height - startSwipeThreshold) {

                                    oldPos = side.y

                                    startTime = System.nanoTime()

                                    startPos = event.y

                                    swipePanel = true

                                    pager.tryShowingSide = this

                                } else {

                                    swipePanel = false
                                }

                            } else {

                                swipePanel = true
                            }
                        }

                        MotionEvent.ACTION_MOVE -> {

                            if (isHidden) {

                                if (swipePanel) {

                                    side.y = oldPos + event.y - startPos

                                    if (side.y > height) side.y = height
                                    if (side.y < height - size) side.y = height - size

                                    val posMin = height - size
                                    val posMax = height
                                    val alphaMin = 0.0f
                                    val alphaMax = 1.0f

                                    val alpha = ((side.y - posMax) / (posMin - posMax)) * (alphaMax - alphaMin) + alphaMin

                                    val scale = 1.0f - (0.08f * alpha)
                                    target.scaleX = scale
                                    target.scaleY = scale

                                    background.alpha = alpha
                                }
                            }
                        }

                        MotionEvent.ACTION_UP -> {

                            if (isHidden) {

                                val delta = startPos - event.rawY

                                val elapsedTime = startTime - System.nanoTime()

                                val countStep = size / abs(delta)

                                val duration = abs(countStep * elapsedTime) / 1_000_000.0f

                                if (abs(delta) > swipeThreshold) {

                                    if (delta < 0) {

                                        pager.hideSide(duration.toLong(), this)

                                    } else {

                                        pager.showSide(this)
                                    }

                                } else {

                                    pager.hideSide(duration.toLong(), this)
                                }

                                swipePanel = false

                            } else {

                                pager.hideSide(pager.duration * 2, this)
                            }
                        }
                    }
                    swipePanel
                }
            }
        }
    }

    fun show(duration: Long) {

        val revPercent = background.alpha
        val percent = 1.0f - revPercent

        val d = (duration * percent).toLong()

        background.startAnimation(sideBackIn(revPercent, d))
        background.alpha = 1.0f

        target.scaleX = 0.92f
        target.scaleY = 0.92f
        target.startAnimation(sideTargetIn(percent, width, height, d))

        ValueAnimator.ofObject(FloatEvaluator(), (35.0f * pager.density) * revPercent, (35.0f * pager.density)).apply {

            setDuration(d)

            interpolator = AccelerateInterpolator()

            addUpdateListener { animator ->
                target.radius = animator.animatedValue as Float
            }

            start()
        }

        handler.postDelayed(d) {

            isHidden = false
        }

        when (direction) {

            Direction.LEFT -> {
                side.x = 0.0f
                side.startAnimation(sideInLeft(size * percent, d))
            }

            Direction.RIGHT -> {
                side.x = back.width.toFloat() - size
                side.startAnimation(sideInRight(percent, size, d))
            }

            Direction.TOP -> {
                side.y = 0.0f
                side.startAnimation(sideInTop(size * percent, d))
            }

            Direction.BOTTOM -> {
                side.y = height - size
                side.startAnimation(sideInBottom(percent, size, d))
            }
        }
    }

    fun hide(duration: Long, revPercent: Float, percent: Float) {

        isHidden = true

        ValueAnimator.ofObject(FloatEvaluator(), (35.0f * pager.density) * revPercent, 0.0f).apply {

            setDuration(duration)

            interpolator = DecelerateInterpolator()

            addUpdateListener { animator ->
                target.radius = animator.animatedValue as Float
            }

            start()
        }

        when (direction) {

            Direction.LEFT -> {

                side.x = 0.0f
                side.startAnimation(sideOutLeft(percent, size, duration))

                handler.postDelayed(duration) {

                    side.x = -size
                }
            }

            Direction.RIGHT -> {

                side.x = width
                side.startAnimation(sideOutRight(revPercent, size, duration))
            }

            Direction.TOP -> {

                side.y = 0.0f
                side.startAnimation(sideOutTop(percent, size, duration))

                handler.postDelayed(duration) {

                    side.y = -size
                }
            }

            Direction.BOTTOM -> {

                side.y = height
                side.startAnimation(sideOutBottom(revPercent, size, duration))
            }
        }
    }

    companion object {

        fun sideBackIn(percent: Float, duration: Long) = AnimationSet(true).apply {
            addAnimation(
                AlphaAnimation(percent, 1.0f).apply {
                    this.duration = duration
                    this.interpolator = AccelerateInterpolator()
                }
            )
        }

        fun sideBackOut(percent: Float, duration: Long) = AnimationSet(true).apply {
            addAnimation(
                AlphaAnimation(percent, 0.0f).apply {
                    this.duration = duration
                    this.interpolator = LinearInterpolator()
                }
            )
        }

        fun sideTargetIn(percent: Float, width: Float, height: Float, duration: Long)= AnimationSet(true).apply {

            addAnimation(
                ScaleAnimation(
                    1.0f + 0.087f * percent, 1.0f,
                    1.0f + 0.087f * percent, 1.0f,
                    width / 2,
                    height / 2
                ).apply {
                    this.duration = duration
                    this.interpolator = AccelerateInterpolator()
                }
            )
        }

        fun sideTargetOut(percent: Float, width: Float, height: Float, duration: Long)= AnimationSet(true).apply {
            addAnimation(
                ScaleAnimation(
                    1.0f - 0.08f * percent, 1.0f,
                    1.0f - 0.08f * percent, 1.0f,
                    width / 2,
                    height / 2
                ).apply {
                    this.duration = duration
                    this.interpolator = LinearInterpolator()
                }
            )
        }

        fun sideInLeft(size: Float, duration: Long) = AnimationSet(true).apply {
            addAnimation(
                TranslateAnimation(
                    -size,
                    0.0f,
                    0.0f,
                    0.0f
                ).apply {
                    this.duration = duration
                    this.interpolator = AccelerateInterpolator()
                }
            )
        }

        fun sideOutLeft(percent: Float, size: Float, duration: Long) = AnimationSet(true).apply {
            addAnimation(
                TranslateAnimation(
                    0.0f - size * percent,
                    -size,
                    0.0f,
                    0.0f
                ).apply {
                    this.duration = duration
                    this.interpolator = DecelerateInterpolator()
                }
            )
        }

        fun sideInRight(percent: Float, size: Float, duration: Long) = AnimationSet(true).apply {
            addAnimation(
                TranslateAnimation(
                    size * percent,
                    0.0f,
                    0.0f,
                    0.0f
                ).apply {
                    this.duration = duration
                    this.interpolator = AccelerateInterpolator()
                }
            )
        }

        fun sideOutRight(percent: Float, size: Float, duration: Long) = AnimationSet(true).apply {
            addAnimation(
                TranslateAnimation(
                    -size * percent,
                    0.0f,
                    0.0f,
                    0.0f
                ).apply {
                    this.duration = duration
                    this.interpolator = DecelerateInterpolator()
                }
            )
        }

        fun sideInTop(size: Float, duration: Long) = AnimationSet(true).apply {
            addAnimation(
                TranslateAnimation(
                    0.0f,
                    0.0f,
                    -size,
                    0.0f
                ).apply {
                    this.duration = duration
                    this.interpolator = AccelerateInterpolator()
                }
            )
        }

        fun sideOutTop(percent: Float, size: Float, duration: Long) = AnimationSet(true).apply {
            addAnimation(
                TranslateAnimation(
                    0.0f,
                    0.0f,
                    0.0f - size * percent,
                    -size
                ).apply {
                    this.duration = duration
                    this.interpolator = DecelerateInterpolator()
                }
            )
        }

        fun sideInBottom(percent: Float, size: Float, duration: Long) = AnimationSet(true).apply {
            addAnimation(
                TranslateAnimation(
                    0.0f,
                    0.0f,
                    size * percent,
                    0.0f
                ).apply {
                    this.duration = duration
                    this.interpolator = AccelerateInterpolator()
                }
            )
        }

        fun sideOutBottom(percent: Float, size: Float, duration: Long) = AnimationSet(true).apply {
            addAnimation(
                TranslateAnimation(
                    0.0f,
                    0.0f,
                    -size * percent,
                    0.0f
                ).apply {
                    this.duration = duration
                    this.interpolator = DecelerateInterpolator()
                }
            )
        }
    }
}