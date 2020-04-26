package at.roteskreuz.stopcorona.screens.base.epoxy

import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.widget.TextView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

/**
 * Model with text in copy text style
 */
@EpoxyModelClass(layout = R.layout.dashboard_copy_text_epoxy_model)
abstract class CopyTextModel : BaseEpoxyModel<CopyTextModel.Holder>() {

    @EpoxyAttribute
    var text: SpannableString? = null

    override fun Holder.onBind() {
        txtText.text = text
        txtText.movementMethod = LinkMovementMethod()
    }

    class Holder : BaseEpoxyHolder() {
        val txtText by bind<TextView>(R.id.txtText)
    }
}