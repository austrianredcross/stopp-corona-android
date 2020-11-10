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

fun PeriodicKey.toTemporaryExposureKey(): TemporaryExposureKey {

    return TemporaryExposureKey.TemporaryExposureKeyBuilder()
        .setKeyData(content)
        .setRollingStartIntervalNumber(periodicKeyValidTime.toInt())
        .setRollingPeriod(periodicKeyLifeTime.toInt())
        .setTransmissionRiskLevel(initialRiskLevel)
        .setReportType(reportType)
        .build()
}

fun ContactSketch.toExposureSummary(): ExposureSummary {
    return ExposureSummary.ExposureSummaryBuilder()
        .setDaysSinceLastExposure(daysSinceLastHit)
        .setMatchedKeyCount(numberOfHits)
        .setMaximumRiskScore(maxRiskValue)
        .setSummationRiskScore(summationRiskValue)
        .setAttenuationDurations(attenuationDurations)
        .build();

}

fun ContactDetail.toExposureInformation(): ExposureInformation {
    return ExposureInformationBuilder()
        .setDateMillisSinceEpoch(Instant.EPOCH.plus(dayNumber, ChronoUnit.DAYS).toEpochMilli())
        .setAttenuationValue(attenuationRiskValue)
        .setTransmissionRiskLevel(initialRiskLevel)
        .setDurationMinutes(durationMinutes)
        .setAttenuationDurations(attenuationDurations)
        .setTotalRiskScore(totalRiskValue)
        .build()
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