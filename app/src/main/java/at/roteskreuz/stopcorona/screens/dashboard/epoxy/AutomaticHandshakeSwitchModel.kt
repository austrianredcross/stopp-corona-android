package at.roteskreuz.stopcorona.screens.dashboard.epoxy

import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.dashboard.CombinedExposureNotificationsState
import at.roteskreuz.stopcorona.screens.dashboard.CombinedExposureNotificationsState.*
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import at.roteskreuz.stopcorona.utils.string
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

@EpoxyModelClass(layout = R.layout.automatic_handshake_switch_epoxy_model)
abstract class AutomaticHandshakeSwitchModel(
    private val onCheckedChange: (isChecked: Boolean) -> Unit
) : BaseEpoxyModel<AutomaticHandshakeSwitchModel.Holder>() {

    @EpoxyAttribute
    var state: CombinedExposureNotificationsState = Disabled

    override fun Holder.onBind() {
        switch.isChecked = state is Enabled || state is EnabledWithError
        switch.setOnCheckedChangeListener(null)
        when (state) {
            Enabled -> {
                with(txtState) {
                    text = string(R.string.main_automatic_handshake_switch_on)
                    isEnabled = true
                    isActivated = true
                }
                with(switch) {
                    isActivated = true
                    isChecked = true
                }
            }
            is EnabledWithError -> {
                with(txtState) {
                    text = string(R.string.main_automatic_handshake_switch_paused)
                    isEnabled = true
                    isActivated = false
                }
                with(switch) {
                    isActivated = false
                    isChecked = true
                }
            }
            Disabled -> {
                with(txtState) {
                    text = string(R.string.main_automatic_handshake_switch_off)
                    isEnabled = false
                    isActivated = false
                }
                with(switch) {
                    isActivated = false
                    isChecked = false
                }
            }
        }

        view.setOnClickListener {
            switch.toggle()
        }

        switch.setOnCheckedChangeListener { _, isChecked ->
            onCheckedChange(isChecked)
        }
    }

    class Holder : BaseEpoxyHolder() {
        val txtState by bind<TextView>(R.id.txtState)
        val switch by bind<SwitchCompat>(R.id.switchAutomaticHandshake)
    }
}
