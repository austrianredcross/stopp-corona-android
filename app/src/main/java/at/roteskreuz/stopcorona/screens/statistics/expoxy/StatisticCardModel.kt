package at.roteskreuz.stopcorona.screens.statistics.expoxy

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.statistics.StatisticCardInfo
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

/**
 * UI component to display statistic card.
 */
@EpoxyModelClass(layout = R.layout.statistics_card_epoxy_model)
abstract class StatisticCardModel : BaseEpoxyModel<StatisticCardModel.Holder>() {

    @EpoxyAttribute
    var data: StatisticCardInfo? = null

    override fun Holder.onBind() {

        data?.let { data ->
            txtTitle1.text = data.firstContentText
            txtValue1.text = data.firstContentValue
            txtDiff1.text = data.firstContentDiff
            txtTitle2.text = data.secondContentText
            txtValue2.text = data.secondContentValue
            txtDiff2.text = data.secondContentDiff

            data.firstIcon?.let {
                imgStatisticIcon1.setImageResource(data.firstIcon)
                imgStatisticIcon1.visibility = View.VISIBLE
            } ?: run {
                imgStatisticIcon1.visibility = View.GONE
            }

            data.secondIcon?.let {
                imgStatisticIcon2.setImageResource(data.secondIcon)
                imgStatisticIcon2.visibility = View.VISIBLE
            } ?: run {
                imgStatisticIcon2.visibility = View.GONE
            }
        }
    }

    class Holder : BaseEpoxyHolder() {
        val txtTitle1 by bind<TextView>(R.id.txtTitle1)
        val txtValue1 by bind<TextView>(R.id.txtValue1)
        val txtDiff1 by bind<TextView>(R.id.txtDiff1)
        val imgStatisticIcon1 by bind<ImageView>(R.id.imgStatisticIcon1)
        val txtTitle2 by bind<TextView>(R.id.txtTitle2)
        val txtValue2 by bind<TextView>(R.id.txtValue2)
        val txtDiff2 by bind<TextView>(R.id.txtDiff2)
        val imgStatisticIcon2 by bind<ImageView>(R.id.imgStatisticIcon2)
    }
}