package at.roteskreuz.stopcorona.screens.dashboard.epoxy

import android.widget.TextView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.dashboard.HealthStatusData
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import at.roteskreuz.stopcorona.skeleton.core.utils.visible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

/**
 * UI component to display the handshake headline by health state.
 */
@EpoxyModelClass(layout = R.layout.dashboard_risk_headline_epoxy_model)
abstract class RiskHeadlineModel : BaseEpoxyModel<RiskHeadlineModel.Holder>() {

    @EpoxyAttribute
    var active: Boolean = false

    @EpoxyAttribute
    var data: HealthStatusData = HealthStatusData.NoHealthStatus

    override fun Holder.onBind() {
        val healthStatusData: HealthStatusData = data

        txtTitle.visible = active
        txtDisabled.visible = !active
        txtDisabled.text = context.getString(R.string.main_automatic_handshake_disabled_info)

        if (active){
            var title = context.getString(R.string.main_automatic_handshake_no_risk_headline)
            when(healthStatusData) {
                is HealthStatusData.SicknessCertificate -> {
                    title = context.getString(R.string.main_automatic_handshake_self_infection_headline)
                    with(txtTitle) {
                        isEnabled = false
                        isActivated = false
                    }
                }
                is HealthStatusData.SelfTestingSuspicionOfSickness -> {
                    title = context.getString(R.string.main_automatic_handshake_self_suspicion_headline)
                    with(txtTitle) {
                        isEnabled = true
                        isActivated = false
                    }
                }
                is HealthStatusData.ContactsSicknessInfo -> {
                    if (healthStatusData.warningType.redContactsDetected) {
                        title = context.getString(R.string.main_automatic_handshake_contact_risk_headline)
                        with(txtTitle) {
                            isEnabled = false
                            isActivated = false
                        }
                    } else {
                        title = context.getString(R.string.main_automatic_handshake_suspicion_risk_headline)
                        with(txtTitle) {
                            isEnabled = true
                            isActivated = false
                        }
                    }
                }
                else -> {
                    with(txtTitle) {
                        isEnabled = true
                        isActivated = true
                    }
                }
            }

            txtTitle.text = title
            txtTitle.contentDescription = title + context.getString(R.string.accessibility_heading_1)
        }
    }

    class Holder : BaseEpoxyHolder() {
        val txtTitle by bind<TextView>(R.id.txtTitle)
        val txtDisabled by bind<TextView>(R.id.txtDisabled)
    }
}
