package at.roteskreuz.stopcorona.screens.statistics.expoxy

import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.entities.statistics.Bundesland
import at.roteskreuz.stopcorona.model.entities.statistics.CovidStatistics
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import at.roteskreuz.stopcorona.utils.view.AustriaView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

/**
 * UI component to display statistic austria card.
 */
@EpoxyModelClass(layout = R.layout.statistics_austria_view_epoxy_model)
abstract class StatisticAustriaViewModel : BaseEpoxyModel<StatisticAustriaViewModel.Holder>() {

    @EpoxyAttribute
    var state: Bundesland = Bundesland.Oesterreich

    @EpoxyAttribute
    var statistics: CovidStatistics? = null

    override fun Holder.onBind() {
        austriaView.setStatistics(statistics)
        austriaView.setState(state.value)
        austriaView.invalidate()
    }

    class Holder : BaseEpoxyHolder() {
        val austriaView by bind<AustriaView>(R.id.austriaView)
    }
}