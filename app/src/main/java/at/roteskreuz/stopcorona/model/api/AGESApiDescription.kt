package at.roteskreuz.stopcorona.model.api

import at.roteskreuz.stopcorona.model.entities.statistics.CovidStatistics
import retrofit2.http.GET

/**
 * Description to get AGES data from https://covid19-dashboard.ages.at/data/JsonData.json.
 */
interface AGESApiDescription {

    @GET("data/JsonData.json")
    suspend fun requestAGESData(): CovidStatistics
}
