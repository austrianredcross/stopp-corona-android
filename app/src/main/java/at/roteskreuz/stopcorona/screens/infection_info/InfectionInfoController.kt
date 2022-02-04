package at.roteskreuz.stopcorona.screens.infection_info

import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.Constants.Misc.EMPTY_STRING
import at.roteskreuz.stopcorona.screens.base.epoxy.*
import at.roteskreuz.stopcorona.screens.infection_info.epoxy.GuideInfoRedModel_
import at.roteskreuz.stopcorona.screens.infection_info.epoxy.GuideInfoYellowModel_
import at.roteskreuz.stopcorona.utils.format
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
                builder.append(context.string(R.string.infection_info_suspicion_description_3))
            } else if (data.combinedWarningType.redContactsDetected && data.combinedWarningType.yellowContactsDetected) {
                builder.append(context.string(R.string.infection_info_confirmed_description_1))
                builder.append(context.getBoldSpan(R.string.infection_info_confirmed_description_2))
                builder.append(context.string(R.string.infection_info_confirmed_description_3))
                builder.append("\n\n")
                builder.append(context.string(R.string.infection_info_suspicion_description_1))
                builder.append(context.getBoldSpan(R.string.infection_info_suspicion_description_2))
                builder.append(context.string(R.string.infection_info_suspicion_description_3))
            }
            description(SpannableString.valueOf(builder))
        }

        emptySpace(modelCountBuiltSoFar, 24)

        if (data.combinedWarningType.redContactsDetected.not() && data.combinedWarningType.yellowContactsDetected) {
            verticalBackgroundModelGroup(
                listOf(
                    GuideInfoYellowModel_(
                        onPhoneNumberClick,
                        data.quarantinedUntil?.format(context.getString(R.string.general_date_format))
                    )
                        .id("guide")
                )
            ) {
                backgroundColor(R.color.primary)
            }
        } else {
            verticalBackgroundModelGroup(
                listOf(
                    GuideInfoRedModel_(
                        onPhoneNumberClick,
                        data.quarantinedUntil?.format(context.getString(R.string.general_date_format)),
                        data.lastRedContactDate?.get().format(context.getString(R.string.general_date_format))
                    )
                        .id("guide")
                )
            ) {
                backgroundColor(R.color.primary)
            }
        }


        emptySpace(modelCountBuiltSoFar, 40)
    }
}