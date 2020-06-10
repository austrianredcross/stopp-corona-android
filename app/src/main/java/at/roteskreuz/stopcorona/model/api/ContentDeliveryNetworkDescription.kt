package at.roteskreuz.stopcorona.model.api

import at.roteskreuz.stopcorona.model.entities.infection.exposure_keys.IndexOfDiagnosisKeysArchives
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

/**
 * Description of API for downloading the exposure key archives
 */
interface ContentDeliveryNetworkDescription {

    @GET("exposures/at/index.json")
    suspend fun indexOfDiagnosisKeysArchives(): IndexOfDiagnosisKeysArchives

    @Streaming
    @GET("{fullPathProvidedAsParameter}")
    fun downloadExposureKeyArchive(
        @Path("fullPathProvidedAsParameter", encoded = true) path: String
    ) : Call<ResponseBody>
}