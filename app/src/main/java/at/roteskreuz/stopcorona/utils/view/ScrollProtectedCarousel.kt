package at.roteskreuz.stopcorona.utils.view

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.Carousel
import com.airbnb.epoxy.ModelView
import kotlin.math.abs

/**
 * This Carousel has scroll protection when it is nested inside another [RecyclerView] and is used with a LinearLayoutManager.
 * It [requestDisallowInterceptTouchEvent] from it's parent when it detects the beginning of a scroll in it's layout direction
 */
@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
open class ScrollProtectedCarousel : Carousel {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val improveSwipeOnItemTouchListener = ImproveSwipeOnItemTouchListener()

    override fun init() {
        super.init()
        doOnLayout {
            addScrollProtection()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        addOnItemTouchListener(improveSwipeOnItemTouchListener)
    }

    override fun onDetachedFromWindow() {
        removeOnItemTouchListener(improveSwipeOnItemTouchListener)
        super.onDetachedFromWindow()
    }

    private fun addScrollProtection() {
        val linearLayoutManager = (layoutManager as? LinearLayoutManager)
        linearLayoutManager?.orientation?.let { orientation ->
            val gestureDetector = GestureDetector(context, ScrollDetector(orientation))

            addOnItemTouchListener(object : SimpleOnItemTouchListener() {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    val shouldIntercept = gestureDetector.onTouchEvent(e)
                    if (shouldIntercept) {
                        rv.parent.requestDisallowInterceptTouchEvent(true)
                    }
                    return super.onInterceptTouchEvent(rv, e)
                }
            })
        }
    }

    /**
     * Gesture detector that asks to consume scroll events that start in a set direction
     *
     * @param direction direction of scrolls to detect
     */
    private class ScrollDetector(@Orientation val direction: Int) : GestureDetector.SimpleOnGestureListener() {

        var currentDownTIme = 0L

        /**
         *  Intercepts the first of a series of scroll events, if it has the requested direction
         */
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            if (e1 != null && e1.downTime != currentDownTIme) {
                currentDownTIme = e1.downTime

                val dY = abs(distanceY)
                val dX = abs(distanceX)
                val horizontallyScrolling = dX > dY
                return if (direction == RecyclerView.HORIZONTAL) horizontallyScrolling else !horizontallyScrolling
            }
            return false
        }
    }
}