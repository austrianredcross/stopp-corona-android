package at.roteskreuz.stopcorona.model.api

import at.roteskreuz.stopcorona.model.entities.tan.ApiRequestTan
import at.roteskreuz.stopcorona.model.entities.tan.ApiRequestTanBody
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Description of REST Api for TAN-endpoint
 */
interface TanApiDescription {

    @POST("request-tan")
    suspend fun requestTan(@Body body: ApiRequestTanBody): ApiRequestTan
}
