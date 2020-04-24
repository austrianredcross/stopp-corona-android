package at.roteskreuz.stopcorona.screens.base.epoxy

import android.widget.TextView
import androidx.annotation.StringRes
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

/**
 * Model for text with the style [R.style.AppTheme_ListTime].
 */
@EpoxyModelClass(layout = R.layout.base_list_item_text_epoxy_model)
abstract class ListItemTextModel : BaseEpoxyModel<ListItemTextModel.Holder>() {

    @EpoxyAttribute
    @StringRes
    var text: Int = R.string.certificate_personal_data_description

    override fun Holder.onBind() {
        txtDescription.setText(text)
    }

    class Holder : BaseEpoxyHolder() {
        val txtDescription by bind<TextView>(R.id.txtDescription)
    }
}