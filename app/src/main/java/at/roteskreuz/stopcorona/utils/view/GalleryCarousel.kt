package at.roteskreuz.stopcorona.utils.view

import com.airbnb.epoxy.Carousel
import android.content.Context
import android.util.AttributeSet
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.SnapHelper
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.OnViewRecycled

/**
 * Carousel to match image gallery specifics.
 * Snapping is behaving like viewPager.
 *
 * Use [PreloadingGalleryCarousel] for image preloading preloading support
 */
@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
open class GalleryCarousel : PrebindingCarousel {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var decorator: CirclePagerIndicatorDecoration? = null

    /**
     * Default page snapping like viewPager.
     */
    override fun getSnapHelperFactory(): Carousel.SnapHelperFactory? {
        return object : Carousel.SnapHelperFactory() {
            override fun buildSnapHelper(context: Context?): SnapHelper {
                return PagerSnapHelper()
            }
        }
    }

    @ModelProp(ModelProp.Option.DoNotHash)
    fun setItemIndicator(decorator: CirclePagerIndicatorDecoration?) {
        clearItemDecorator()
        if (decorator != null) {
            addItemDecoration(decorator)
            this.decorator = decorator
        }
    }

    @ModelProp
    fun setGalleryHeight(height: Int) {
        if (height == 0) return
        updateLayoutParams {
            this.height = height
        }
    }

    /**
     * Remove item decoration when the view is recycled to prevent unwanted behaviour.
     */
    @OnViewRecycled
    fun clearItemDecorator() {
        decorator?.let { removeItemDecoration(it) }
        decorator = null
    }
}
