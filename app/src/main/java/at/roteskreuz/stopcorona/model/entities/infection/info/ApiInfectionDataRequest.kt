package at.roteskreuz.stopcorona.model.entities.infection.info

import android.util.Base64
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.squareup.moshi.JsonClass

/**
 * Describes infection info about user with data gathered from the Exposure SDK.
 */
@JsonClass(generateAdapter = true)
data class ApiInfectionDataRequest(
    val temporaryExposureKeys: List<ApiTemporaryTracingKey>,
    val regions: List<String>,
    val appPackageName: String,
    val platform: String,
    val diagnosisType: WarningType,
    val verificationAuthorityName: String,
    val verificationPayload: ApiVerificationPayload
)

@JsonClass(generateAdapter = true)
data class ApiTemporaryTracingKey(
    val key: String,
    val password: String,
    val intervalNumber: Int,
    val intervalCount: Int
)

@JsonClass(generateAdapter = true)
data class ApiVerificationPayload(
    val uuid: String,
    val authorization: String
)

fun List<TemporaryExposureKey>.convertToApiTemporaryTracingKeys(): List<ApiTemporaryTracingKey>{
    return this.map { it.convertToApiTemporaryTracingKey() }
}

fun TemporaryExposureKey.convertToApiTemporaryTracingKey(): ApiTemporaryTracingKey{
    val base64Key = Base64.encodeToString(this.keyData, Base64.NO_WRAP)
    return ApiTemporaryTracingKey(
        key = base64Key,
        password = base64Key,
        /**
         * rollingStartIntervalNumber = A number describing when a key starts.
         * It is equal to startTimeOfKeySinceEpochInSecs / (60 * 10). */
        intervalNumber = this.rollingStartIntervalNumber,
        /**
         * rollingPeriod = A number describing how long a key is valid.
         * It is expressed in increments of 10 minutes (e.g. 144 for 24 hours). */
        intervalCount = this.rollingPeriod
    )
}