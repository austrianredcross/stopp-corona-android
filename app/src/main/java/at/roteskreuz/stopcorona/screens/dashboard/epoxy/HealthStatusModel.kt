package at.roteskreuz.stopcorona.screens.dashboard.epoxy

import android.graphics.Typeface
import android.opengl.Visibility
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
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

    @EpoxyAttribute
    var redContactsDetected: Boolean = false

    @EpoxyAttribute
    var yellowContactsDetected: Boolean = false

    override fun Holder.onBind() {
        val healthStatusData: HealthStatusData? = data

        if (healthStatusData != null) {

            when (healthStatusData) {
                is HealthStatusData.SicknessCertificate -> {
                    txtDescription2Container.visibility = VISIBLE
                    txtTitle.text = context.string(R.string.sickness_certificate_attest_headline)
                    txtTitle.contentDescription = context.string(R.string.sickness_certificate_attest_headline) + context.getString(R.string.accessibility_heading_2)

                    val quarantinedUntil = healthStatusData.quarantineStatus.end.format(context.getString(R.string.general_date_format))
                    val quarantinedSpannable = SpannableString(quarantinedUntil)
                    quarantinedSpannable.setSpan(StyleSpan(Typeface.BOLD), 0, quarantinedSpannable.length, 0)

                    txtDescription.text = context.getString(R.string.sickness_certificate_attest_description)
                    txtDescription2.text = SpannableStringBuilder().apply {
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
                    txtDescription2Container.visibility = VISIBLE
                    val days = healthStatusData.quarantineStatus.daysUntilEnd()
                    txtTitle.text = context.string(R.string.self_testing_suspicion_headline)
                    txtTitle.contentDescription = context.string(R.string.self_testing_suspicion_headline) + context.getString(R.string.accessibility_heading_2)
                    txtDescription.text = context.getString(R.string.self_testing_suspicion_description)
                    txtDescription2.text = SpannableStringBuilder().apply {
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
                    var quarantinedUntil: String? = null
                    if (healthStatusData.quarantineStatus is QuarantineStatus.Jailed.Limited) {
                        quarantinedUntil = healthStatusData.quarantineStatus.end.format(context.getString(R.string.general_date_format))
                    }
                    val quarantinedSpannable = SpannableString(quarantinedUntil)
                    quarantinedSpannable.setSpan(StyleSpan(Typeface.BOLD), 0, quarantinedSpannable.length, 0)

                    if (redContactsDetected){
                        txtDescription2Container.visibility = VISIBLE
                        txtTitle.text = context.string(R.string.contacts_confirmed_one_case_headline)
                        txtTitle.contentDescription = context.string(R.string.health_status_contacts_confirmed_one_or_more_cases_headline) + context.getString(R.string.accessibility_heading_2)

                        txtDescription.text = SpannableStringBuilder().apply {
                            append(context.getString(R.string.contacts_confirmed_one_case_description))
                            append(context.getBoldSpan(R.string.contacts_confirmed_one_case_description_2))
                            append(context.getString(R.string.contacts_confirmed_one_case_description_3))
                        }
                        txtDescription2.text = SpannableStringBuilder().apply {
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

                    if (yellowContactsDetected){
                        txtTitle.text = context.string(R.string.contacts_suspicion_one_case_headline)
                        txtTitle.contentDescription = context.string(R.string.contacts_suspicion_one_case_headline) + context.getString(R.string.accessibility_heading_2)

                        txtDescription.text = SpannableStringBuilder().apply {
                            append(context.getString(R.string.contacts_suspicion_one_case_description))
                            append(context.getBoldSpan(R.string.contacts_suspicion_one_case_description_2))
                            append(context.getString(R.string.contacts_suspicion_one_case_description_3))
                        }
                        if (healthStatusData.warningType.redContactsDetected.not()){
                            txtDescription2Container.visibility = VISIBLE
                            txtDescription2.text = SpannableStringBuilder().apply {
                                append(context.getString(R.string.contacts_suspicion_one_case_description_4))
                                append(context.getBoldSpan(R.string.contacts_suspicion_one_case_description_5))
                                append(quarantinedSpannable)
                                append(" ")
                                append(context.getString(R.string.contacts_suspicion_one_case_description_6))
                            }
                        } else {
                            actionButtonContainer.visibility = GONE
                            separator.visibility = GONE
                        }

                        txtActionButton.text = quarantineDayActionText
                        imgHealthStatusIcon.setImageResource(R.drawable.ic_alert_white)
                        cardViewContainer.setCardBackgroundColor(color(R.color.orange))
                    }
                }
                else -> {
                    resetFields()
                }
            }

            if(healthStatusData is HealthStatusData.ContactsSicknessInfo && healthStatusData.warningType.redContactsDetected && yellowContactsDetected) {
                view.setOnClickListener(null)
            } else {
                view.setOnClickListener {

                    onClick(
                        healthStatusData
                    )
                }
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
        val actionButtonContainer by bind<ConstraintLayout>(R.id.actionButtonContainer)
        val txtDescription2Container by bind<LinearLayout>(R.id.txtDescription2Container)
        val txtDescription2 by bind<TextView>(R.id.txtDescription2)
        val separator by bind<View>(R.id.separator)
    }
}
