package at.roteskreuz.stopcorona.screens.history

import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.epoxy.copyText
import at.roteskreuz.stopcorona.screens.base.epoxy.emptySpace
import at.roteskreuz.stopcorona.screens.base.epoxy.listItemText
import at.roteskreuz.stopcorona.screens.history.epoxy.contactEventTableItem
import at.roteskreuz.stopcorona.screens.history.epoxy.contactHistoryTableHeader
import at.roteskreuz.stopcorona.skeleton.core.utils.adapterProperty
import at.roteskreuz.stopcorona.utils.getBoldSpan
import com.airbnb.epoxy.EpoxyController

/**
 * Contains the UI components of the contact events history screen.
 */
class ContactHistoryController(
    private val context: Context
) : EpoxyController() {

    var listOfNearbyRecords: List<NearbyRecordWrapper>? by adapterProperty(null as List<NearbyRecordWrapper>?)

    override fun buildModels() {
        emptySpace(modelCountBuiltSoFar, 20)

        copyText {
            id("contact_history_description")
            val builder = SpannableStringBuilder()
            builder.append(context.getBoldSpan(R.string.contact_history_description_1, insertLeadingSpace = false))
            builder.append(context.getString(R.string.contact_history_description_2))
            text(SpannableString.valueOf(builder))
        }

        emptySpace(modelCountBuiltSoFar, 32)

        contactHistoryTableHeader {
            id("contact_history_header")
        }

        if (listOfNearbyRecords?.isNotEmpty() == true) {
            listOfNearbyRecords?.forEachIndexed { index, nearbyRecord ->
                contactEventTableItem {
                    id("contact_history_table_item_$index")
                    timestamp(nearbyRecord.record.timestamp)
                    automaticMode(nearbyRecord.record.detectedAutomatically)
                    backgroundColor(
                        when (index % 2) {
                            0 -> R.color.white
                            else -> R.color.whiteGray
                        }
                    )
                }
            }
        } else {
            emptySpace(modelCountBuiltSoFar, 16)

            listItemText {
                id("description_no_records")
                text(R.string.contact_history_no_records)
            }
        }
    }
}