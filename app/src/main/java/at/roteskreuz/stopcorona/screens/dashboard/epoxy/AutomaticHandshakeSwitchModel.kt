package at.roteskreuz.stopcorona.screens.dashboard.epoxy

import android.graphics.Color
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.repositories.CombinedExposureNotificationsState
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
    var state: CombinedExposureNotificationsState = CombinedExposureNotificationsState.Disabled

    @EpoxyAttribute
    var enabled: Boolean = true

    override fun Holder.onBind() {
        val checked = state in arrayOf(CombinedExposureNotificationsState.UserWantsItEnabled, CombinedExposureNotificationsState.ItIsEnabledAndRunning)
        txtTitle.text = title
        switch.isChecked = checked
        txtState.isEnabled = checked
        txtState.text = if (checked) stateTextEnabled else stateTextDisabled

        view.setOnClickListener {
            onCheckedChanged(checked.not())
        }

        view.isEnabled = enabled

        when (state) {
            CombinedExposureNotificationsState.UserWantsItEnabled -> TODO() //switch.currentTextColor = Color.GREEN
            CombinedExposureNotificationsState.ItIsEnabledAndRunning -> TODO()
            CombinedExposureNotificationsState.Disabled -> TODO()
        }
    }

    class Holder : BaseEpoxyHolder() {
        val txtTitle by bind<TextView>(R.id.txtTitle)
        val txtState by bind<TextView>(R.id.txtState)
        val switch by bind<SwitchCompat>(R.id.switchAutomaticHandshake)
    }
}
