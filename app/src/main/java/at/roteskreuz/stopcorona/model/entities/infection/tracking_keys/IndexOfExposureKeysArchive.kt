package at.roteskreuz.stopcorona.model.entities.infection.tracking_keys

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Describes infection info about user with data gathered from the Exposure SDK.
 */
@JsonClass(generateAdapter = true)
data class IndexOfExposureKeysArchive(
    @field:Json(name = "full_batch")
    val full_batch: Batch,

    @field:Json(name = "daily_batches")
    val dailyBatches: List<Batch>
)

@JsonClass(generateAdapter = true)
data class Batch(
    val interval: Int, // Interval number of the keys in that batch

    @field:Json(name = "batch_file_paths")
    val batchFilePaths: List<String>
)

