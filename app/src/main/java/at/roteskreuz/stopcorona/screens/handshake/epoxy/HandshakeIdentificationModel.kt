package at.roteskreuz.stopcorona.screens.handshake.epoxy

import android.widget.TextView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

/**
 * Model for the random identification number
 */
@EpoxyModelClass(layout = R.layout.handshake_identification_epoxy_model)
abstract class HandshakeIdentificationModel : BaseEpoxyModel<HandshakeIdentificationModel.Holder>() {

    @EpoxyAttribute
    var identification: String? = null

    override fun Holder.onBind() {
        txtIdentification.text = identification
    }

    class Holder : BaseEpoxyHolder() {
        val txtIdentification by bind<TextView>(R.id.txtIdentification)
    }
}
