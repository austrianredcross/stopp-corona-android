package at.roteskreuz.stopcorona.model.entities.statistics

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// https://covid19-dashboard.ages.at/data/JsonData.json
// Auto generated Code from: https://app.quicktype.io/

@JsonClass(generateAdapter = true)
data class CovidFallzahlen (
    @field:Json(name = "Meldedat")
    val meldedat: String,

    @field:Json(name = "TestGesamt")
    val testGesamt: Long,

    @field:Json(name = "MeldeDatum")
    val meldeDatum: String,

    @field:Json(name = "FZHosp")
    val fzHosp: Long,

    @field:Json(name = "FZICU")
    val fzicu: Long,

    @field:Json(name = "FZHospFree")
    val fzHospFree: Long,

    @field:Json(name = "FZICUFree")
    val fzicuFree: Long,

    @field:Json(name = "BundeslandID")
    val bundeslandID: Long,

    @field:Json(name = "Bundesland")
    val bundesland: Bundesland
)
