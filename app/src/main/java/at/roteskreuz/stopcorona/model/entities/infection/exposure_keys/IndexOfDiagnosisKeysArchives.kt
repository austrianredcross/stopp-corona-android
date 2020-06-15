package at.roteskreuz.stopcorona.model.entities.infection.exposure_keys

import at.roteskreuz.stopcorona.utils.asExposureInterval
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.threeten.bp.ZonedDateTime

/**
 * Describes infection info about user with data gathered from the Exposure SDK.
 */
@JsonClass(generateAdapter = true)
data class IndexOfDiagnosisKeysArchives(
    @field:Json(name = "full_14_batch")
    val full14DaysBatch: DiagnosisKeysBatch,

    @field:Json(name = "full_7_batch")
    val full07DaysBatch: DiagnosisKeysBatch,

    @field:Json(name = "daily_batches")
    val dailyBatches: List<DiagnosisKeysBatch>
) {

    fun batchesForLastHours(hour: Long): List<DiagnosisKeysBatch> {
        return dailyBatches.filter {
            (it.interval.asExposureInterval().isAfter(ZonedDateTime.now().minusHours(hour)))
        }
    }
}

@JsonClass(generateAdapter = true)
data class DiagnosisKeysBatch(
    val interval: Long, // Interval number of the keys in that batch

    @field:Json(name = "batch_file_paths")
    val batchFilePaths: List<String>
)
