package at.roteskreuz.stopcorona.model.entities.infection.info

import com.squareup.moshi.JsonClass

/**
 * Describes infection info about user with data from the Exposure SDK.
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
    val intervalNumber: Long,
    val intervalCount: Int,
    val transmissionRisk: Int
)

@JsonClass(generateAdapter = true)
data class ApiVerificationPayload(
    val uuid: String,
    val authorization: String
)