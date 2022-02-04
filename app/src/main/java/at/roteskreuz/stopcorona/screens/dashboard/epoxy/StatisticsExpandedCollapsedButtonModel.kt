package at.roteskreuz.stopcorona.screens.dashboard.epoxy

import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.dashboard.HealthStatusData
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import at.roteskreuz.stopcorona.utils.color
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

/**
 * UI component to display the handshake headline by health state.
 */
@EpoxyModelClass(layout = R.layout.dashboard_statistics_expanded_collapsed_button_epoxy_model)
abstract class StatisticsExpandedCollapsedButtonModel(val onClick: () -> Unit) :
    BaseEpoxyModel<StatisticsExpandedCollapsedButtonModel.Holder>() {

    @EpoxyAttribute
    var expanded: Boolean = false

    override fun Holder.onBind() {
        if (expanded) {
            imgIcon.setImageResource(R.drawable.ic_minus)
            container.setBackgroundResource(R.drawable.statistics_expanded_button)
            txtActionButton.setTextColor(color(R.color.dashboard_statistics_expanded_text))
        } else {
            imgIcon.setImageResource(R.drawable.ic_plus)
            container.setBackgroundResource(R.drawable.statistics_collapsed_button)
            txtActionButton.setTextColor(color(R.color.dashboard_statistics_collapsed_text))
        }
        view.setOnClickListener { onClick() }
    }

    class Holder : BaseEpoxyHolder() {
        val imgIcon by bind<ImageView>(R.id.imgIcon)
        val container by bind<ConstraintLayout>(R.id.container)
        val txtActionButton by bind<TextView>(R.id.txtActionButton)
    }
}