package at.roteskreuz.stopcorona.model.api

import at.roteskreuz.stopcorona.model.entities.configuration.ApiConfigurationHolder
import at.roteskreuz.stopcorona.model.entities.infection.info.ApiInfectionDataRequest
import at.roteskreuz.stopcorona.model.entities.infection.info.ApiInfectionInfoRequest
import at.roteskreuz.stopcorona.model.entities.infection.message.ApiInfectionMessages
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

/**
 * Description of REST Api for Retrofit.
 */
interface ApiDescription {

    @GET("configuration")
    suspend fun configuration(): ApiConfigurationHolder

    @GET("infection-messages")
    suspend fun infectionMessages(@Query("addressPrefix") addressPrefix: String, @Query("fromId") fromId: Long? = null): ApiInfectionMessages

    @PUT("infection-info")
    suspend fun infectionInfo(@Body infectionInfoRequest: ApiInfectionInfoRequest)

    @PUT("publish")
    suspend fun publish(@Body infectionDataRequest: ApiInfectionDataRequest)
}
