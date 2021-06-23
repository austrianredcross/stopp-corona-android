package at.roteskreuz.stopcorona

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import at.roteskreuz.stopcorona.commonexposure.CommonExposureClient
import at.roteskreuz.stopcorona.commonexposure.ExposureServiceStatus
import at.roteskreuz.stopcorona.constants.Constants.ExposureNotification.MIN_SUPPORTED_GOOGLE_PLAY_APK_VERSION
import at.roteskreuz.stopcorona.model.GoogleServiceStatus
import com.google.android.gms.common.ConnectionResult

import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.nearby.exposurenotification.*
import kotlinx.coroutines.tasks.await
import timber.log.Timber
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
        exposureNotificationClient.provideDiagnosisKeys(archives, exposureConfiguration, token).await()
    }

    override suspend fun getExposureSummary(token: String): ExposureSummary {
        return exposureNotificationClient.getExposureSummary(token).await()
    }

    override suspend fun getExposureInformation(token: String): List<ExposureInformation> {
        return exposureNotificationClient.getExposureInformation(token).await()
    }

    override fun getServiceStatus(): ExposureServiceStatus {

        val statusCode = googleApiAvailability.isGooglePlayServicesAvailable(application)
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

    override fun getServiceVersion(context : Context): String {
        return "Google Mobile Services: ${googleMobileServicesVersion(context)}";
    }

    override fun getFrameworkExposureNotificationSettingIntent(context: Context): Intent {
        return Intent(ExposureNotificationClient.ACTION_EXPOSURE_NOTIFICATION_SETTINGS)
    }

    private fun googleMobileServicesVersion(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e, "Couldn't get the app version")
            "Not Available"
        }
    }
}