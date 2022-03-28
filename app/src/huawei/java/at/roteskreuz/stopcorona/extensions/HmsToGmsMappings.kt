package at.roteskreuz.stopcorona.extensions

import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import com.google.android.gms.nearby.exposurenotification.ExposureInformation.ExposureInformationBuilder
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.huawei.hms.contactshield.ContactDetail
import com.huawei.hms.contactshield.ContactSketch
import com.huawei.hms.contactshield.DiagnosisConfiguration
import com.huawei.hms.contactshield.PeriodicKey
import org.threeten.bp.Instant
import org.threeten.bp.temporal.ChronoUnit

private const val MAXIMUM_RISK_SCORE = 4096
private const val MAXIMUM_TRANSMISSION_LEVEL = 8
private const val MAXIMUM_ATTENUATION_VALUE = 255
private const val MAXIMUM_DURATION_MINUTES = 30

fun PeriodicKey.toTemporaryExposureKey(): TemporaryExposureKey {

    return TemporaryExposureKey.TemporaryExposureKeyBuilder()
        .setKeyData(content)
        .setRollingStartIntervalNumber(periodicKeyValidTime.toInt())
        .setRollingPeriod(periodicKeyLifeTime.toInt())
        .setTransmissionRiskLevel(initialRiskLevel.coerceIn(0, MAXIMUM_TRANSMISSION_LEVEL))
        .setReportType(reportType)
        .build()
}

fun ContactSketch.toExposureSummary(): ExposureSummary {
    return ExposureSummary.ExposureSummaryBuilder()
        .setDaysSinceLastExposure(daysSinceLastHit)
        .setMatchedKeyCount(numberOfHits)
        .setMaximumRiskScore(maxRiskValue.coerceIn(0,MAXIMUM_RISK_SCORE))
        .setSummationRiskScore(summationRiskValue)
        .setAttenuationDurations(attenuationDurations)
        .build();

}

fun ContactDetail.toExposureInformation(): ExposureInformation {
    return ExposureInformationBuilder()
        .setDateMillisSinceEpoch(Instant.EPOCH.plus(dayNumber, ChronoUnit.DAYS).toEpochMilli())
        .setAttenuationValue(attenuationRiskValue.coerceIn(0, MAXIMUM_ATTENUATION_VALUE))
        .setTransmissionRiskLevel(initialRiskLevel.coerceIn(0, MAXIMUM_TRANSMISSION_LEVEL))
        .setDurationMinutes(clampTo5Granularity(durationMinutes).coerceIn(0, MAXIMUM_DURATION_MINUTES))
        .setAttenuationDurations(attenuationDurations)
        .setTotalRiskScore(totalRiskValue.coerceIn(0, MAXIMUM_RISK_SCORE))
        .build()
}

private fun clampTo5Granularity(value : Int) : Int {
    val rest = value % 5
    return if(rest != 0) {
        value - rest + 5
    } else {
        value
    }
}

fun ExposureConfiguration.toDiagnosisConfiguration(): DiagnosisConfiguration {
    return DiagnosisConfiguration.Builder()
        .setMinimumRiskValueThreshold(minimumRiskScore)
        .setAttenuationRiskValues(*attenuationScores)
        .setDaysAfterContactedRiskValues(*daysSinceLastExposureScores)
        .setDurationRiskValues(*durationScores)
        .setInitialRiskLevelRiskValues(*transmissionRiskScores)
        .setAttenuationDurationThresholds(*durationAtAttenuationThresholds)
        .build()

}