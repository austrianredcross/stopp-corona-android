package at.roteskreuz.stopcorona.model.api

import at.roteskreuz.stopcorona.model.entities.infection.exposure_keys.IndexOfExposureKeysArchive
import retrofit2.http.GET

/**
 * Description of API for downloading the exposure key archives
 */
interface ContentDeliveryNetworkDescription {

    @GET("exposures/at/index.json")
    suspend fun indexOfTrackingKeysArchives(): IndexOfExposureKeysArchive
}