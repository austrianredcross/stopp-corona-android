package at.roteskreuz.stopcorona.screens.base.epoxy

import android.view.Gravity
import android.widget.TextView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

/**
 * Model for H2 headline
 */
@EpoxyModelClass(layout = R.layout.headline_h2_epoxy_model)
abstract class HeadlineH2Model : BaseEpoxyModel<HeadlineH2Model.Holder>() {

    @EpoxyAttribute
    var title: String? = null

    @EpoxyAttribute
    var gravity: Int = Gravity.START

    override fun Holder.onBind() {
        txtTitle.gravity = gravity
        txtTitle.text = title
    }

    class Holder : BaseEpoxyHolder() {
        val txtTitle by bind<TextView>(R.id.txtTitle)
    }
}
