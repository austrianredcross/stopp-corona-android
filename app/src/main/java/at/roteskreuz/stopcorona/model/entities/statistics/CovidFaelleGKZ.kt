package at.roteskreuz.stopcorona.model.entities.statistics

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// https://covid19-dashboard.ages.at/data/JsonData.json
// Auto generated Code from: https://app.quicktype.io/

@JsonClass(generateAdapter = true)
data class CovidFaelleGKZ (
    @field:Json(name = "Bezirk")
    val bezirk: String,

    @field:Json(name = "GKZ")
    val gkz: Long,

    @field:Json(name = "AnzEinwohner")
    val anzEinwohner: Long,

    @field:Json(name = "Anzahl")
    val anzahl: Long,

    @field:Json(name = "AnzahlTot")
    val anzahlTot: Long,

    @field:Json(name = "AnzahlFaelle7Tage")
    val anzahlFaelle7Tage: Long
)