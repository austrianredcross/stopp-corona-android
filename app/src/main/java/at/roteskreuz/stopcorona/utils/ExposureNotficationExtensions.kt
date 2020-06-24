package at.roteskreuz.stopcorona.utils

import at.roteskreuz.stopcorona.model.entities.infection.info.WarningType
import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import org.threeten.bp.Instant
import timber.log.Timber

/**
 * This implements the "risk algorithm". Determining from a list of exposures the latest Yellow
 * and RED exposure day.
 */
fun List<ExposureInformation>.extractLatestRedAndYellowContactDate(dailyRiskThreshold: Int): ExposureDates {
    val infectionMessagesDays =
        groupBy {
            //from https://stackoverflow.com/questions/51952984/how-can-i-convert-a-time-in-milliseconds-to-zoneddatetime
            val intervalStart = Instant.ofEpochMilli(it.dateMillisSinceEpoch)
            val dayStart = intervalStart.startOfTheUtcDay()
            dayStart
        }
            .map { (dayOfExposures, listOfExposureInformationsOfThisDay) ->

                val totalRiskScoreOfTheDay = listOfExposureInformationsOfThisDay
                    .map { it.totalRiskScore }
                    .sum()

                val warningTypeOfThisDay: WarningType

                if (totalRiskScoreOfTheDay < dailyRiskThreshold) {
                    Timber.d("there is not enough risc ($totalRiskScoreOfTheDay) in " +
                        "this day ($dayOfExposures) we can skip it")
                    warningTypeOfThisDay = WarningType.GREEN
                } else {
                    Timber.d("we know the warningType of this day " +
                        "($dayOfExposures) is at least yellow because the risc was $totalRiskScoreOfTheDay")

                    val totalRedRiscScoreOfTheDay = listOfExposureInformationsOfThisDay
                        .filter { it.transmissionRiskLevel == WarningType.RED.transmissionRiskLevel }
                        .map { it.totalRiskScore }
                        .takeIf { it.isNotEmpty() }
                        ?.reduce { acc, totalRiskScore ->
                            acc + totalRiskScore
                        } ?: 0

                    if (totalRedRiscScoreOfTheDay >= dailyRiskThreshold) {
                        Timber.d("found enough risk in at least one RED exposure " +
                            "(transmissionRiskLevel=${WarningType.RED.transmissionRiskLevel}) " +
                            "in $listOfExposureInformationsOfThisDay")
                        warningTypeOfThisDay = WarningType.RED
                    } else {
                        Timber.d("Not enough risk in RED exposures found. Setting day to YELLOW")
                        warningTypeOfThisDay = WarningType.YELLOW
                    }
                }
                InfectionMessageDay(
                    day = dayOfExposures,
                    exposureInformations = listOfExposureInformationsOfThisDay,
                    warningType = warningTypeOfThisDay
                )
            }.sortedBy {
                it.day
            }

    val firstRedDay = infectionMessagesDays
        .filter { it.warningType == WarningType.RED }
        .minBy { it.day }
        ?.day

    val firstYellowDay = infectionMessagesDays
        .filter { it.warningType == WarningType.YELLOW }
        .minBy { it.day }
        ?.day
    return ExposureDates(firstRedDay = firstRedDay, firstYellowDay = firstYellowDay)
}

data class ExposureDates(
    val firstRedDay: Instant?,
    val firstYellowDay: Instant?
) {
    fun noDates(): Boolean {
        return firstRedDay == null && firstYellowDay == null
    }
}

data class InfectionMessageDay(
    val day: Instant,
    val exposureInformations: List<ExposureInformation>,
    val warningType: WarningType
)