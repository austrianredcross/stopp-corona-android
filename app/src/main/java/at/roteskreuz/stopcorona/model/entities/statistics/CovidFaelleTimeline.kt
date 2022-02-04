package at.roteskreuz.stopcorona.model.entities.statistics

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// https://covid19-dashboard.ages.at/data/JsonData.json
// Auto generated Code from: https://app.quicktype.io/

@JsonClass(generateAdapter = true)
data class CovidFaelleTimeline (
    @field:Json(name = "Time")
    val time: String,

    @field:Json(name = "Bundesland")
    val bundesland: Bundesland,

    @field:Json(name = "BundeslandID")
    val bundeslandID: Long,

    @field:Json(name = "AnzEinwohner")
    val anzEinwohner: Long,

    @field:Json(name = "AnzahlFaelle")
    val anzahlFaelle: Long,

    @field:Json(name = "AnzahlFaelleSum")
    val anzahlFaelleSum: Long,

    @field:Json(name = "AnzahlFaelle7Tage")
    val anzahlFaelle7Tage: Long,

    @field:Json(name = "SiebenTageInzidenzFaelle")
    val siebenTageInzidenzFaelle: Double,

    @field:Json(name = "AnzahlTotTaeglich")
    val anzahlTotTaeglich: Long,

    @field:Json(name = "AnzahlTotSum")
    val anzahlTotSum: Long,

    @field:Json(name = "AnzahlGeheiltTaeglich")
    val anzahlGeheiltTaeglich: Long,

    @field:Json(name = "AnzahlGeheiltSum")
    val anzahlGeheiltSum: Long
)