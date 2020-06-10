package at.roteskreuz.stopcorona.model.api

import at.roteskreuz.stopcorona.model.entities.infection.tracking_keys.IndexOfTrackingKeysArchive
import retrofit2.http.GET

/**
 * Description of API for downloading the trackging key archives
 */
interface TrackingKeysDescription {

    @GET("exposures/at/index.json")
    suspend fun indexOfTrackingKeysArchives(): IndexOfTrackingKeysArchive
}