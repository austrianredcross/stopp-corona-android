package at.roteskreuz.stopcorona.model.entities.statistics

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// https://covid19-dashboard.ages.at/data/JsonData.json
// Auto generated Code from: https://app.quicktype.io/

@JsonClass(generateAdapter = true)
data class CovidStatistics (
    @field:Json(name = "VersionsNr")
    val versionsNr: String,

    @field:Json(name = "VersionsDate")
    val versionsDate: String,

    @field:Json(name = "CreationDate")
    val creationDate: String,

    @field:Json(name = "CovidFaelle_Altersgruppe")
    val covidFaelleAltersgruppe: List<CovidFaelleAltersgruppe>,

    @field:Json(name = "CovidFaelle_Timeline")
    val covidFaelleTimeline: List<CovidFaelleTimeline>,

    @field:Json(name = "CovidFaelle_GKZ")
    val covidFaelleGKZ: List<CovidFaelleGKZ>,

    @field:Json(name = "CovidFallzahlen")
    val covidFallzahlen: List<CovidFallzahlen>
)
enum class Altersgruppe(val value: String) {
    @field:Json(name = "15-24")
    The1524("15-24"),
    @field:Json(name = "25-34")
    The2534("25-34"),
    @field:Json(name = "35-44")
    The3544("35-44"),
    @field:Json(name = "45-54")
    The4554("45-54"),
    @field:Json(name = "<5")
    The5("<5"),
    @field:Json(name = "5-14")
    The514("5-14"),
    @field:Json(name = "55-64")
    The5564("55-64"),
    @field:Json(name = "65-74")
    The6574("65-74"),
    @field:Json(name = "75-84")
    The7584("75-84"),
    @field:Json(name = ">84")
    The84(">84");

    companion object {
        fun fromValue(value: String): Altersgruppe = when (value) {
            "15-24" -> The1524
            "25-34" -> The2534
            "35-44" -> The3544
            "45-54" -> The4554
            "<5"    -> The5
            "5-14"  -> The514
            "55-64" -> The5564
            "65-74" -> The6574
            "75-84" -> The7584
            ">84"   -> The84
            else    -> throw IllegalArgumentException()
        }
    }
}

enum class Bundesland(val value: String) {
    @field:Json(name = "Alle")
    Alle("Alle"),
    @field:Json(name = "Burgenland")
    Burgenland("Burgenland"),
    @field:Json(name = "Kärnten")
    Kaernten("Kärnten"),
    @field:Json(name = "Niederösterreich")
    Niederoesterreich("Niederösterreich"),
    @field:Json(name = "Oberösterreich")
    Oberoesterreich("Oberösterreich"),
    @field:Json(name = "Salzburg")
    Salzburg("Salzburg"),
    @field:Json(name = "Steiermark")
    Steiermark("Steiermark"),
    @field:Json(name = "Tirol")
    Tirol("Tirol"),
    @field:Json(name = "Vorarlberg")
    Vorarlberg("Vorarlberg"),
    @field:Json(name = "Wien")
    Wien("Wien"),
    @field:Json(name = "Österreich")
    Oesterreich("Österreich");

    companion object {
        fun fromValue(value: String): Bundesland = when (value) {
            "Alle"             -> Alle
            "Burgenland"       -> Burgenland
            "Kärnten"          -> Kaernten
            "Niederösterreich" -> Niederoesterreich
            "Oberösterreich"   -> Oberoesterreich
            "Salzburg"         -> Salzburg
            "Steiermark"       -> Steiermark
            "Tirol"            -> Tirol
            "Vorarlberg"       -> Vorarlberg
            "Wien"             -> Wien
            "Österreich"       -> Oesterreich
            else               -> throw IllegalArgumentException()
        }
    }
}

enum class Geschlecht(val value: String) {
    @field:Json(name = "M")
    M("M"),
    @field:Json(name = "W")
    W("W");

    companion object {
        fun fromValue(value: String): Geschlecht = when (value) {
            "M"  -> M
            "W"  -> W
            else -> throw IllegalArgumentException()
        }
    }
}