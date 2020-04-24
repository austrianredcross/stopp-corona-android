package at.roteskreuz.stopcorona.screens.questionnaire.compliance

import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.epoxy.buttons.buttonType1
import at.roteskreuz.stopcorona.screens.base.epoxy.checkbox
import at.roteskreuz.stopcorona.screens.base.epoxy.copyText
import at.roteskreuz.stopcorona.screens.base.epoxy.emptySpace
import at.roteskreuz.stopcorona.screens.base.epoxy.headlineH1
import at.roteskreuz.stopcorona.skeleton.core.utils.adapterProperty
import at.roteskreuz.stopcorona.utils.getClickableSpan
import at.roteskreuz.stopcorona.utils.string
import com.airbnb.epoxy.EpoxyController

class QuestionnaireComplianceController(
    private val context: Context,
    private val onAgreementCheckboxChange: (Boolean) -> Unit,
    private val onDataPrivacyClick: () -> Unit,
    private val onContinueClick: () -> Unit
) : EpoxyController() {

    var continueButtonEnabled: Boolean by adapterProperty(false)

    override fun buildModels() {
        emptySpace(modelCountBuiltSoFar, 24)

        headlineH1 {
            id("questionnaire_compliance_headline")
            text(context.string(R.string.questionnaire_compliance_headline))
        }

        emptySpace(modelCountBuiltSoFar, 24)

        copyText {
            id("questionnaire_compliance_description_1")
            text(SpannableString(context.string(R.string.questionnaire_compliance_description_1)))
        }

        emptySpace(modelCountBuiltSoFar, 24)

        checkbox(onAgreementCheckboxChange) {
            id("questionnaire_compliance_checkbox")
            textStyle(R.style.AppTheme_Heading2)
            label(context.string(R.string.questionnaire_compliance_approval))
        }

        emptySpace(modelCountBuiltSoFar, 24)

        copyText {
            id("questionnaire_compliance_description_2")
            text(SpannableString(context.string(R.string.questionnaire_compliance_description_2)))
        }

        emptySpace(modelCountBuiltSoFar, 24)

        copyText {
            id("questionnaire_compliance_description_3")

            val builder = SpannableStringBuilder()
            builder.append(context.string(R.string.questionnaire_compliance_description_3))
            builder.append(context.getClickableSpan(
                textRes = R.string.questionnaire_compliance_description_4,
                insertTrailingSpace = false,
                onClick = { onDataPrivacyClick() })
            )
            builder.append(".")
            text(SpannableString.valueOf(builder))
        }

        emptySpace(modelCountBuiltSoFar, 80)

        buttonType1(onContinueClick) {
            id("questionnaire_compliance_accept_button")
            text(context.string(R.string.general_continue))
            enabled(continueButtonEnabled)
        }

        emptySpace(modelCountBuiltSoFar, 56)
    }
}
