package at.roteskreuz.stopcorona.utils.view

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Helper class for accurate (precise) scroll events.
 * You must use [LinearLayoutManagerAccurateOffset] or [GridLayoutManagerAccurateOffset].
 *
 * Instance of this class should be stored in the fragment for whole its live (ex. in immutable variable).
 */
class AccurateScrollListener(
    private val onScroll: (scrolledDistance: Int) -> Unit
) : RecyclerView.OnScrollListener() {

    private var scrolledDistance: Int = 0

    /**
     * Map child view indexes to its sizes in px.
     *
     * This height map is belonging to the layout manager measured heights of child views and
     * is stored here because the instance of [AccurateScrollListener] is kept
     * in the fragment for whole its live even the view is recreated.
     */
    private val childViewSizesMap = mutableMapOf<Int, Int>()

    /**
     * Get height of child view at position [index].
     */
    operator fun get(index: Int) = childViewSizesMap[index]

    /**
     * Set height of child view at position [index].
     */
    operator fun set(index: Int, childViewHeight: Int) {
        childViewSizesMap[index] = childViewHeight
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        scrolledDistance = recyclerView.computeVerticalScrollOffset()
        onScroll(scrolledDistance)
    }
}

/**
 * This implementation of layout manager improves the result of [computeVerticalScrollOffset] method.
 * In the original implementation the method guesses how height are items which are not visible
 * on the screen (above the viewport) by averaging heights of visible items. But it is not so
 * accurate as and it doesn't have to be true.
 * This improved implementation remembers all heights of all child views and returns true scrolled
 * distance from the top.
 */
class LinearLayoutManagerAccurateOffset(
    context: Context,
    private val accurateScrollListener: AccurateScrollListener
) : LinearLayoutManager(context, RecyclerView.VERTICAL, false) {

    /**
     * Enable or disable scrolling of recycler view.
     */
    var scrollable = true

    override fun canScrollVertically() = super.canScrollVertically() && scrollable

    override fun canScrollHorizontally() = super.canScrollHorizontally() && scrollable

    override fun onLayoutCompleted(state: RecyclerView.State?) {
        super.onLayoutCompleted(state)
        for (i in 0 until childCount) {
            getChildAt(i)?.let { child ->
                accurateScrollListener[getPosition(child)] = getDecoratedMeasuredHeight(child)
            }
        }
    }

    override fun computeVerticalScrollOffset(state: RecyclerView.State): Int {
        if (childCount == 0) return 0

        var scrolledY = 0
        getChildAt(0)?.let { firstChild ->
            val firstChildPosition = getPosition(firstChild)
            scrolledY = -getDecoratedTop(firstChild)
            for (i in 0 until firstChildPosition) {
                scrolledY += accurateScrollListener[i] ?: 0
            }
        }
        return scrolledY
    }
}

/**
 * This helper implementation of layout manager improves the result of [computeVerticalScrollOffset] method.
 * In the original implementation the method guesses how height are items which are not visible
 * on the screen (above the viewport) by averaging heights of visible items. But it is not so
 * accurate as and it doesn't have to be true.
 * This improved implementation remembers all heights of all child views and returns true scrolled
 * distance from the top.
 */
class GridLayoutManagerAccurateOffset(
    context: Context,
    spanCount: Int,
    private val accurateScrollListener: AccurateScrollListener
) : GridLayoutManager(context, spanCount) {

    /**
     * Enable or disable scrolling of recycler view.
     */
    var scrollable = true

    override fun canScrollVertically() = super.canScrollVertically() && scrollable

    override fun canScrollHorizontally() = super.canScrollHorizontally() && scrollable

    override fun onLayoutCompleted(state: RecyclerView.State?) {
        super.onLayoutCompleted(state)
        var spansSoFar = 0
        for (i in 0 until childCount) {
            getChildAt(i)?.let { child ->
                val position = getPosition(child)
                val spans = spanSizeLookup.getSpanSize(position)
                if (spans == spanCount) {
                    // if view is for full width of the row
                    accurateScrollListener[position] = getDecoratedMeasuredHeight(child)
                } else {
                    // measure height of the last view on the row
                    if (spansSoFar == 0) {
                        spansSoFar = spans
                    } else {
                        spansSoFar += spans
                        if (spansSoFar == spanCount) {
                            accurateScrollListener[position] = getDecoratedMeasuredHeight(child)
                            spansSoFar = 0
                        }
                    }
                }
            }
        }
    }

    override fun computeVerticalScrollOffset(state: RecyclerView.State): Int {
        if (childCount == 0) return 0

        var scrolledY = 0
        getChildAt(0)?.let { firstChild ->
            val firstChildPosition = getPosition(firstChild)
            scrolledY = -getDecoratedTop(firstChild)
            for (i in 0 until firstChildPosition) {
                scrolledY += accurateScrollListener[i] ?: 0
            }
        }
        return scrolledY
    }
}