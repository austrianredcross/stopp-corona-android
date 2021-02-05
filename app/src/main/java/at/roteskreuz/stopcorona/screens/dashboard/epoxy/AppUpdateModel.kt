package at.roteskreuz.stopcorona.screens.dashboard.epoxy

import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

/**
 * UI component to display app status data.
 */
@EpoxyModelClass(layout = R.layout.dashboard_app_update_epoxy_model)
abstract class AppUpdateModel : BaseEpoxyModel<AppUpdateModel.Holder>() {

    @EpoxyAttribute
    @DrawableRes
    var imageRes: Int? = null

    @EpoxyAttribute
    var status: String? = null

    override fun Holder.onBind() {
        imageRes?.let { imgAppStatusIcon.setImageResource(it) }
        statusText.text = status
    }

    class Holder : BaseEpoxyHolder() {
        val imgAppStatusIcon by bind<ImageView>(R.id.imgAppStatusIcon)
        val statusText by bind<TextView>(R.id.txtText)
    }
}