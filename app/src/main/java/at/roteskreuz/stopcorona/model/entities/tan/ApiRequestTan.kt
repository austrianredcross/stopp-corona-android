package at.roteskreuz.stopcorona.model.entities.tan

import com.squareup.moshi.JsonClass

/**
 * Describes tan request response.
 */
@JsonClass(generateAdapter = true)
data class ApiRequestTan(
    val uuid: String,
    val status: String
)

/**
 * Describes tan request body.
 */
data class ApiRequestTanBody(
    val phoneNumber: String
)
