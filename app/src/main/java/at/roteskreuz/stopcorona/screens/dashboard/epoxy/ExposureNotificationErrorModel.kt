package at.roteskreuz.stopcorona.screens.dashboard.epoxy

import android.widget.ImageView
import android.widget.TextView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import at.roteskreuz.stopcorona.skeleton.core.utils.visible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

/**
 * UI component to display error from exposure notification framework.
 */
@EpoxyModelClass(layout = R.layout.dashboard_exposure_notification_error_epoxy_model)
abstract class ExposureNotificationErrorModel(
    private val onClick: () -> Unit
) : BaseEpoxyModel<ExposureNotificationErrorModel.Holder>() {

    @EpoxyAttribute
    var title: String? = null

    @EpoxyAttribute
    var description: String? = null

    @EpoxyAttribute
    var action: String? = null

    override fun Holder.onBind() {
        txtTitle.text = title
        txtDescription.text = description
        txtAction.text = action
        txtAction.visible = action != null
        imgArrow.visible = action != null
        view.isEnabled = action != null
        view.setOnClickListener { onClick() }
    }

    override fun Holder.onUnbind() {
        view.setOnClickListener(null)
    }

    class Holder : BaseEpoxyHolder() {
        val txtTitle by bind<TextView>(R.id.txtTitle)
        val txtDescription by bind<TextView>(R.id.txtDescription)
        val txtAction by bind<TextView>(R.id.txtAction)
        val imgArrow by bind<ImageView>(R.id.imgArrow)
    }
}
