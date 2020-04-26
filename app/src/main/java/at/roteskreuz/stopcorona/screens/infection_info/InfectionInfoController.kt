package at.roteskreuz.stopcorona.screens.infection_info

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.epoxy.*
import at.roteskreuz.stopcorona.screens.infection_info.epoxy.GuideInfoModel_
import at.roteskreuz.stopcorona.utils.formatDayAndMonth
import at.roteskreuz.stopcorona.utils.formatHandshakeLongVersion
import at.roteskreuz.stopcorona.utils.string
import com.airbnb.epoxy.TypedEpoxyController

/**
 * Add your KDoc here.
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
                if (data.yellowMessages.isEmpty()) {
                    if (data.redMessages.size == 1) {
                        context.string(R.string.infection_info_headline_single)
                    } else {
                        context.string(R.string.infection_info_headline_many, data.redMessages.size)
                    }
                } else if (data.redMessages.isEmpty()) {
                    if (data.yellowMessages.size == 1) {
                        context.string(R.string.infection_info_warning_headline_single)
                    } else {
                        context.string(R.string.infection_info_warning_headline_many, data.yellowMessages.size)
                    }
                } else {
                    context.string(R.string.infection_info_headline_many, data.messages.size)
                }
            )
        }

        emptySpace(modelCountBuiltSoFar, 24)

        if (data.redMessages.isNotEmpty()) {
            description {
                id("red_contacts")
                val evidence = context.string(R.string.infection_info_description_acording_evidence)
                val text = SpannableString(
                    if (data.redMessages.size == 1) context.string(R.string.infection_info_description1_single, evidence)
                    else context.string(R.string.infection_info_description1_many, data.redMessages.size, evidence)
                )
                val indexEvidence = text.indexOf(evidence)
                text.setSpan(StyleSpan(Typeface.BOLD), indexEvidence, indexEvidence + evidence.length, 0)
                description(text)
            }

            emptySpace(modelCountBuiltSoFar, 24)

            description {
                id("red_contacts_handshake_title")
                description(SpannableString(
                    if (data.redMessages.size == 1) {
                        context.string(R.string.infection_info_handshake_took_place_single)
                    } else {
                        context.string(R.string.infection_info_handshake_took_place_many)
                    }
                ))
            }

            data.redMessages.forEachIndexed { index, message ->
                description {
                    id("red_contacts_handshake_$index")
                    description(SpannableString(message.timeStamp.formatHandshakeLongVersion(context)))
                }
            }
        }

        if (data.yellowMessages.isNotEmpty()) {
            if (data.redMessages.isNotEmpty()) {
                emptySpace(modelCountBuiltSoFar, 24)
            }

            description {
                id("yellow_contacts")
                val evidence = context.string(R.string.infection_info_warning_explanation)
                val text = SpannableString(
                    if (data.yellowMessages.size == 1) context.string(R.string.infection_info_warning_description1_single, evidence)
                    else context.string(R.string.infection_info_warning_description1_many, data.yellowMessages.size, evidence)
                )
                val indexEvidence = text.indexOf(evidence)
                text.setSpan(StyleSpan(Typeface.BOLD), indexEvidence, indexEvidence + evidence.length, 0)
                description(text)
            }

            emptySpace(modelCountBuiltSoFar, 24)

            description {
                id("yellow_contacts_handshake_title")
                description(SpannableString(
                    if (data.yellowMessages.size == 1) {
                        context.string(R.string.infection_info_handshake_took_place_single)
                    } else {
                        context.string(R.string.infection_info_handshake_took_place_many)
                    }
                ))
            }

            data.yellowMessages.forEachIndexed { index, message ->
                description {
                    id("yellow_contacts_handshake_$index")
                    description(SpannableString(message.timeStamp.formatHandshakeLongVersion(context)))
                }
            }
        }

        emptySpace(modelCountBuiltSoFar, 24)

        verticalBackgroundModelGroup(listOf(
            EmptySpaceModel_()
                .id(modelCountBuiltSoFar)
                .height(24),
            DescriptionModel_()
                .id("go_to_quarantine")
                .description(SpannableString(
                    if (data.redMessages.isNotEmpty()) context.getString(R.string.infection_info_go_to_quarantine)
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
                    if (data.redMessages.isNotEmpty()) context.getString(R.string.infection_info_no_public_transport)
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