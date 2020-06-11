package at.roteskreuz.stopcorona.model.entities.infection.exposure_keys

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Describes infection info about user with data gathered from the Exposure SDK.
 */
@JsonClass(generateAdapter = true)
data class IndexOfDiagnosisKeysArchives(
    @field:Json(name = "full_batch")
    val fullBatch: DiagnosisKeysBatch,

    @field:Json(name = "daily_batches")
    val dailyBatches: List<DiagnosisKeysBatch>
)

@JsonClass(generateAdapter = true)
data class DiagnosisKeysBatch(
    val interval: Int, // Interval number of the keys in that batch

    @field:Json(name = "batch_file_paths")
    val batchFilePaths: List<String>
)

