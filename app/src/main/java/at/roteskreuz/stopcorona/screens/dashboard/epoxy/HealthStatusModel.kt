package at.roteskreuz.stopcorona.screens.dashboard.epoxy

import android.graphics.Typeface
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
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
import at.roteskreuz.stopcorona.utils.format
import at.roteskreuz.stopcorona.utils.color
import at.roteskreuz.stopcorona.utils.getBoldSpan
import at.roteskreuz.stopcorona.utils.string
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
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
                is HealthStatusData.SicknessCertificate -> {
                    txtTitle.text = context.string(R.string.sickness_certificate_attest_headline)
                    txtTitle.contentDescription = context.string(R.string.sickness_certificate_attest_headline) + context.getString(R.string.accessibility_heading_2)
                    txtDescription.text = context.string(R.string.sickness_certificate_attest_description)
                    val quarantinedUntil = healthStatusData.quarantineStatus.end.format(context.getString(R.string.general_date_format))
                    val quarantinedSpannable = SpannableString(quarantinedUntil)
                    quarantinedSpannable.setSpan(StyleSpan(Typeface.BOLD), 0, quarantinedSpannable.length, 0)

                    txtDescription.text = SpannableStringBuilder().apply {
                        append(context.getString(R.string.sickness_certificate_attest_description))
                        append("\n")
                        append(context.getString(R.string.sickness_certificate_attest_description_2))
                        append(context.getBoldSpan(R.string.sickness_certificate_attest_description_3))
                        append(quarantinedSpannable)
                        append(" ")
                        append(context.getString(R.string.sickness_certificate_attest_description_4))
                    }

                    txtActionButton.text = context.string(R.string.sickness_certificate_attest_button)
                    imgHealthStatusIcon.setImageResource(R.drawable.ic_alert_white)
                    cardViewContainer.setCardBackgroundColor(color(R.color.red))
                }
                is HealthStatusData.SelfTestingSuspicionOfSickness -> {
                    val days = healthStatusData.quarantineStatus.daysUntilEnd()
                    txtTitle.text = context.string(R.string.self_testing_suspicion_headline)
                    txtTitle.contentDescription = context.string(R.string.self_testing_suspicion_headline) + context.getString(R.string.accessibility_heading_2)
                    txtDescription.text = SpannableStringBuilder().apply {
                        append(context.getString(R.string.self_testing_suspicion_description))
                        append("\n")
                        append(context.getString(R.string.self_testing_suspicion_description_2))
                        append(context.getBoldSpan(R.string.self_testing_suspicion_description_3))
                        append(context.getString(R.string.self_testing_suspicion_description_4))
                    }
                    txtActionButton.text = when (days) {
                        1L -> string(R.string.contacts_quarantine_day_single)
                        else -> string(R.string.contacts_quarantine_day_many)
                    }
                    imgHealthStatusIcon.setImageResource(R.drawable.ic_alert_white)
                    cardViewContainer.setCardBackgroundColor(color(R.color.orange))
                }
                HealthStatusData.SelfTestingSymptomsMonitoring -> {
                    txtTitle.text = context.string(R.string.self_testing_symptoms_headline)
                    txtTitle.contentDescription = context.string(R.string.self_testing_symptoms_headline) + context.getString(R.string.accessibility_heading_2)
                    txtDescription.text = context.string(R.string.self_testing_symptoms_description)
                    txtActionButton.text = context.string(R.string.self_testing_symptoms_button)
                    imgHealthStatusIcon.setImageResource(R.drawable.ic_alert_white)
                    cardViewContainer.setCardBackgroundColor(color(R.color.orange))
                }
                is HealthStatusData.ContactsSicknessInfo -> {
                    val days = if (healthStatusData.quarantineStatus is QuarantineStatus.Jailed.Limited) {
                        healthStatusData.quarantineStatus.daysUntilEnd()
                    } else {
                        Timber.e(SilentError("HealthStatusData.ContactsSicknessInfo must have QuarantineStatus.Jailed.Limited"))
                        0L
                    }
                    val quarantineDayActionText = when (days) {
                        1L -> string(R.string.contacts_quarantine_day_single)
                        else -> string(R.string.contacts_quarantine_day_many)
                    }
                    when {
                        healthStatusData.warningType.redContactsDetected && healthStatusData.warningType.yellowContactsDetected.not() -> {
                            txtTitle.text = context.string(R.string.contacts_confirmed_one_case_headline)
                            txtTitle.contentDescription = context.string(R.string.health_status_contacts_confirmed_one_or_more_cases_headline) + context.getString(R.string.accessibility_heading_2)

                            var quarantinedUntil: String? = ""
                            if (healthStatusData.quarantineStatus is QuarantineStatus.Jailed.Limited) {
                                quarantinedUntil = healthStatusData.quarantineStatus.end.format(context.getString(R.string.general_date_format))
                            }
                            val quarantinedSpannable = SpannableString(quarantinedUntil)
                            quarantinedSpannable.setSpan(StyleSpan(Typeface.BOLD), 0, quarantinedSpannable.length, 0)

                            txtDescription.text = SpannableStringBuilder().apply {
                                append(context.getString(R.string.contacts_confirmed_one_case_description))
                                append(context.getBoldSpan(R.string.contacts_confirmed_one_case_description_2))
                                append(context.getString(R.string.contacts_confirmed_one_case_description_3))
                                append("\n")
                                append(context.getString(R.string.sickness_certificate_attest_description_2))
                                append(context.getBoldSpan(R.string.sickness_certificate_attest_description_3))
                                append(quarantinedSpannable)
                                append(" ")
                                append(context.getString(R.string.sickness_certificate_attest_description_4))
                            }
                            txtActionButton.text = quarantineDayActionText
                            imgHealthStatusIcon.setImageResource(R.drawable.ic_alert_white)
                            cardViewContainer.setCardBackgroundColor(color(R.color.red))
                        }
                        healthStatusData.warningType.redContactsDetected && healthStatusData.warningType.yellowContactsDetected -> {
                            txtTitle.text = context.string(R.string.health_status_contacts_confirmed_and_suspicion_one_or_more_cases_headline)
                            txtTitle.contentDescription = context.string(R.string.health_status_contacts_confirmed_and_suspicion_one_or_more_cases_headline) + context.getString(R.string.accessibility_heading_2)
                            txtDescription.text =
                                context.string(R.string.health_status_contacts_confirmed_and_suspicion_one_or_more_cases_description)
                            txtActionButton.text = quarantineDayActionText
                            imgHealthStatusIcon.setImageResource(R.drawable.ic_alert_white)
                            cardViewContainer.setCardBackgroundColor(color(R.color.red))
                        }
                        healthStatusData.warningType.redContactsDetected.not() && healthStatusData.warningType.yellowContactsDetected -> {
                            txtTitle.text = context.string(R.string.contacts_suspicion_one_case_headline)
                            txtTitle.contentDescription = context.string(R.string.contacts_suspicion_one_case_headline) + context.getString(R.string.accessibility_heading_2)
                            txtDescription.text = context.string(R.string.contacts_suspicion_one_case_description)
                            txtDescription.text = SpannableStringBuilder().apply {
                                append(context.getString(R.string.contacts_suspicion_one_case_description))
                                append(context.getBoldSpan(R.string.contacts_suspicion_one_case_description_2))
                                append(context.getString(R.string.contacts_suspicion_one_case_description_3))
                            }
                            txtActionButton.text = quarantineDayActionText
                            imgHealthStatusIcon.setImageResource(R.drawable.ic_alert_white)
                            cardViewContainer.setCardBackgroundColor(color(R.color.orange))
                        }
                    }
                }
                else -> {
                    resetFields()
                }
            }

            view.setOnClickListener {

                onClick(
                    healthStatusData
                )
            }
        } else {
            resetFields()
        }

        val visibleQuarantine =
            (healthStatusData is HealthStatusData.ContactsSicknessInfo && ownHealthStatus !is HealthStatusData.SicknessCertificate) ||
                    healthStatusData is HealthStatusData.SelfTestingSuspicionOfSickness
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
        val cardViewContainer by bind<CardView>(R.id.cardViewContainer)
    }
}
