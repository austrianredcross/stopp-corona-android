package at.roteskreuz.stopcorona.model.api

import at.roteskreuz.stopcorona.model.entities.tan.ApiRequestTan
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Description of REST Api for TAN-endpoint
 */
interface TanApiDescription {

    @GET("request-tan")
    suspend fun requestTan(@Query("phone") mobileNumber: String): ApiRequestTan
}
