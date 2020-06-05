package at.roteskreuz.stopcorona.model.entities.infection.info

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.squareup.moshi.JsonClass
import java.util.Base64

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
    val intervalCount: Int,
    val transmissionRisk: Int
)

class WarningTypeToTransmissionRiscConverter{
    companion object {
        fun transmissionRisc(warningType: WarningType): Int {
            return when(warningType){
                WarningType.RED -> 4
                WarningType.YELLOW -> 5
                WarningType.REVOKE -> 6
            }
        }
    }
}

class ApiTemporaryTracingKeyConverter{
    companion object TEK{
        @RequiresApi(Build.VERSION_CODES.O)
        fun convert(tek: TemporaryExposureKey, warningType: WarningType):ApiTemporaryTracingKey{
            val base64Key = Base64.getEncoder().encodeToString(tek.keyData)
            return ApiTemporaryTracingKey(
                key = base64Key,
                password = base64Key,
                intervalCount = tek.rollingPeriod,
                intervalNumber = tek.rollingStartIntervalNumber,
                transmissionRisk = WarningTypeToTransmissionRiscConverter.transmissionRisc(warningType)
            )
        }
    }
}


@JsonClass(generateAdapter = true)
data class ApiVerificationPayload(
    val uuid: String,
    val authorization: String
)