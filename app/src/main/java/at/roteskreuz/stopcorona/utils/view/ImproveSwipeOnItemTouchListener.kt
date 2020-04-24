package at.roteskreuz.stopcorona.utils.view

import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

/**
 * [RecyclerView.OnItemTouchListener] that improves the swiping of horizontal recycler views
 * that are part of a vertical recycler view. Implemented based on the solution described in
 * https://medium.com/@elye.project/smooth-cross-recyclingviews-swipe-cc2810e13e0a.
 */
class ImproveSwipeOnItemTouchListener : RecyclerView.OnItemTouchListener {

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        // Do nothing.
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN && rv.scrollState == RecyclerView.SCROLL_STATE_SETTLING) {
            rv.stopScroll()
        }
        return false
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // Do nothing.
    }
}
