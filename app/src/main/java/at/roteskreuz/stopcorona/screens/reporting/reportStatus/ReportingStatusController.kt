package at.roteskreuz.stopcorona.screens.reporting.reportStatus

import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.Constants.Misc.EMPTY_STRING
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.repositories.AgreementData
import at.roteskreuz.stopcorona.screens.base.epoxy.*
import at.roteskreuz.stopcorona.screens.base.epoxy.buttons.buttonType1
import at.roteskreuz.stopcorona.skeleton.core.utils.adapterProperty
import at.roteskreuz.stopcorona.utils.formatDayAndMonthAndYear
import at.roteskreuz.stopcorona.utils.getBoldSpan
import at.roteskreuz.stopcorona.utils.string
import at.roteskreuz.stopcorona.utils.view.safeMap
import com.airbnb.epoxy.EpoxyController
import org.threeten.bp.ZonedDateTime

/**
 * Contains the UI elements of the sickness certification reporting status screen.
 */
class ReportingStatusController(
    private val context: Context,
    private val onAgreementCheckboxChange: (Boolean) -> Unit,
    private val onSendReportClick: () -> Unit
) : EpoxyController() {

    var agreementData: AgreementData? by adapterProperty(null as AgreementData?)
    var messageType: MessageType? by adapterProperty(null as MessageType?)
    var dateOfFirstSelfDiagnose: ZonedDateTime? by adapterProperty(null as ZonedDateTime?)

    override fun buildModels() {
        emptySpace(modelCountBuiltSoFar, 12)

        when (messageType) {
            MessageType.InfectionLevel.Red -> buildScreenForProvenSickness()
            MessageType.InfectionLevel.Yellow -> buildScreenForSelfTestSuspicion()
            MessageType.Revoke -> buildScreenForRevokeSuspicion()
        }

        emptySpace(modelCountBuiltSoFar, 40)
    }

    private fun buildScreenForProvenSickness() {
        headlineH1 {
            id("headline")
            text(context.string(R.string.certificate_report_status_headline))
        }

        emptySpace(modelCountBuiltSoFar, 32)

        description {
            id("description_agreement")
            val builder = SpannableStringBuilder()
            builder.append(context.getString(R.string.certificate_report_status_description_1))
            builder.append(context.getBoldSpan(R.string.certificate_report_status_description_2, insertLeadingSpace = false))
            builder.append(context.getString(R.string.certificate_report_status_description_3))

            description(SpannableString.valueOf(builder))
        }

        emptySpace(modelCountBuiltSoFar, 50)

        headlineH2 {
            id("headline_agreement_description")
            title(context.string(R.string.certificate_report_status_agreement_description))
        }

        emptySpace(modelCountBuiltSoFar, 22)

        checkbox(onAgreementCheckboxChange) {
            id("checkbox_agreement")
            label(context.string(R.string.certificate_report_status_agreement_label))
            checked(agreementData?.userHasAgreed ?: false)
            textStyle(R.style.AppTheme_Heading2)
            spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount } // whole row
        }

        emptySpace(modelCountBuiltSoFar, 38)

        buttonType1(onSendReportClick) {
            id("button_report_infection")
            text(context.string(R.string.certificate_report_status_button))
            enabled(agreementData?.userHasAgreed ?: false)
        }
    }

    private fun buildScreenForSelfTestSuspicion() {
        headlineH1 {
            id("headline")
            text(context.string(R.string.questionnaire_report_status_headline))
        }

        emptySpace(modelCountBuiltSoFar, 32)

        description {
            id("description_agreement")
            val builder = SpannableStringBuilder()
            builder.append(context.getString(R.string.questionnaire_report_status_description_1))
            builder.append(context.getBoldSpan(R.string.questionnaire_report_status_description_2, insertLeadingSpace = false))
            builder.append(context.getString(R.string.questionnaire_report_status_description_3))

            description(SpannableString.valueOf(builder))
        }

        emptySpace(modelCountBuiltSoFar, 84)


        checkbox(onAgreementCheckboxChange) {
            id("checkbox_agreement")
            label(context.string(R.string.questionnaire_report_status_agreement))
            checked(agreementData?.userHasAgreed ?: false)
            textStyle(R.style.AppTheme_Heading2)
            spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount } // whole row
        }

        emptySpace(modelCountBuiltSoFar, 38)

        buttonType1(onSendReportClick) {
            id("button_report_infection")
            text(context.string(R.string.questionnaire_report_status_button))
            enabled(agreementData?.userHasAgreed ?: false)
        }
    }

    private fun buildScreenForRevokeSuspicion() {
        headlineH1 {
            id("headline")
            text(context.string(R.string.revoke_suspicion_headline))
        }

        emptySpace(modelCountBuiltSoFar, 32)



        description {
            id("description_revoke")
            description(SpannableString(context.string(
                R.string.revoke_suspicion_description,
                dateOfFirstSelfDiagnose?.formatDayAndMonthAndYear(context).safeMap("Self diagnose date not available!", EMPTY_STRING)
            )))
        }

        emptySpace(modelCountBuiltSoFar, 84)

        checkbox(onAgreementCheckboxChange) {
            id("checkbox_revoke_agreement")
            label(context.string(R.string.revoke_suspicion_approval))
            checked(agreementData?.userHasAgreed ?: false)
            textStyle(R.style.AppTheme_Heading2)
            spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount } // whole row
        }

        emptySpace(modelCountBuiltSoFar, 38)

        buttonType1(onSendReportClick) {
            id("button_revoke_suspicion")
            text(context.string(R.string.revoke_suspicion_action))
            enabled(agreementData?.userHasAgreed ?: false)
        }
    }
}
