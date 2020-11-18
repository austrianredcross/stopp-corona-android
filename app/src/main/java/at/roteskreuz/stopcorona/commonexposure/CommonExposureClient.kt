package at.roteskreuz.stopcorona.commonexposure

import android.content.Context
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import java.io.File

interface CommonExposureClient {

    /**
     * Starts the exposure client.
     */
    suspend fun start()

    /**
     * Stops the exposure client.
     */
    suspend fun stop()

    /**
     * Checks if the exposure client is running.
     * @return True if the exposure client is running, false if not.
     */
    suspend fun isRunning(): Boolean

    suspend fun getTemporaryExposureKeys(): List<TemporaryExposureKey>

    suspend fun provideDiagnosisKeys(
        archives: List<File>,
        exposureConfiguration: ExposureConfiguration,
        token: String
    )

    suspend fun getExposureSummary(token: String): ExposureSummary
    suspend fun getExposureInformation(token: String): List<ExposureInformation>

    /**
     * Returns the service error status (if any).
     * Returns null if the service is running okay.
     */
    fun getServiceStatus(): ExposureServiceStatus

    fun getServiceVersion(context: Context) : String

    fun isGmsService() : Boolean
}