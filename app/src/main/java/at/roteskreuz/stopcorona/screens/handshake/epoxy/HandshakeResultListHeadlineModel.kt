package at.roteskreuz.stopcorona.screens.handshake.epoxy

import android.widget.ProgressBar
import android.widget.TextView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import at.roteskreuz.stopcorona.skeleton.core.utils.visible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

/**
 * Model for the result list headline with rounded corner background
 */
@EpoxyModelClass(layout = R.layout.handshake_resultlist_headline_epoxy_model)
abstract class HandshakeResultListHeadlineModel : BaseEpoxyModel<HandshakeResultListHeadlineModel.Holder>() {

    @EpoxyAttribute
    var title: String? = null

    @EpoxyAttribute
    var showLoadingIndicator: Boolean = false

    override fun Holder.onBind() {
        txtHeadline.text = title
        loadingIndicator.visible = showLoadingIndicator
    }

    class Holder : BaseEpoxyHolder() {
        val txtHeadline by bind<TextView>(R.id.txtTitle)
        val loadingIndicator by bind<ProgressBar>(R.id.loadingIndicator)
    }
}
