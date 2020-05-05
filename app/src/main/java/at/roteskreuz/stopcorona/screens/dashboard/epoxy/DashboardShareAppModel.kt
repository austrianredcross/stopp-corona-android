package at.roteskreuz.stopcorona.screens.dashboard.epoxy

import android.widget.ImageView
import android.widget.TextView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import com.airbnb.epoxy.EpoxyModelClass

/**
 * UI component to display closable status update box.
 */
@EpoxyModelClass(layout = R.layout.dashboard_share_app_epoxy_model)
abstract class DashboardShareAppModel(
    private val onShareClick: () -> Unit
) : BaseEpoxyModel<DashboardShareAppModel.Holder>() {

    override fun Holder.onBind() {
        txtAction.setOnClickListener { onShareClick() }
        imgArrow.setOnClickListener { onShareClick() }
    }

    override fun Holder.onUnbind() {
        txtAction.setOnClickListener(null)
        imgArrow.setOnClickListener(null)
    }

    class Holder : BaseEpoxyHolder() {
        val txtAction by bind<TextView>(R.id.txtAction)
        val imgArrow by bind<ImageView>(R.id.imgArrow)
    }
}
