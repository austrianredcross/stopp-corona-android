package at.roteskreuz.stopcorona

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import at.roteskreuz.stopcorona.commonexposure.CommonExposureClient
import at.roteskreuz.stopcorona.commonexposure.ExposureServiceStatus
import at.roteskreuz.stopcorona.extensions.*
import at.roteskreuz.stopcorona.model.HuaweiServiceStatus
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.huawei.hms.api.HuaweiApiAvailability
import com.huawei.hms.contactshield.*
import org.threeten.bp.Instant
import org.threeten.bp.ZoneOffset.UTC
import timber.log.Timber
import java.io.File

class HuaweiExposureClient(
    private val application: Application,
    private val huaweiApiAvailability: HuaweiApiAvailability,
    private val contactShieldEngine: ContactShieldEngine
) : CommonExposureClient {

    override suspend fun start() {

        Timber.tag(LOG_TAG).d("Trying to start HMS ContactShield...")
        contactShieldEngine.startContactShield(ContactShieldSetting.DEFAULT).await()

        Timber.tag(LOG_TAG).d("HMS ContactShield successfully started.")
    }

    override suspend fun stop() {

        Timber.tag(LOG_TAG).d("Trying to stop HMS ContactShield...")
        contactShieldEngine.stopContactShield().await()

        Timber.tag(LOG_TAG).d("HMS ContactShield successfully stopped.")

    }

    override suspend fun isRunning(): Boolean {

        Timber.tag(LOG_TAG).d("Checking if HMS ContactShield is running...")
        val isRunning: Boolean = contactShieldEngine.isContactShieldRunning.await()

        Timber.tag(LOG_TAG).d("HMS ContactShield running: $isRunning")
        return isRunning

    }

    override suspend fun getTemporaryExposureKeys(): List<TemporaryExposureKey> {
        val periodKeys = correctParametersToSameLikeGoogle(contactShieldEngine.periodicKey.await())
        return periodKeys.map { it.toTemporaryExposureKey() }
    }

    private fun correctParametersToSameLikeGoogle(periodicKey: List<PeriodicKey>) : List<PeriodicKey>{
        return periodicKey.map { k ->
            PeriodicKey.Builder()
                .setContent(k.content)
                .setInitialRiskLevel(k.initialRiskLevel)
                .setReportType(k.reportType)
                .setPeriodicKeyLifeTime(FULL_DAY_ROLLING_PERIOD)
                .setPeriodicKeyValidTime(mapToDayStartEpochMinutesTenFractions(k.periodicKeyValidTime))
                .build()
        }
    }

    private fun mapToDayStartEpochMinutesTenFractions(epochMinutesTenFractions : Long) : Long{
        val epochMilliSeconds = epochMinutesTenFractions * 10 * 60 * 1000
        val utcDate = Instant.ofEpochMilli(epochMilliSeconds).atZone(UTC).toLocalDate()
        return utcDate.atStartOfDay().toEpochSecond(UTC) / (10 * 60)
    }

    override suspend fun provideDiagnosisKeys(
        archives: List<File>,
        exposureConfiguration: ExposureConfiguration,
        token: String
    ) {
        val huaweiConfiguration: DiagnosisConfiguration =
            exposureConfiguration.toDiagnosisConfiguration()

        val pendingIntent = PendingIntent.getService(application, 0, Intent(application, ContactShieldIntentService::class.java), PendingIntent.FLAG_UPDATE_CURRENT)

        contactShieldEngine.putSharedKeyFiles(pendingIntent, archives, huaweiConfiguration, token).await()
    }

    override suspend fun getExposureSummary(token: String): ExposureSummary {
        return contactShieldEngine.getContactSketch(token).await().toExposureSummary()
    }

    override suspend fun getExposureInformation(token: String): List<ExposureInformation> {
        val contactDetails: List<ContactDetail> =
            contactShieldEngine.getContactDetail(token).await()
        return contactDetails.map { it.toExposureInformation() }
    }

    override fun getServiceStatus(): ExposureServiceStatus {

        val result: Int = huaweiApiAvailability.isHuaweiMobileServicesAvailable(application)
        Timber.tag(LOG_TAG).d("Huawei API availability status code: $result")

        return when (result) {
            0 -> HuaweiServiceStatus.Success(result)
            1 -> HuaweiServiceStatus.HmsCoreNotFound(result)
            2 -> HuaweiServiceStatus.OutOfDate(result)
            3 -> HuaweiServiceStatus.Unavailable(result)
            9 -> HuaweiServiceStatus.UnofficialVersion(result)
            21 -> HuaweiServiceStatus.DeviceTooOld(result)
            else -> HuaweiServiceStatus.UnknownStatus(result)

        }
    }

    override fun getServiceVersion(context : Context): String {
        return "Huawei Mobile Services: ${huaweiMobileServicesVersion(context)}";
    }

    override fun isGmsService(): Boolean {
        return false
    }

    private fun huaweiMobileServicesVersion(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(HuaweiApiAvailability.SERVICES_PACKAGE, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e, "Couldn't get the app version")
            "Not Available"
        }
    }

    companion object {
        private const val LOG_TAG = "HuaweiExposureClient"

        private const val FULL_DAY_ROLLING_PERIOD = 144L
    }

}