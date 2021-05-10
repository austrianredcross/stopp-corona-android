package at.roteskreuz.stopcorona.screens.diary.day

import android.content.Context
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.entities.diary.DbDiaryEntry
import at.roteskreuz.stopcorona.model.entities.diary.DbDiaryEntryWrapper
import at.roteskreuz.stopcorona.model.entities.diary.DiaryEntryType
import at.roteskreuz.stopcorona.screens.base.epoxy.*
import at.roteskreuz.stopcorona.screens.base.epoxy.buttons.ButtonType1Model_
import at.roteskreuz.stopcorona.screens.base.epoxy.buttons.buttonType1
import at.roteskreuz.stopcorona.screens.diary.epoxy.DiaryDayItemModel_
import at.roteskreuz.stopcorona.screens.diary.epoxy.diaryDayItem
import at.roteskreuz.stopcorona.skeleton.core.utils.adapterProperty
import at.roteskreuz.stopcorona.skeleton.core.utils.addTo
import at.roteskreuz.stopcorona.utils.format
import at.roteskreuz.stopcorona.utils.string
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyModel
import org.threeten.bp.LocalDate

class DiaryDayController(
    private val context: Context,
    private val onDeleteDiaryEntryClick: (id: Long) -> Unit,
    private val onAddDiaryEntryClick: () -> Unit
) : EpoxyController() {

    var entries: List<DbDiaryEntryWrapper> by adapterProperty(mutableListOf())

    override fun buildModels() {

        buildAddDiaryEntry()

        separator{
            id(modelCountBuiltSoFar)
            color(R.color.dashboard_separator)
        }


        emptySpace {
            id(modelCountBuiltSoFar)
            height(24)
        }

        if (entries.isNotEmpty()) {
            entries.forEachIndexed { index, entry ->
                when (entry.dbDiaryEntry.type) {
                    DiaryEntryType.PERSON -> {
                        diaryDayItem(onDeleteDiaryEntryClick) {
                            id("contact_person_$index")
                            entry(entry.dbDiaryEntry)
                            description(entry.dbPersonEntity?.fullName)
                            details(entry.dbPersonEntity?.notes)
                        }
                    }
                    DiaryEntryType.LOCATION -> {
                        diaryDayItem(onDeleteDiaryEntryClick) {
                            id("contact_location_$index")
                            entry(entry.dbDiaryEntry)
                            description(entry.dbLocationEntity?.locationName)
                            details(entry.dbLocationEntity?.timeOfDay)
                        }
                    }
                    DiaryEntryType.PUBLIC_TRANSPORT -> {
                        var description = ""

                        if (entry.dbDiaryPublicTransportEntity?.startLocation != null) {
                            description += entry.dbDiaryPublicTransportEntity.startLocation

                            if (entry.dbDiaryPublicTransportEntity.endLocation != null) {
                                description += " - "
                            }
                        }
                        if (entry.dbDiaryPublicTransportEntity?.endLocation != null) {
                            description += entry.dbDiaryPublicTransportEntity.endLocation
                        }

                        var time: String? = null
                        if (entry.dbDiaryPublicTransportEntity?.startTime != null) {
                            time =
                                entry.dbDiaryPublicTransportEntity.startTime.format(context.string(R.string.diary_time_format)) + " " + context.string(R.string.diary_hour)
                        }

                        diaryDayItem(onDeleteDiaryEntryClick) {
                            id("contact_public_transport_$index")
                            entry(entry.dbDiaryEntry)
                            description(entry.dbDiaryPublicTransportEntity?.description)
                            details(description)
                            time(time)
                        }
                    }
                    DiaryEntryType.EVENT -> {
                        var time: String? = null
                        if (entry.dbEventEntity?.startTime != null) {
                            time = entry.dbEventEntity.startTime.format(context.string(R.string.diary_time_format)) + " " + context.string(R.string.diary_hour)

                            if (entry.dbEventEntity.endTime != null) {
                                time += " - "
                            }
                        }
                        if (entry.dbEventEntity?.endTime != null) {
                            time += entry.dbEventEntity.endTime.format(context.string(R.string.diary_time_format)) + " " + context.string(R.string.diary_hour)
                        }

                        diaryDayItem(onDeleteDiaryEntryClick) {
                            id("contact_public_transport_$index")
                            description(entry.dbEventEntity?.description)
                            entry(entry.dbDiaryEntry)
                            details(time)
                        }
                    }
                }
            }
        } else {
            descriptionBlock {
                id("diary_day_empty")
                title(context.string(R.string.diary_new_entry_empty_title))
                description(context.string(R.string.diary_new_entry_empty_description))
            }
        }

        emptySpace {
            id(modelCountBuiltSoFar)
            height(8)
        }
    }

    private fun buildAddDiaryEntry() {
        val modelList = arrayListOf<EpoxyModel<out Any>>()

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(24)
            .addTo(modelList)

        ImageModel_()
            .id("diary_day_image")
            .imageRes(R.drawable.ic_diary)
            .addTo(modelList)

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(24)
            .addTo(modelList)

        ButtonType1Model_(onAddDiaryEntryClick)
            .id("diary_day_add_entry_button")
            .text("+ " + context.string(R.string.diary_new_entry_button))
            .addTo(modelList)

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(24)
            .addTo(modelList)

        verticalBackgroundModelGroup(modelList) {
            id("vertical_model_group_diary_dates")
            backgroundColor(R.color.background)
        }
    }
}