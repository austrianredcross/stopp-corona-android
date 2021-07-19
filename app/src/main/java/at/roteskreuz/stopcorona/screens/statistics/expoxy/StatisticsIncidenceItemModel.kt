package at.roteskreuz.stopcorona.screens.statistics.expoxy

import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.statistics.StatisticIncidenceItem
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import kotlinx.android.synthetic.main.statistics_incidence_item_expoxy_model.view.*

/**
 * UI component to display statistic incidence.
 */
@EpoxyModelClass(layout = R.layout.statistics_incidence_item_expoxy_model)
abstract class StatisticsIncidenceItemModel : BaseEpoxyModel<StatisticsIncidenceItemModel.Holder>() {

    @EpoxyAttribute
    var data: StatisticIncidenceItem? = null

    override fun Holder.onBind() {
        data?.let { data ->
            txtText.text = data.text
            txtValue.text = data.value
            data.diff?.let {
                txtDiff.text = data.diff
                txtDiff.visibility = View.VISIBLE
            } ?: run {
                txtDiff.visibility = View.GONE
            }
            val params =
                LayoutParams(
                    0, LayoutParams.MATCH_PARENT
                )
            data.icon?.let {
                imgIcon.imgIcon.setImageResource(data.icon)
                imgIcon.visibility = View.VISIBLE
                params.weight = 0.5f
                valueContainer.gravity = Gravity.START
            } ?: run {
                imgIcon.visibility = View.GONE
                params.weight = 0.8f
                valueContainer.gravity = Gravity.END
            }
            valueContainer.layoutParams = params
            viewColor.setBackgroundResource(data.colorDrawable)
        }
    }

    class Holder : BaseEpoxyHolder() {
        val viewColor by bind<View>(R.id.viewColor)
        val txtText by bind<TextView>(R.id.txtText)
        val imgIcon by bind<ImageView>(R.id.imgIcon)
        val txtValue by bind<TextView>(R.id.txtValue)
        val txtDiff by bind<TextView>(R.id.txtDiff)
        val valueContainer by bind<LinearLayout>(R.id.valueContainer)
    }
}