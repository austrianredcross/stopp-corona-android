package at.roteskreuz.stopcorona.screens.dashboard.epoxy

import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

@EpoxyModelClass(layout = R.layout.automatic_handshake_switch_epoxy_model)
abstract class AutomaticHandshakeSwitchModel(
    private val onCheckedChanged: (isChecked: Boolean) -> Unit
) : BaseEpoxyModel<AutomaticHandshakeSwitchModel.Holder>() {

    @EpoxyAttribute
    var title: String? = null

    @EpoxyAttribute
    var stateTextEnabled: String? = null

    @EpoxyAttribute
    var stateTextDisabled: String? = null

    @EpoxyAttribute
    var checked: Boolean = false

    @EpoxyAttribute
    var enabled: Boolean = true

    override fun Holder.onBind() {
        txtTitle.text = title
        switch.isChecked = checked
        txtState.isEnabled = checked
        txtState.text = if (checked) stateTextEnabled else stateTextDisabled

        view.setOnClickListener {
            onCheckedChanged(checked.not())
        }

        view.isEnabled = enabled
    }

    class Holder : BaseEpoxyHolder() {
        val txtTitle by bind<TextView>(R.id.txtTitle)
        val txtState by bind<TextView>(R.id.txtState)
        val switch by bind<SwitchCompat>(R.id.switchAutomaticHandshake)
    }
}
