package at.roteskreuz.stopcorona.utils.view

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.epoxy.ModelView
import kotlin.math.max

/**
 * A Carousel that prebinds one off-viewport view to reduce latency in image display
 *
 * As the default LinearLayoutManager only prebinds views during scroll, the first off screen image view will still have latency betwen rendering the
 * view and binding the image. Therfore this Carousel will use a layout manager that prebinds one off-viewport view by having one pixel extra
 * layout space.
 *
 * To add preloading, please use [PreloadingGalleryCarousel].
 *
 * This Carousel also has scroll protection when it is nested inside another [RecyclerView] and is used with a LinearLayoutManager.
 * It [requestDisallowInterceptTouchEvent] from it's parent when it detects the beginning of a scroll in it's layout direction
 */
@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
open class PrebindingCarousel : ScrollProtectedCarousel {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    /**
     * create LayoutManager exactly as [EpoxyRecyclerView] but use [PrebindingLayoutManager] instead of [LinearLayoutManager]
     *
     * @return A matching LayoutManager
     */
    override fun createLayoutManager(): LayoutManager {
        // Code copied verbatim from EpoxyRecyclerView.createLayoutManager, except using PrebindingLayoutManager instead of LinearLeyoutManager
        val layoutParams = layoutParams

        // 0 represents matching constraints in a LinearLayout or ConstraintLayout
        if (layoutParams.height == RecyclerView.LayoutParams.MATCH_PARENT || layoutParams.height == 0) {

            if (layoutParams.width == RecyclerView.LayoutParams.MATCH_PARENT || layoutParams.width == 0) {
                // If we are filling as much space as possible then we usually are fixed size
                setHasFixedSize(true)
            }

            // A sane default is a vertically scrolling linear layout
            return PrebindingLayoutManager(context)
        } else {
            // This is usually the case for horizontally scrolling carousels and should be a sane
            // default
            return PrebindingLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
    }
}

/**
 * Layout manager that always pre-binds the next, adjecant view.
 */
class PrebindingLayoutManager : LinearLayoutManager {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, @RecyclerView.Orientation orientation: Int, reverseLayout: Boolean) : super(context, orientation,
        reverseLayout)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    /**
     * Bind views up to at least one pixel outside the client area, or whatever LinearLayoutManager decides
     */
    override fun getExtraLayoutSpace(state: RecyclerView.State?): Int {
        return max(1, super.getExtraLayoutSpace(state))
    }
}
