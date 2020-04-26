package at.roteskreuz.stopcorona.screens.base.epoxy

import android.view.Gravity
import android.widget.TextView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

/**
 * Model for title-style headline
 */
@EpoxyModelClass(layout = R.layout.title_epoxy_model)
abstract class TitleModel : BaseEpoxyModel<TitleModel.Holder>() {

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
