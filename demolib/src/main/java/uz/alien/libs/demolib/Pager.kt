package uz.alien.libs.demolib

import android.animation.FloatEvaluator
import android.animation.ValueAnimator
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.os.postDelayed
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

abstract class Pager(val activity: AppCompatActivity) { // TODO Restyle Pager library to last version

    // Main Properties

    lateinit var handler: Handler

    var insetsController: WindowInsetsControllerCompat? = null

    lateinit var root: ViewGroup

    private fun backPress() {
        activity.moveTaskToBack(true)
    }

    fun showNavigation() {
        insetsController?.show(WindowInsetsCompat.Type.navigationBars())
    }

    fun hideNavigation() {
        insetsController?.hide(WindowInsetsCompat.Type.navigationBars())
    }

    fun showSystemBars() {
        insetsController?.show(WindowInsetsCompat.Type.systemBars())
    }

    fun hideSystemBars() {
        insetsController?.show(WindowInsetsCompat.Type.systemBars())
    }

    var tryShowingSide: Side? = null

    open var density = 0.0f
    open var screenWidth = 0
    open var screenHeight = 0

    // Animations Properties
    open val duration = 137L * 1

    lateinit var lockPage: Button
    lateinit var lockSide: Button
    lateinit var lockMove: Button

    // For Paging Available
    private var isPagingAvailable = true

    // For All Elements
    enum class Level {
        PAGE,
        SIDE,
        MOVE
    }

    private var level = Level.PAGE

    // For Paging
    private lateinit var currentPage: Page
    private val pages = ArrayList<Page>()

    // For Siding
    private lateinit var currentSide: Side
    private var sides = ArrayList<Side>()

    // For Moving
    private lateinit var currentMove: Move
    private var moves = ArrayList<Move>()

    // For Go Back
    private var isGoBack = true
    private var goBackMessage = ""

    // Started Page
    private var isStarted = false

    fun setNoGoBack(message: String) {
        isGoBack = false
        goBackMessage = message
    }

    private fun resetGoBack() {
        isGoBack = true
        goBackMessage = ""
    }

    open fun init() {
        handler = Handler(Looper.getMainLooper())
        pages.clear()
        sides.clear()
        moves.clear()
    }

    fun setBackground(background: ViewGroup, showPagers: Runnable) {
        background.visibility = View.VISIBLE
        background.startAnimation(AnimationSet(true).apply {
            addAnimation(AlphaAnimation(0.0f, 1.0f).apply {
                duration = this@Pager.duration * 2
            })
        })
        background.alpha = 1.0f
        handler.postDelayed(duration * 6) {
            showPagers.run()
        }
    }

    fun firstPage(page: Page, systemBarDurationTimes: Int, duration: Long = this.duration * 2) {
        handler.postDelayed(duration * 3) {
            openPage(page)
            handler.postDelayed(this.duration * systemBarDurationTimes) {
                showSystemBars()
            }
        }
    }

    fun openPage(page: Page) {

        if (isPagingAvailable) {

            resetGoBack()

            when (level) {

                Level.MOVE -> {

                    hideMoves()

                    hideSides()

                    handler.postDelayed(duration * 3) {

                        if (isStarted) {
                            swapPage(page)
                        } else {
                            startPage(page)
                        }
                    }
                }

                Level.SIDE -> {

                    hideSides()

                    handler.postDelayed(duration * 3) {

                        if (isStarted) {
                            swapPage(page)
                        } else {
                            startPage(page)
                        }
                    }
                }

                Level.PAGE -> {

                    if (isStarted) {
                        swapPage(page)
                    } else {
                        startPage(page)
                    }
                }
            }
        }
    }

    private fun startPage(page: Page) {

        isPagingAvailable = false

        isStarted = true

        lockPage.visibility = View.VISIBLE

        currentPage = page

        if (currentPage.isBackable) {
            pages.add(currentPage)
        }

        currentPage.background.visibility = View.VISIBLE
        currentPage.foreground.visibility = View.VISIBLE

        val isAnimate = currentPage.onStart()

        handler.post {

            if (isAnimate) {

                currentPage.background.startAnimation(Page.pageBackInFastOut(duration * 2))
                currentPage.background.alpha = 1.0f

                currentPage.foreground.startAnimation(Page.pageForeInZoomIn(duration * 2))
                currentPage.foreground.alpha = 1.0f

                val radiusAnimator = ValueAnimator.ofObject(FloatEvaluator(), 35.0f * density, 0.0f * density)
                radiusAnimator.setDuration(duration * 2)
                radiusAnimator.addUpdateListener { animator ->
                    try{
                        (currentPage.foreground as CardView).radius = animator.animatedValue as Float
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                radiusAnimator.start()

                handler.postDelayed(duration * 2) {
                    isPagingAvailable = true
                    lockPage.visibility = View.GONE
                }
            } else {
                isPagingAvailable = true
                lockPage.visibility = View.GONE
            }
        }
    }

    private fun swapPage(page: Page) {

        isPagingAvailable = false
        lockPage.visibility = View.VISIBLE

        val oldPage = currentPage

        currentPage = page

        if (currentPage.isBackable) {
            pages.add(currentPage)
        }

        oldPage.onPause()

        handler.post {

            if (oldPage !== currentPage) {

                oldPage.background.startAnimation(Page.pageBackOutFastIn(duration * 2))

                currentPage.background.visibility = View.VISIBLE

                if (!currentPage.isCustomBackAnimate) {

                    currentPage.background.startAnimation(Page.pageBackInFastOut(duration * 2))
                    currentPage.background.alpha = 1.0f
                }

                handler.postDelayed(duration * 2) {
                    oldPage.background.alpha = 0.0f
                    oldPage.background.visibility = View.GONE
                }
            }

            oldPage.foreground.startAnimation(Page.pageForeOutZoomOut(duration))

            handler.postDelayed(duration) {

                oldPage.foreground.alpha = 0.0f
                oldPage.foreground.visibility = View.GONE

                currentPage.foreground.visibility = View.VISIBLE

                val isAnimate = currentPage.onStart()

                handler.post {

                    if (isAnimate) {

                        currentPage.foreground.startAnimation(Page.pageForeInZoomIn(duration))
                        currentPage.foreground.alpha = 1.0f

                        val radiusAnimator = ValueAnimator.ofObject(FloatEvaluator(), 35.0f * density, 0.0f * density)
                        radiusAnimator.setDuration(duration)
                        radiusAnimator.addUpdateListener { animator ->
                            try{
                                (currentPage.foreground as CardView).radius = animator.animatedValue as Float
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        radiusAnimator.start()

                        handler.postDelayed(duration) {
                            isPagingAvailable = true
                            lockPage.visibility = View.GONE
                        }
                    } else {
                        isPagingAvailable = true
                        lockPage.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun closePage() {

        isPagingAvailable = false
        lockPage.visibility = View.VISIBLE

        val oldPage = currentPage

        if (currentPage.isBackable) {
            pages.removeLast()
        }

        currentPage = pages.last()


        if (oldPage !== currentPage) {

            oldPage.background.startAnimation(Page.pageBackOutFastIn(duration * 2))

            currentPage.background.visibility = View.VISIBLE
            currentPage.background.startAnimation(Page.pageBackInFastOut(duration * 2))
            currentPage.background.alpha = 1.0f

            handler.postDelayed(duration * 2) {
                oldPage.background.alpha = 0.0f
                oldPage.background.visibility = View.GONE
            }
        }

        oldPage.foreground.startAnimation(Page.pageForeOutZoomIn(duration))

        val radiusAnimator = ValueAnimator.ofObject(FloatEvaluator(), 0.0f * density, 35.0f * density)
        radiusAnimator.setDuration(duration)
        radiusAnimator.addUpdateListener { animator ->
            try{
                (oldPage.foreground as CardView).radius = animator.animatedValue as Float
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        radiusAnimator.start()

        handler.postDelayed(duration) {

            oldPage.foreground.alpha = 0.0f
            oldPage.foreground.visibility = View.GONE

            currentPage.foreground.visibility = View.VISIBLE

            currentPage.onStart()

            handler.post {

                try {
                    (currentPage.foreground as CardView).radius = 0.0f
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                currentPage.foreground.startAnimation(Page.pageForeInZoomOut(duration))
                currentPage.foreground.alpha = 1.0f

                handler.postDelayed(duration) {
                    isPagingAvailable = true
                    lockPage.visibility = View.GONE
                }
            }
        }
    }

    fun showSide(side: Side, notAdd: Boolean = false) {

        isPagingAvailable = false

        tryShowingSide = null

        lockSide.visibility = View.VISIBLE

        level = Level.SIDE

        currentSide = side

        if (!notAdd) sides.add(currentSide)

        currentSide.onStart()

        handler.post {

            currentSide.show(duration * 2)

            handler.postDelayed(duration * 2) {

                isPagingAvailable = true
                lockSide.visibility = View.GONE
            }
        }
    }

    fun hideSide(duration: Long = this.duration * 2, side: Side? = null) {

        tryShowingSide = null

        if (sides.isNotEmpty()) {

            isPagingAvailable = false
            lockSide.visibility = View.VISIBLE

            val oldSide = currentSide

            sides.removeLast()

            if (sides.isNotEmpty()) {

                currentSide = sides.last()
            } else {

                level = Level.PAGE
            }


            val revPercent = oldSide.background.alpha
            val percent = 1.0f - revPercent

            var d = if (this.duration * 2 < duration) this.duration * 2 else duration
            d = (d * revPercent).toLong()


            if (oldSide !== currentSide || sides.isEmpty()) {

                oldSide.background.startAnimation(Side.sideBackOut(revPercent, d))
                oldSide.background.alpha = 1.0f

                oldSide.target.scaleX = 1.0f
                oldSide.target.scaleY = 1.0f
                oldSide.target.startAnimation(Side.sideTargetOut(revPercent, oldSide.width, oldSide.height, d))

                handler.postDelayed(d) {

                    oldSide.background.alpha = 0.0f
                }
            }

            oldSide.hide(d, revPercent, percent)

            handler.postDelayed(d) {

                isPagingAvailable = true
                lockSide.visibility = View.GONE
            }
        } else {

            side?.let {

                val revPercent = it.background.alpha
                val percent = 1.0f - revPercent

                var d = if (this.duration * 2 < duration) this.duration * 2 else duration
                d = (d * revPercent).toLong()

                it.background.startAnimation(Side.sideBackOut(revPercent, d))
                it.background.alpha = 1.0f

                it.target.scaleX = 1.0f
                it.target.scaleY = 1.0f
                it.target.startAnimation(Side.sideTargetOut(revPercent, it.width, it.height, d))

                handler.postDelayed(d) {

                    it.background.alpha = 0.0f
                }

                it.hide(d, revPercent, percent)
            }
        }
    }

    fun showMove(move: Move) {

        isPagingAvailable = false

        lockPage.visibility = View.VISIBLE
        lockSide.visibility = View.VISIBLE

        lockMove.visibility = View.VISIBLE

        level = Level.MOVE

        currentMove = move

        moves.add(currentMove)

        currentMove.background.visibility = View.VISIBLE
        currentMove.foreground.visibility = View.VISIBLE

        currentMove.onStart()

        handler.post {

            currentMove.background.startAnimation(Move.floatBackInFastIn(duration * 2))
            currentMove.background.alpha = 1.0f

            currentMove.move.startAnimation(Move.floatForeIn(duration * 2))
            currentMove.foreground.alpha = 1.0f

            handler.postDelayed(duration * 2) {
                isPagingAvailable = true
                lockMove.visibility = View.GONE
            }
        }
    }

    fun hideMove(animation: Animation = Move.floatForeOut(duration * 2)) {

        if (moves.isNotEmpty()) {

            isPagingAvailable = false

            lockMove.visibility = View.VISIBLE

            val oldMove = currentMove

            moves.removeLast()

            if (moves.isNotEmpty()) {

                currentMove = moves.last()
            } else {

                level = if (sides.isNotEmpty()) Level.SIDE else Level.PAGE
            }


            if (oldMove !== currentMove || moves.isEmpty()) {

                oldMove.background.startAnimation(Move.floatBackOutFastOut(duration * 2))

                handler.postDelayed(duration * 2) {

                    oldMove.background.alpha = 0.0f
                    oldMove.background.visibility = View.GONE
                }
            }

            oldMove.move.startAnimation(animation)

            handler.postDelayed(duration * 2) {

                oldMove.foreground.alpha = 0.0f
                oldMove.foreground.visibility = View.GONE

                isPagingAvailable = true

                lockMove.visibility = View.GONE

                lockSide.visibility = View.GONE
                lockPage.visibility = View.GONE
            }
        }
    }

    private fun hideSides() {

        for (i in 0 until sides.size) {
            hideSide(duration * 2)
        }
    }

    private fun hideMoves() {

        for (i in moves) {
            hideMove()
        }
    }

    fun changeFocus(hasFocus: Boolean) {

        if (!hasFocus) {
            tryShowingSide?.let {
                hideSide(duration * 2, it)
            }
        }
    }

    fun back() {

        if (isPagingAvailable) {

            when (level) {

                Level.MOVE -> {

                    val isMoveContinue = currentMove.onPause()

                    handler.post {

                        if (isMoveContinue) {
                            hideMove()
                        }
                    }
                }

                Level.SIDE -> {

                    Log.d("@@@@", "back press side")

                    val isSideContinue = currentSide.onPause()

                    handler.post {

                        if (isSideContinue) {
                            hideSide(duration * 2)
                        }
                    }
                }

                Level.PAGE -> {

                    val isPageContinue = currentPage.onPause()

                    handler.post {

                        if (isPageContinue) {

                            if (isGoBack) {
                                if (pages.size > 1) {
                                    closePage()
                                } else {
                                    backPress()
                                }
                            } else {
                                Toast.makeText(activity, goBackMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }
}