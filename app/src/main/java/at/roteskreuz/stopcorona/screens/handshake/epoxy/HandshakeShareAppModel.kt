package at.roteskreuz.stopcorona.screens.handshake.epoxy

import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import com.airbnb.epoxy.EpoxyModelClass

/**
 * UI component to display closable status update box.
 */
@EpoxyModelClass(layout = R.layout.handshake_share_app_epoxy_model)
abstract class HandshakeShareAppModel(
    private val onShareClick: () -> Unit
) : BaseEpoxyModel<HandshakeShareAppModel.Holder>() {

    override fun Holder.onBind() {
        view.setOnClickListener { onShareClick() }
    }

    override fun Holder.onUnbind() {
        view.setOnClickListener(null)
    }

    class Holder : BaseEpoxyHolder()
}
