package at.roteskreuz.stopcorona.model.api

import at.roteskreuz.stopcorona.model.entities.configuration.ApiConfigurationHolder
import at.roteskreuz.stopcorona.model.entities.infection.info.ApiInfectionDataRequest
import at.roteskreuz.stopcorona.model.entities.infection.message.ApiInfectionMessages
import retrofit2.http.*

/**
 * Description of REST Api for Retrofit.
 */
interface ApiDescription {

    @GET("configuration")
    suspend fun configuration(): ApiConfigurationHolder

    @Deprecated(message = "This is API is obsolete, we will use the Exposure Notificationframework")
    @GET("infection-messages")
    suspend fun infectionMessages(@Query("addressPrefix") addressPrefix: String, @Query("fromId") fromId: Long? = null): ApiInfectionMessages

    @POST("publish")
    suspend fun publish(@Body infectionDataRequest: ApiInfectionDataRequest)
}
