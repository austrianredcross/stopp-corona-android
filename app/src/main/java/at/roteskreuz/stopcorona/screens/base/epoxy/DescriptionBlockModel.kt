package at.roteskreuz.stopcorona.screens.base.epoxy

import android.widget.TextView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

/**
 * Model with one title and one description text below.
 */
@EpoxyModelClass(layout = R.layout.base_description_block_epoxy_model)
abstract class DescriptionBlockModel : BaseEpoxyModel<DescriptionBlockModel.Holder>() {

    @EpoxyAttribute
    var title: String? = null

    @EpoxyAttribute
    var description: String? = null

    override fun Holder.onBind() {
        txtTitle.text = title
        txtTitle.contentDescription = title + context.getString(R.string.accessibility_heading_2)
        txtDescription.text = description
    }

    class Holder : BaseEpoxyHolder() {
        val txtTitle by bind<TextView>(R.id.txtTitle)
        val txtDescription by bind<TextView>(R.id.txtDescription)
    }
}