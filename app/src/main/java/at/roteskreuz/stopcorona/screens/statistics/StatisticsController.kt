package at.roteskreuz.stopcorona.screens.statistics

import android.content.Context
import android.graphics.Paint
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.entities.statistics.Bundesland
import at.roteskreuz.stopcorona.model.entities.statistics.CovidStatistics
import at.roteskreuz.stopcorona.screens.base.epoxy.*
import at.roteskreuz.stopcorona.screens.statistics.expoxy.StatisticCardModel_
import at.roteskreuz.stopcorona.screens.statistics.expoxy.statisticAustriaView
import at.roteskreuz.stopcorona.screens.statistics.expoxy.statisticsIncidenceItem
import at.roteskreuz.stopcorona.skeleton.core.utils.adapterProperty
import at.roteskreuz.stopcorona.skeleton.core.utils.addTo
import at.roteskreuz.stopcorona.skeleton.core.utils.color
import at.roteskreuz.stopcorona.skeleton.core.utils.dip
import at.roteskreuz.stopcorona.utils.formatDayAndMonth
import at.roteskreuz.stopcorona.utils.string
import at.roteskreuz.stopcorona.utils.view.CirclePagerIndicatorDecoration
import at.roteskreuz.stopcorona.utils.view.GalleryCarouselModel_
import com.airbnb.epoxy.Carousel
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyModel

class StatisticsController(
    private val context: Context,
    private val onLegendClick: () -> Unit
) : EpoxyController() {
    var statisticCardInfos: MutableList<StatisticCardInfo> by adapterProperty(mutableListOf())
    var statisticIncidenceItems: MutableList<StatisticIncidenceItem> by adapterProperty(mutableListOf())
    var statistics: CovidStatistics? by adapterProperty(null as CovidStatistics?)
    var currentDate: String? by adapterProperty(null as String?)
    var compareDate: String? by adapterProperty(null as String?)
    var selectedState: Bundesland by adapterProperty(Bundesland.Oesterreich)

    override fun buildModels() {
        buildStatisticsCard()

        emptySpace {
            id(modelCountBuiltSoFar)
            height(12)
        }

        separator {
            id(modelCountBuiltSoFar)
            color(R.color.dashboard_separator)
        }

        emptySpace {
            id(modelCountBuiltSoFar)
            height(40)
            backgroundColor(R.color.background_gray)
        }

        emptySpace {
            id(modelCountBuiltSoFar)
            height(32)
        }

        title {
            id("statistics_incidence_header_title")
            title(context.string(R.string.covid_statistics_incidence_title))
        }

        emptySpace {
            id(modelCountBuiltSoFar)
            height(10)
        }

        smallDescription {
            id("statistics_incidence_header_description")
            description(currentDate?.formatDayAndMonth(context)?.let {currentDate->
                compareDate?.formatDayAndMonth(context)?.let { compareDate ->
                    context.string(R.string.covid_statistics_incidence_comparison,
                        currentDate, compareDate
                    )
                }
            })
        }

        emptySpace {
            id(modelCountBuiltSoFar)
            height(32)
        }

        statisticAustriaView {
            id("statistics_austria_view")
            state(selectedState)
            statistics(statistics)
        }

        emptySpace {
            id(modelCountBuiltSoFar)
            height(32)
        }

        statisticIncidenceItems.forEachIndexed { index, statisticIncidenceItem ->
            statisticsIncidenceItem {
                id("statistic_incidence_item_$index")
                data(statisticIncidenceItem)
            }
        }

        emptySpace {
            id(modelCountBuiltSoFar)
            height(24)
        }

        additionalInformation(onLegendClick){
            id("statistic_legend")
            title(context.string(R.string.covid_statistics_incidence_legend))
            textColor(R.color.blue)
        }

        emptySpace {
            id(modelCountBuiltSoFar)
            height(24)
        }
    }

    private val pagerIndicator: CirclePagerIndicatorDecoration by lazy {
        CirclePagerIndicatorDecoration(
            context = context,
            colorActive = context.color(R.color.gray_4),
            colorInactive = context.color(R.color.gray_4),
            paddingBottom = context.dip(9),
            indicatorItemPadding = context.dip(8),
            indicatorItemDiameter = context.dip(15),
            indicatorInactivestyle = Paint.Style.STROKE
        )
    }

    private fun buildStatisticsCard() {
        val modelList = arrayListOf<EpoxyModel<out Any>>()

        val cardModelList = arrayListOf<EpoxyModel<out Any>>()

        statisticCardInfos.forEachIndexed { index, statisticCardInfo ->
            StatisticCardModel_()
                .id("statistic_card_$index")
                .data(statisticCardInfo)
                .addTo(cardModelList)
        }

        GalleryCarouselModel_()
            .id("statistics_carousel")
            .models(cardModelList)
            .padding(Carousel.Padding.dp(24,15))
            .itemIndicator(pagerIndicator)
            .addTo(modelList)

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(24)
            .addTo(modelList)

        verticalBackgroundModelGroup(modelList) {
            id("vertical_model_group_statistics")
            backgroundColor(R.color.background)
        }

    }
}