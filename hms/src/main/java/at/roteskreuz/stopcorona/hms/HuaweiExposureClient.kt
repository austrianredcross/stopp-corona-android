package at.roteskreuz.stopcorona.hms

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import at.roteskreuz.stopcorona.commonexposure.CommonExposureClient
import at.roteskreuz.stopcorona.commonexposure.ExposureServiceStatus
import at.roteskreuz.stopcorona.hms.extensions.*
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.huawei.hms.api.HuaweiApiAvailability
import com.huawei.hms.contactshield.*
import timber.log.Timber
import java.io.File

class HuaweiExposureClient(
    private val application: Application,
    private val huaweiApiAvailability: HuaweiApiAvailability,
    private val contactShieldEngine: ContactShieldEngine
) : CommonExposureClient {

    override suspend fun start() {

        Timber.tag(LOG_TAG).d("Trying to start HMS ContactShield...")

        val pendingIntent: PendingIntent = PendingIntent.getService(
            application,
            REQUEST_CODE,
            Intent(application, ContactShieldIntentService::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        contactShieldEngine.startContactShield(pendingIntent, ContactShieldSetting.DEFAULT).await()

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
        val periodKeys: List<PeriodicKey> = contactShieldEngine.periodicKey.await()
        return periodKeys.map { it.toTemporaryExposureKey() }
    }

    override suspend fun provideDiagnosisKeys(
        archives: List<File>,
        exposureConfiguration: ExposureConfiguration,
        token: String
    ) {
        val huaweiConfiguration: DiagnosisConfiguration =
            exposureConfiguration.toDiagnosisConfiguration()
        contactShieldEngine.putSharedKeyFiles(archives, huaweiConfiguration, token)
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

    companion object {

        private const val LOG_TAG = "HuaweiExposureClient"

        private const val REQUEST_CODE = 1337
    }

}