package at.roteskreuz.stopcorona.model.entities.statistics

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// https://covid19-dashboard.ages.at/data/JsonData.json
// Auto generated Code from: https://app.quicktype.io/

@JsonClass(generateAdapter = true)
data class CovidFaelleAltersgruppe (
    @field:Json(name = "AltersgruppeID")
    val altersgruppeID: Long,

    @field:Json(name = "Altersgruppe")
    val altersgruppe: Altersgruppe,

    @field:Json(name = "Bundesland")
    val bundesland: Bundesland,

    @field:Json(name = "BundeslandID")
    val bundeslandID: Long,

    @field:Json(name = "AnzEinwohner")
    val anzEinwohner: Long,

    @field:Json(name = "Geschlecht")
    val geschlecht: Geschlecht,

    @field:Json(name = "Anzahl")
    val anzahl: Long,

    @field:Json(name = "AnzahlGeheilt")
    val anzahlGeheilt: Long,

    @field:Json(name = "AnzahlTot")
    val anzahlTot: Long
)
