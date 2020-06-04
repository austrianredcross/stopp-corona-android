package at.roteskreuz.stopcorona.model.entities.infection.info

import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.squareup.moshi.JsonClass

/**
 * Describes infection info about user with data gathered from the Exposure SDK.
 */
@JsonClass(generateAdapter = true)
data class ApiInfectionDataRequest(
    val temporaryTracingKeys: List<ApiTemporaryTracingKey>,
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
    val intervalCount: Int,
    val transmissionRisk: Int
)
class ApiTemporaryTracingKeyConverter{
    companion object TEK{
        fun convert(tek: TemporaryExposureKey, risc: Int):ApiTemporaryTracingKey{
            return ApiTemporaryTracingKey(
                key = tek.keyData.toString(),
                password = tek.keyData.toString(),
                intervalCount = tek.rollingPeriod,
                intervalNumber = tek.rollingStartIntervalNumber,
                transmissionRisk = risc
            )
        }
    }
}


@JsonClass(generateAdapter = true)
data class ApiVerificationPayload(
    val uuid: String,
    val authorization: String
)