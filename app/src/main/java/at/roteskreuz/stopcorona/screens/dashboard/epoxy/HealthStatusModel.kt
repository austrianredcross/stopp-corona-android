package at.roteskreuz.stopcorona.screens.dashboard.epoxy

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.repositories.QuarantineStatus
import at.roteskreuz.stopcorona.screens.dashboard.HealthStatusData
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import at.roteskreuz.stopcorona.skeleton.core.utils.visible
import at.roteskreuz.stopcorona.utils.color
import at.roteskreuz.stopcorona.utils.daysTo
import at.roteskreuz.stopcorona.utils.string
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import org.threeten.bp.ZonedDateTime
import timber.log.Timber

/**
 * UI component to display your health status (when sending a certificate or self examination).
 * Or the health status of your contacts.
 */
@EpoxyModelClass(layout = R.layout.health_status_epoxy_model)
abstract class HealthStatusModel(
    val onClick: (healthStatusData: HealthStatusData) -> Unit
) : BaseEpoxyModel<HealthStatusModel.Holder>() {

    @EpoxyAttribute
    var data: HealthStatusData? = null

    @EpoxyAttribute
    var ownHealthStatus: HealthStatusData = HealthStatusData.NoHealthStatus

    @EpoxyAttribute
    var title: String? = null

    @EpoxyAttribute
    var description: String? = null

    override fun Holder.onBind() {
        val healthStatusData: HealthStatusData? = data
        if (healthStatusData != null) {

            when (healthStatusData) {
                HealthStatusData.SicknessCertificate -> {
                    txtTitle.text = context.string(R.string.sickness_certificate_attest_headline)
                    txtDescription.text = context.string(R.string.sickness_certificate_attest_description)
                    txtActionButton.text = context.string(R.string.general_additional_information)
                    imgHealthStatusIcon.setImageResource(R.drawable.ic_checkmark_white_red)
                    cardViewContainer.setCardBackgroundColor(color(R.color.red))
                }
                is HealthStatusData.SelfTestingSuspicionOfSickness -> {
                    val days = healthStatusData.quarantineStatus.end.toLocalDate().daysTo(ZonedDateTime.now().toLocalDate())
                    txtTitle.text = context.string(R.string.self_testing_suspicion_headline)
                    txtDescription.text = context.string(R.string.self_testing_suspicion_description)
                    txtActionButton.text = if (days == 1L) string(R.string.contacts_quarantine_day_single)
                    else string(R.string.contacts_quarantine_day_many)
                    imgHealthStatusIcon.setImageResource(R.drawable.ic_alert_white)
                    txtQuarantineDays.text = days.toString()
                    cardViewContainer.setCardBackgroundColor(color(R.color.orange))
                }
                HealthStatusData.SelfTestingSymptomsMonitoring -> {
                    txtTitle.text = context.string(R.string.self_testing_symptoms_headline)
                    txtDescription.text = context.string(R.string.self_testing_symptoms_description)
                    txtActionButton.text = context.string(R.string.self_testing_symptoms_button)
                    imgHealthStatusIcon.setImageResource(R.drawable.ic_alert_white)
                    cardViewContainer.setCardBackgroundColor(color(R.color.orange))
                }
                is HealthStatusData.ContactsSicknessInfo -> {
                    val days = if (healthStatusData.quarantineStatus is QuarantineStatus.Jailed.Limited) {
                        healthStatusData.quarantineStatus.end.toLocalDate().daysTo(ZonedDateTime.now().toLocalDate())
                    } else {
                        Timber.e(SilentError("HealthStatusData.ContactsSicknessInfo must have QuarantineStatus.Jailed.Limited"))
                        0L
                    }

                    txtQuarantineDays.text = days.toString()
                    val quarantineDayActionText = if (days == 1L) string(R.string.contacts_quarantine_day_single)
                    else string(R.string.contacts_quarantine_day_many)

                    when {
                        healthStatusData.warningType.redContactsDetected && healthStatusData.warningType.yellowContactsDetected.not() -> {
                            txtTitle.text = context.string(R.string.health_status_contacts_confirmed_one_or_more_cases_headline)
                            txtDescription.text = context.string(R.string.health_status_contacts_confirmed_one_or_more_cases_description)
                            txtActionButton.text = quarantineDayActionText
                            imgHealthStatusIcon.setImageResource(R.drawable.ic_checkmark_white_red)
                            cardViewContainer.setCardBackgroundColor(color(R.color.red))
                        }
                        healthStatusData.warningType.redContactsDetected && healthStatusData.warningType.yellowContactsDetected -> {
                            txtTitle.text = context.string(R.string.health_status_contacts_confirmed_and_suspicion_one_or_more_cases_headline)
                            txtDescription.text =
                                context.string(R.string.health_status_contacts_confirmed_and_suspicion_one_or_more_cases_description)
                            txtActionButton.text = quarantineDayActionText
                            imgHealthStatusIcon.setImageResource(R.drawable.ic_alert_white)
                            cardViewContainer.setCardBackgroundColor(color(R.color.red))
                        }
                        healthStatusData.warningType.redContactsDetected.not() && healthStatusData.warningType.yellowContactsDetected -> {
                            txtTitle.text = context.string(R.string.health_status_contacts_suspicion_one_or_more_cases_headline)
                            txtDescription.text = context.string(R.string.health_status_contacts_suspicion_one_or_more_cases_description)
                            txtActionButton.text = quarantineDayActionText
                            imgHealthStatusIcon.setImageResource(R.drawable.ic_alert_white)
                            cardViewContainer.setCardBackgroundColor(color(R.color.orange))
                        }
                    }

                    if (ownHealthStatus == HealthStatusData.SicknessCertificate) {
                        txtActionButton.text = context.string(R.string.general_additional_information)
                    }
                }
                else -> {
                    resetFields()
                }
            }

            view.setOnClickListener {

                /**
                 * When the own health status is [SicknessCertificate] we override the status of [data]
                 */
                onClick(
                    when (ownHealthStatus) {
                        is HealthStatusData.SicknessCertificate -> HealthStatusData.SicknessCertificate
                        else -> healthStatusData
                    }
                )
            }
        } else {
            resetFields()
        }

        val visibleQuarantine =
            (healthStatusData is HealthStatusData.ContactsSicknessInfo && ownHealthStatus !is HealthStatusData.SicknessCertificate) || healthStatusData is HealthStatusData.SelfTestingSuspicionOfSickness
        txtQuarantineDays.visible = visibleQuarantine
        backgroundQuarantineDays.visible = visibleQuarantine
    }

    private fun Holder.resetFields() {
        txtTitle.text = null
        txtDescription.text = null
        txtActionButton.text = null
        view.setOnClickListener(null)
    }

    class Holder : BaseEpoxyHolder() {
        val txtTitle by bind<TextView>(R.id.txtTitle)
        val txtDescription by bind<TextView>(R.id.txtDescription)
        val txtActionButton by bind<TextView>(R.id.txtActionButton)
        val imgHealthStatusIcon by bind<ImageView>(R.id.imgHealthStatusIcon)
        val txtQuarantineDays by bind<TextView>(R.id.txtQuarantineDays)
        val backgroundQuarantineDays by bind<View>(R.id.backgroundQuarantineDays)
        val cardViewContainer by bind<CardView>(R.id.cardViewContainer)
    }
}
