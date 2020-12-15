package at.roteskreuz.stopcorona.screens.dashboard.epoxy

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
        view.setOnClickListener { onShareClick() }
        txtTitle.contentDescription = context.getString(R.string.share_app_title) + context.getString(R.string.accessibility_heading_2)
    }

    override fun Holder.onUnbind() {
        view.setOnClickListener(null)
    }

    class Holder : BaseEpoxyHolder() {
        val txtTitle by bind<TextView>(R.id.txtTitle)
    }
}
