package at.roteskreuz.stopcorona.screens.base.epoxy.buttons

import android.widget.Button
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

/**
 * Button type 2.
 */
@EpoxyModelClass(layout = R.layout.base_button_type2_epoxy_model)
abstract class ButtonType2Model(
    private val onClick: () -> Unit
) : BaseEpoxyModel<ButtonType2Model.Holder>() {

    @EpoxyAttribute
    var text: String? = null

    @EpoxyAttribute
    var enabled: Boolean = true

    /**
     * Called when [enabled] = false and user clicked on the view.
     */
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var onDisabledClick: () -> Unit = {}

    override fun Holder.onBind() {
        btnType2.text = text
        btnType2.isActivated = enabled
        btnType2.setOnClickListener {
            if (enabled) {
                onClick()
            } else {
                onDisabledClick()
            }
        }
    }

    override fun Holder.onUnbind() {
        btnType2.setOnClickListener(null)
    }

    class Holder : BaseEpoxyHolder() {
        val btnType2 by bind<Button>(R.id.btnType2)
    }
}