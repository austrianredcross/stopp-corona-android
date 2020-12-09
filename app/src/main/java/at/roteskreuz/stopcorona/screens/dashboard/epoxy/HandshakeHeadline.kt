package at.roteskreuz.stopcorona.screens.dashboard.epoxy

import android.widget.TextView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

@EpoxyModelClass(layout = R.layout.handshake_headline_epoxy_model)
abstract class HandshakeHeadline : BaseEpoxyModel<HandshakeHeadline.Holder>() {

    @EpoxyAttribute
    var title: String? = null

    override fun Holder.onBind() {
        txtTitle.text = title
        txtTitle.contentDescription = title + context.getString(R.string.accessibility_heading_2)
    }

    class Holder : BaseEpoxyHolder() {
        val txtTitle by bind<TextView>(R.id.txtTitle)
    }
}
