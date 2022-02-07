package at.roteskreuz.stopcorona.screens.dashboard.epoxy

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.cardview.widget.CardView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.dashboard.CardUpdateStatus
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import at.roteskreuz.stopcorona.skeleton.core.utils.visible
import at.roteskreuz.stopcorona.utils.*
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

/**
 * UI component to display status update box.
 */
@EpoxyModelClass(layout = R.layout.dashboard_status_update_epoxy_model)
abstract class StatusUpdateModel(
    private val clickAction: (() -> Unit)?
) : BaseEpoxyModel<StatusUpdateModel.Holder>() {

    @EpoxyAttribute
    var title: String? = null

    @EpoxyAttribute
    var description: String? = null

    @EpoxyAttribute
    var cardStatus: CardUpdateStatus? = null

    override fun Holder.onBind() {
        txtTitle.text = title
        txtTitle.contentDescription = title + context.getString(R.string.accessibility_heading_2)
        txtDescription.text = description

        when (cardStatus) {
            CardUpdateStatus.ContactUpdate -> {
                imgHealthStatusIcon.visible = false
                txtTitle.setTextColor(color(R.color.text_default_heading2))
                txtDescription.setTextColor(color(R.color.text_default_copy))
                cardViewContainer.setCardBackgroundColor(color(R.color.white))
                clickAction?.let { clickAction ->
                    btnClose.setOnClickListener {
                        clickAction()
                    }
                }
            }

            CardUpdateStatus.EndOfQuarantine -> {
                imgHealthStatusIcon.visible = true
                txtDescription2Container.visible = true
                txtTitle.setTextColor(color(R.color.white))
                txtDescription.setTextColor(color(R.color.white))
                cardViewContainer.setCardBackgroundColor(color(R.color.mediumGreen))
                setTextColor(R.color.dashboard_card_color)
                clickAction?.let { clickAction ->
                    btnClose.setOnClickListener {
                        clickAction()
                    }
                }
            }

            CardUpdateStatus.SunDowner -> {
                txtDescription2Container.visible = true
                txtDescription2.text = string(R.string.sunDowner_notification_content_2)
                txtTitle.setTextColor(color(R.color.white))
                txtDescription.setTextColor(color(R.color.white))
                cardViewContainer.setCardBackgroundColor(color(R.color.mediumGreen))
                setTextColor(R.color.dashboard_card_color)
                btnClose.visible = false
            }
        }
    }

    private fun Holder.setTextColor(@ColorRes color:Int){
        txtTitle.setTextColor(color(color))
        txtDescription.setTextColor(color(color))
        txtDescription2.setTextColor(color(color))
        imgHealthStatusIcon.tint(color)
        btnClose.tint(color)
        separator.setBackgroundColor(color(color))
    }

    override fun Holder.onUnbind() {
        btnClose.setOnClickListener(null)
    }

    class Holder : BaseEpoxyHolder() {
        val txtTitle by bind<TextView>(R.id.txtTitle)
        val txtDescription by bind<TextView>(R.id.txtDescription)
        val txtDescription2 by bind<TextView>(R.id.txtDescription2)
        val btnClose by bind<ImageView>(R.id.btnClose)
        val imgHealthStatusIcon by bind<ImageView>(R.id.imgHealthStatusIcon)
        val cardViewContainer by bind<CardView>(R.id.cardViewContainer)
        val txtDescription2Container by bind<LinearLayout>(R.id.txtDescription2Container)
        val separator by bind<View>(R.id.separator)
    }
}
