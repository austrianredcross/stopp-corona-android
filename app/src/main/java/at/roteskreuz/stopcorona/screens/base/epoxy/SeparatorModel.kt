package at.roteskreuz.stopcorona.screens.base.epoxy

import android.view.View
import androidx.annotation.ColorRes
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import at.roteskreuz.stopcorona.utils.backgroundColor
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

/**
 * Model for a separator line.
 */
@EpoxyModelClass(layout = R.layout.base_separator_epoxy_model)
abstract class SeparatorModel : BaseEpoxyModel<SeparatorModel.Holder>() {

    @ColorRes
    @EpoxyAttribute
    var color: Int = R.color.white

    override fun Holder.onBind() {
        separator.backgroundColor(color)
    }

    class Holder : BaseEpoxyHolder() {
        val separator by bind<View>(R.id.separator)
    }
}