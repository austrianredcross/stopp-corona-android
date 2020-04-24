package at.roteskreuz.stopcorona.screens.history.epoxy

import android.widget.TextView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import at.roteskreuz.stopcorona.skeleton.core.utils.invisible
import at.roteskreuz.stopcorona.utils.formatDayAndMonthAndYear
import at.roteskreuz.stopcorona.utils.formatHandshakeShortVersion
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import org.threeten.bp.ZonedDateTime

/**
 * A row of the contact events history table.
 */
@EpoxyModelClass(layout = R.layout.contact_event_table_item_epoxy_model)
abstract class ContactEventTableItemModel : BaseEpoxyModel<ContactEventTableItemModel.Holder>() {

    @EpoxyAttribute
    var timestamp: ZonedDateTime? = null

    @EpoxyAttribute
    var index: Int? = null

    @EpoxyAttribute
    var automaticMode: Boolean = false

    @EpoxyAttribute
    var backgroundColor: Int = R.color.white

    override fun Holder.onBind() {
        val timestamp = timestamp
        if (timestamp != null) {
            txtDate.text = timestamp.formatDayAndMonthAndYear(context, monthAsText = false)
            txtTime.text = timestamp.formatHandshakeShortVersion(context)
        } else {
            txtDate.text = null
            txtTime.text = null
        }

        txtAutomaticMode.invisible = automaticMode.not()
    }

    class Holder : BaseEpoxyHolder() {
        val txtDate by bind<TextView>(R.id.txtDate)
        val txtTime by bind<TextView>(R.id.txtTime)
        val txtAutomaticMode by bind<TextView>(R.id.txtAutomaticMode)
    }
}
