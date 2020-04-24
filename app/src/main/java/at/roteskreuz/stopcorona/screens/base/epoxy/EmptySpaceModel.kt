package at.roteskreuz.stopcorona.screens.base.epoxy

import android.view.View
import androidx.annotation.ColorRes
import androidx.core.view.updateLayoutParams
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import at.roteskreuz.stopcorona.skeleton.core.utils.dip
import at.roteskreuz.stopcorona.utils.backgroundColor
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyModelClass

/**
 * Model for configurable empty space.
 */
@EpoxyModelClass(layout = R.layout.base_empty_space_epoxy_model)
abstract class EmptySpaceModel : BaseEpoxyModel<EmptySpaceModel.Holder>() {

    /**
     * Height of the empty space in dp.
     */
    @EpoxyAttribute
    var height: Int = 0

    @EpoxyAttribute
    @ColorRes
    var backgroundColor: Int = R.color.transparent

    override fun Holder.onBind() {
        emptyView.updateLayoutParams {
            height = context.dip(this@EmptySpaceModel.height)
        }
        emptyView.backgroundColor(backgroundColor)
    }

    class Holder : BaseEpoxyHolder() {
        val emptyView by bind<View>(R.id.emptyView)
    }
}

/**
 * Helper fun to add empty space with defined height (in dp).
 * @param idSuffix Must be `modelCountBuiltSoFar`, because we need stable ids during rebuild.
 */
@JvmOverloads
fun EpoxyController.emptySpace(
    idSuffix: Int,
    height: Int,
    modelInitializer: (EmptySpaceModelBuilder.() -> Unit)? = null
) {
    emptySpace {
        id("empty_space_${idSuffix}")
        height(height)
        modelInitializer?.invoke(this)
        spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount } // whole row
    }
}