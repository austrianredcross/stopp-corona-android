package at.roteskreuz.stopcorona.screens.infection_info

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.Constants.Misc.EMPTY_STRING
import at.roteskreuz.stopcorona.screens.base.epoxy.*
import at.roteskreuz.stopcorona.screens.infection_info.epoxy.GuideInfoModel_
import at.roteskreuz.stopcorona.utils.formatDayAndMonth
import at.roteskreuz.stopcorona.utils.getBoldSpan
import at.roteskreuz.stopcorona.utils.string
import com.airbnb.epoxy.TypedEpoxyController

/**
 * Screen with indications based on the health state of the contacts you've been exposed to.
 */
class InfectionInfoController(
    private val context: Context,
    private val onPhoneNumberClick: (phoneNumber: String) -> Unit
) : TypedEpoxyController<InfectedContactsViewState>() {

    override fun buildModels(data: InfectedContactsViewState) {
        emptySpace(modelCountBuiltSoFar, 22)

        headlineH1 {
            id("headline")
            text(
                if (data.combinedWarningType.redContactsDetected && data.combinedWarningType.yellowContactsDetected.not()) {
                    context.string(R.string.infection_info_confirmed_headline)
                } else if (data.combinedWarningType.redContactsDetected.not() && data.combinedWarningType.yellowContactsDetected) {
                    context.string(R.string.infection_info_suspicion_headline)
                } else if (data.combinedWarningType.redContactsDetected && data.combinedWarningType.yellowContactsDetected) {
                    context.string(R.string.infection_info_confirmed_and_suspicion_headline)
                } else {
                    EMPTY_STRING
                }
            )
        }

        emptySpace(modelCountBuiltSoFar, 24)

            description {
                id("red_contacts")
                val builder = SpannableStringBuilder()
                if (data.combinedWarningType.redContactsDetected && data.combinedWarningType.yellowContactsDetected.not()) {
                    builder.append(context.string(R.string.infection_info_confirmed_description_1))
                    builder.append(context.getBoldSpan(R.string.infection_info_confirmed_description_2))
                    builder.append(context.string(R.string.infection_info_confirmed_description_3))
                } else if (data.combinedWarningType.redContactsDetected.not() && data.combinedWarningType.yellowContactsDetected) {
                    builder.append(context.string(R.string.infection_info_suspicion_description_1))
                    builder.append(context.getBoldSpan(R.string.infection_info_suspicion_description_2))
                } else if (data.combinedWarningType.redContactsDetected && data.combinedWarningType.yellowContactsDetected) {
                    builder.append(context.string(R.string.infection_info_confirmed_description_1))
                    builder.append(context.getBoldSpan(R.string.infection_info_confirmed_description_2))
                    builder.append(context.string(R.string.infection_info_confirmed_description_3))
                    builder.append("\n\n")
                    builder.append(context.string(R.string.infection_info_suspicion_description_1))
                    builder.append(context.getBoldSpan(R.string.infection_info_suspicion_description_2))
                }
                description(SpannableString.valueOf(builder))
            }

        emptySpace(modelCountBuiltSoFar, 24)

        verticalBackgroundModelGroup(listOf(
            EmptySpaceModel_()
                .id(modelCountBuiltSoFar)
                .height(24),
            DescriptionModel_()
                .id("go_to_quarantine")
                .description(SpannableString(
                    if (data.combinedWarningType.redContactsDetected) context.getString(R.string.infection_info_go_to_quarantine)
                    else context.getString(R.string.infection_info_warning_go_to_quarantine)
                ).apply { setSpan(StyleSpan(Typeface.BOLD), 0, length, 0) }),
            EmptySpaceModel_()
                .id(modelCountBuiltSoFar)
                .height(24),
            DescriptionModel_()
                .id("quarantine")
                .description(SpannableString(
                    context.getString(R.string.infection_info_quarantine_end,
                        data.quarantinedUntil?.formatDayAndMonth(context)
                    )
                ).apply { setSpan(StyleSpan(Typeface.BOLD), 0, length, 0) })
                .textColor(R.color.accent),
            EmptySpaceModel_()
                .id(modelCountBuiltSoFar)
                .height(24),
            DescriptionModel_()
                .id("do_not_go_outside")
                .description(SpannableString(
                    if (data.combinedWarningType.redContactsDetected) context.getString(R.string.infection_info_no_public_transport)
                    else context.getString(R.string.infection_info_warning_no_public_transport)
                )),
            GuideInfoModel_(onPhoneNumberClick)
                .id("guide")
        )) {
            backgroundColor(R.color.background_gray)
        }
        emptySpace(modelCountBuiltSoFar, 40)
    }
}