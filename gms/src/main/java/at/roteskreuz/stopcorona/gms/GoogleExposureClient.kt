package at.roteskreuz.stopcorona.gms

import android.app.Application
import at.roteskreuz.stopcorona.commonexposure.CommonExposureClient
import at.roteskreuz.stopcorona.commonexposure.ExposureServiceStatus
import at.roteskreuz.stoppcorona.google.GoogleServiceStatus
import com.google.android.gms.common.ConnectionResult

import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.nearby.exposurenotification.*
import kotlinx.coroutines.tasks.await
import java.io.File

class GoogleExposureClient(
    private val application: Application,
    private val googleApiAvailability: GoogleApiAvailability,
    private val exposureNotificationClient: ExposureNotificationClient
) : CommonExposureClient {

    override suspend fun start() {
        exposureNotificationClient.start().await()
    }

    override suspend fun stop() {
        exposureNotificationClient.stop().await()
    }

    override suspend fun isRunning(): Boolean {
        return exposureNotificationClient.isEnabled.await()
    }

    override suspend fun getTemporaryExposureKeys(): List<TemporaryExposureKey> {
        return exposureNotificationClient.temporaryExposureKeyHistory.await()
    }

    override suspend fun provideDiagnosisKeys(archives: List<File>, exposureConfiguration: ExposureConfiguration, token: String) {
        exposureNotificationClient.provideDiagnosisKeys(archives, exposureConfiguration, token)
    }

    override suspend fun getExposureSummary(token: String): ExposureSummary {
        return exposureNotificationClient.getExposureSummary(token).await()
    }

    override suspend fun getExposureInformation(token: String): List<ExposureInformation> {
        return exposureNotificationClient.getExposureInformation(token).await()
    }

    override fun getServiceStatus(): ExposureServiceStatus {

        val statusCode: Int = googleApiAvailability.isGooglePlayServicesAvailable(application)
        val version = googleApiAvailability.getApkVersion(application)

        if (version < MIN_SUPPORTED_GOOGLE_PLAY_APK_VERSION) {
            return GoogleServiceStatus.InvalidVersionOfGooglePlayServices
        }

        return when (statusCode) {
            ConnectionResult.SUCCESS -> GoogleServiceStatus.Success(statusCode)
            ConnectionResult.SERVICE_MISSING -> GoogleServiceStatus.ServiceMissing(statusCode)
            ConnectionResult.SERVICE_UPDATING -> GoogleServiceStatus.ServiceUpdating(statusCode)
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> GoogleServiceStatus.ServiceVersionUpdateRequired(statusCode)
            ConnectionResult.SERVICE_DISABLED -> GoogleServiceStatus.ServiceDisabled(statusCode)
            ConnectionResult.SERVICE_INVALID -> GoogleServiceStatus.ServiceInvalid(statusCode)
            else -> GoogleServiceStatus.UnknownStatus(statusCode)
        }

    }

    private companion object {
        const val MIN_SUPPORTED_GOOGLE_PLAY_APK_VERSION = 201813000
    }
}