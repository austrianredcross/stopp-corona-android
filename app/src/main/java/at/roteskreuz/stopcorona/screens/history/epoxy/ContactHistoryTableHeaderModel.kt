package at.roteskreuz.stopcorona.screens.history.epoxy

import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import com.airbnb.epoxy.EpoxyModelClass

/**
 * Header of the table with contact events.
 */
@EpoxyModelClass(layout = R.layout.contact_history_table_header_epoxy_model)
abstract class ContactHistoryTableHeaderModel :
    BaseEpoxyModel<ContactHistoryTableHeaderModel.Holder>() {

    override fun Holder.onBind() {
        // Do nothing.
    }

    class Holder : BaseEpoxyHolder() {
        // Bind nothing.
    }
}