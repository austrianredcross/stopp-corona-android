package at.roteskreuz.stopcorona.screens.base.epoxy.buttons

import android.widget.Button
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

/**
 * Button type 1.
 */
@EpoxyModelClass(layout = R.layout.base_button_type1_epoxy_model)
abstract class ButtonType1Model(
    private val onClick: () -> Unit
) : BaseEpoxyModel<ButtonType1Model.Holder>() {

    @EpoxyAttribute
    var text: String? = null

    @EpoxyAttribute
    var enabled: Boolean = true

    @EpoxyAttribute
    var contentDescription: String? = null

    override fun Holder.onBind() {
        btnType1.text = text
        btnType1.isEnabled = enabled
        btnType1.contentDescription = contentDescription
        btnType1.setOnClickListener { onClick() }
    }

    override fun Holder.onUnbind() {
        btnType1.setOnClickListener(null)
    }

    class Holder : BaseEpoxyHolder() {
        val btnType1 by bind<Button>(R.id.btnType1)
    }
}