package at.roteskreuz.stopcorona.model.api

import at.roteskreuz.stopcorona.model.entities.configuration.ApiConfigurationHolder
import at.roteskreuz.stopcorona.model.entities.infection.info.ApiInfectionDataRequest
import retrofit2.http.*

/**
 * Description of REST Api for Retrofit.
 */
interface ApiDescription {

    @GET("configuration")
    suspend fun configuration(): ApiConfigurationHolder

    @POST("publish")
    suspend fun publish(@Body infectionDataRequest: ApiInfectionDataRequest)
}
