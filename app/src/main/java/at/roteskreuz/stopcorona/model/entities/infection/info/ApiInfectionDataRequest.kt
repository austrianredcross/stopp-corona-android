package at.roteskreuz.stopcorona.model.entities.infection.info

import android.util.Base64
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson
import java.util.UUID

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
    /**
     * rollingStartIntervalNumber = A number describing when a key starts.
     * It is equal to startTimeOfKeySinceEpochInSecs / (60 * 10). */
    val intervalNumber: Int,
    /**
     * rollingPeriod = A number describing how long a key is valid.
     * It is expressed in increments of 10 minutes (e.g. 144 for 24 hours). */
    val intervalCount: Int
)

@JsonClass(generateAdapter = true)
data class ApiVerificationPayload(
    val uuid: String,
    val authorization: String
)

fun List<Pair<List<TemporaryExposureKey>, UUID>>.asApiEntity(): List<ApiTemporaryTracingKey> {
    return this.map { (temporaryExposureKeys, password) ->
        temporaryExposureKeys.map { temporaryExposureKey ->
            val base64Key = Base64.encodeToString(temporaryExposureKey.keyData, Base64.NO_WRAP)

            ApiTemporaryTracingKey(
                key = base64Key,
                password = password.toString(),
                intervalNumber = temporaryExposureKey.rollingStartIntervalNumber,
                intervalCount = temporaryExposureKey.rollingPeriod
            )
        }
    }.flatten()
}

/**
 * Adapter for UUID to String.
 */
object UUIDAdapter {

    @FromJson
    fun fromJson(value: String?): UUID? {
        return value?.let {
            UUID.fromString(it)
        }
    }

    @ToJson
    fun toJson(value: UUID?): String? {
        return value?.toString()
    }
}