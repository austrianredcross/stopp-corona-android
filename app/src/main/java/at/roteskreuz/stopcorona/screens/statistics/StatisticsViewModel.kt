package at.roteskreuz.stopcorona.screens.statistics

import at.roteskreuz.stopcorona.model.entities.statistics.*
import at.roteskreuz.stopcorona.model.repositories.StatisticsRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import io.reactivex.Observable

class StatisticsViewModel(
    appDispatchers: AppDispatchers,
    val statisticsRepository: StatisticsRepository
) : ScopedViewModel(appDispatchers) {
    var selectedState: Bundesland = Bundesland.Oesterreich
    var statistics: CovidStatistics? = null
    var statisticCardInfos = mutableListOf<StatisticCardInfo>()
    var statisticIncidenceItems = mutableListOf<StatisticIncidenceItem>()

    fun updateSelectedState(state: Bundesland) {
        statisticsRepository.setSelectedState(state)
    }

    fun observeSelectedState(): Observable<Bundesland> {
        return statisticsRepository.observeSelectedState()
    }

    fun observeStatistics(): Observable<CovidStatistics> {
        return statisticsRepository.observeStatistics()
    }
}

data class StatisticCardInfo(
    val firstContentText: String,
    val firstContentValue: String,
    val firstContentDiff: String,
    val firstIcon: Int?,
    val secondContentText: String,
    val secondContentValue: String,
    val secondContentDiff: String,
    val secondIcon: Int?
)

data class StatisticIncidenceItem(
    val text: String,
    val value: String,
    val diff: String?,
    val icon: Int?,
    val colorDrawable: Int
)