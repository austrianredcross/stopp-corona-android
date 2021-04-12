package at.roteskreuz.stopcorona.screens.diary

import android.content.Context
import android.text.SpannableString
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.entities.diary.DbDiaryEntry
import at.roteskreuz.stopcorona.screens.base.epoxy.*
import at.roteskreuz.stopcorona.screens.base.epoxy.buttons.ButtonType1Model_
import at.roteskreuz.stopcorona.screens.diary.epoxy.DiaryItemModel_
import at.roteskreuz.stopcorona.screens.diary.epoxy.diaryItem
import at.roteskreuz.stopcorona.skeleton.core.utils.adapterProperty
import at.roteskreuz.stopcorona.skeleton.core.utils.addTo
import at.roteskreuz.stopcorona.utils.string
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyModel
import org.threeten.bp.LocalDate

class DiaryController(
    private val context: Context,
    private val onDiaryEntryClick: (date: LocalDate) -> Unit,
    private val onAdditionalInformationClick: () -> Unit
) : EpoxyController() {

    var dates: MutableMap<LocalDate, MutableList<DbDiaryEntry>> by adapterProperty(mutableMapOf())

    override fun buildModels() {

        emptySpace {
            id(modelCountBuiltSoFar)
            height(24)
        }

        description {
            id("diary_info_description")
            description(SpannableString(context.string(R.string.diary_overview_description)))
        }

        emptySpace {
            id(modelCountBuiltSoFar)
            height(16)
        }

        additionalInformation(onAdditionalInformationClick) {
            id("diary_additional_information")
            title(context.string(R.string.diary_overview_link))
            textColor(R.color.blue)
        }

        emptySpace {
            id(modelCountBuiltSoFar)
            height(24)
        }

        buildDiaryDates()
    }

    private fun buildDiaryDates() {
        val modelList = arrayListOf<EpoxyModel<out Any>>()

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(24)
            .addTo(modelList)

        var dateIndex = 0
        dates.forEach { (date, entries) ->
            DiaryItemModel_(onDiaryEntryClick)
                .id("diary_day_$dateIndex")
                .date(date)
                .count(entries.size)
                .addTo(modelList)
            dateIndex++
        }

        verticalBackgroundModelGroup(modelList) {
            id("vertical_model_group_diary_dates")
            backgroundColor(R.color.background_gray)
        }

    }

}