package at.roteskreuz.stopcorona.utils

import at.roteskreuz.stopcorona.model.entities.infection.info.WarningType
import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import timber.log.Timber

/**
 * This implements the "risk algorithm". Determining from a list of exposures the latest Yellow
 * and RED exposure day.
 */
fun List<ExposureInformation>.extractLatestRedAndYellowContactDate(dailyRiskThreshold: Int): ExposureDates {
    val infectionMessagesDays =
        this.groupBy {
            //from https://stackoverflow.com/questions/51952984/how-can-i-convert-a-time-in-milliseconds-to-zoneddatetime
            val intervalStart = Instant.ofEpochMilli(it.dateMillisSinceEpoch)
            val dayStart = ZonedDateTime.ofInstant(intervalStart, ZoneId.systemDefault()).startOfTheDay()
            dayStart
        }
            .map { (dayOfExposures, listOfExposureInformationsOfThisDay) ->

                val totalRiskScoreOfTheDay = listOfExposureInformationsOfThisDay
                    .map { it.totalRiskScore }
                    .reduce { acc, totalRiskScore ->
                        acc + totalRiskScore
                    }

                val warningTypeOfThisDay: WarningType

                if (totalRiskScoreOfTheDay < dailyRiskThreshold) {
                    Timber.d("there is not enough risc ($totalRiskScoreOfTheDay) in " +
                        "this day ($dayOfExposures) we can skip it")
                    warningTypeOfThisDay = WarningType.GREEN
                } else {
                    Timber.d("we know at least the warningType of this day " +
                        "($dayOfExposures) is yellow because the risc was $totalRiskScoreOfTheDay")

                    val totalRedRiscScoreOfTheDay = listOfExposureInformationsOfThisDay
                        .filter { it.transmissionRiskLevel == WarningType.RED.transmissionRiskLevel }
                        .mapNotNull { it.totalRiskScore }
                        .takeIf { it.size > 0 }
                        ?.reduce { acc, totalRiskScore ->
                            acc + totalRiskScore
                        }

                    if ((totalRedRiscScoreOfTheDay ?: 0) >= dailyRiskThreshold) {
                        Timber.d("found at least one RED exposure " +
                            "(transmissionRiskLevel=${WarningType.RED.transmissionRiskLevel}) " +
                            "in $listOfExposureInformationsOfThisDay")
                        warningTypeOfThisDay = WarningType.RED
                    } else {
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
        .firstOrNull()
        ?.day

    val firstYellowDay = infectionMessagesDays
        .filter { it.warningType == WarningType.YELLOW }
        .sortedBy { it.day }
        .firstOrNull()
        ?.day
    return ExposureDates(firstRedDay = firstRedDay, firstYellowDay = firstYellowDay)
}

data class ExposureDates(
    val firstRedDay: ZonedDateTime?,
    val firstYellowDay: ZonedDateTime?
)

data class InfectionMessageDay(
    val day: ZonedDateTime,
    val exposureInformations: List<ExposureInformation>,
    val warningType: WarningType
)