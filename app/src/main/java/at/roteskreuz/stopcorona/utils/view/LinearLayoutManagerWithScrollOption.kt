package at.roteskreuz.stopcorona.utils.view

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LinearLayoutManagerWithScrollOption(
    context: Context,
    orientation: Int = RecyclerView.HORIZONTAL,
    reverseLayout: Boolean = false
) : LinearLayoutManager(context, orientation, reverseLayout) {

    /**
     * Enable or disable scrolling of recycler view.
     */
    var scrollable = false

    override fun canScrollVertically() = super.canScrollVertically() && scrollable
    override fun canScrollHorizontally() = super.canScrollHorizontally() && scrollable
}
