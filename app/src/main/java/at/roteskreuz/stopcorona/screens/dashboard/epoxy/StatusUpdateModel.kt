package at.roteskreuz.stopcorona.screens.dashboard.epoxy

import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.dashboard.CardUpdateStatus
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import at.roteskreuz.stopcorona.skeleton.core.utils.visible
import at.roteskreuz.stopcorona.utils.color
import at.roteskreuz.stopcorona.utils.tint
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

/**
 * UI component to display closable status update box.
 */
@EpoxyModelClass(layout = R.layout.dashboard_status_update_epoxy_model)
abstract class StatusUpdateModel(
    private val onCloseClick: () -> Unit
) : BaseEpoxyModel<StatusUpdateModel.Holder>() {

    @EpoxyAttribute
    var title: String? = null

    @EpoxyAttribute
    var description: String? = null

    @EpoxyAttribute
    var cardStatus: CardUpdateStatus? = null

    override fun Holder.onBind() {
        txtTitle.text = title
        txtDescription.text = description
        btnClose.setOnClickListener {
            onCloseClick()
        }

        when (cardStatus) {
            CardUpdateStatus.ContactUpdate -> {
                imgHealthStatusIcon.visible = false
                txtTitle.setTextColor(color(R.color.text_default_heading2))
                txtDescription.setTextColor(color(R.color.text_default_copy))
                cardViewContainer.setCardBackgroundColor(color(R.color.white))
                btnClose.tint(R.color.black)
            }

            CardUpdateStatus.EndOfQuarantine -> {
                imgHealthStatusIcon.visible = true
                txtTitle.setTextColor(color(R.color.white))
                txtDescription.setTextColor(color(R.color.white))
                cardViewContainer.setCardBackgroundColor(color(R.color.mediumGreen))
                btnClose.tint(R.color.white)
            }
        }
    }

    override fun Holder.onUnbind() {
        btnClose.setOnClickListener(null)
    }

    class Holder : BaseEpoxyHolder() {
        val txtTitle by bind<TextView>(R.id.txtTitle)
        val txtDescription by bind<TextView>(R.id.txtDescription)
        val btnClose by bind<ImageView>(R.id.btnClose)
        val imgHealthStatusIcon by bind<ImageView>(R.id.imgHealthStatusIcon)
        val cardViewContainer by bind<CardView>(R.id.cardViewContainer)
    }
}
