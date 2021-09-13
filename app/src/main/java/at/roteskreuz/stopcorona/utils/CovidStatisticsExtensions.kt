package at.roteskreuz.stopcorona.utils

import androidx.fragment.app.FragmentManager
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.entities.statistics.Bundesland
import at.roteskreuz.stopcorona.model.entities.statistics.CovidFaelleGKZ
import at.roteskreuz.stopcorona.model.entities.statistics.CovidFaelleTimeline
import at.roteskreuz.stopcorona.model.entities.statistics.CovidFallzahlen
import at.roteskreuz.stopcorona.screens.base.dialog.GeneralErrorDialog

fun List<CovidFaelleTimeline>.lastTwoTimeLines(state: Bundesland): List<CovidFaelleTimeline> {
    return this.filter { it.bundesland == state }
        .sortedBy { it.time }.takeLast(2)
}

fun List<CovidFallzahlen>.lastTwoFallZahlen(state: Bundesland): List<CovidFallzahlen> {
    // This check is necessary because there is no covidFallzahlen for the state Bundesland.Oesterreich
    val covidFallzahlenState = if (state == Bundesland.Oesterreich) {
        Bundesland.Alle
    } else {
        state
    }
    return this.filter { it.bundesland == covidFallzahlenState }
        .sortedBy { it.meldedat }.takeLast(2)
}

fun Double.formatIncidenceValue(): String {
    var formatted = numberFormatter.format(this)
    formatted = when {
        this > 0 -> {
            "+$formatted"
        }
        this == 0.0 -> {
            "+/-$formatted"
        }
        else -> {
            formatted
        }
    }
    return formatted
}

fun Long.formatIncidenceValue(): String {
    var formatted = numberFormatter.format(this)
    formatted = when {
        this > 0 -> {
            "+$formatted"
        }
        this == 0L -> {
            "+/-$formatted"
        }
        else -> {
            formatted
        }
    }
    return formatted
}

fun CovidFaelleGKZ.state(childFragmentManager: FragmentManager): Bundesland? {
    when (this.gkz) {
        in 900..1000 -> return Bundesland.Wien
        in 800..900 -> return Bundesland.Vorarlberg
        in 700..800 -> return Bundesland.Tirol
        in 600..700 -> return Bundesland.Steiermark
        in 500..600 -> return Bundesland.Salzburg
        in 400..500 -> return Bundesland.Oberoesterreich
        in 300..400 -> return Bundesland.Niederoesterreich
        in 200..300 -> return Bundesland.Kaernten
        in 100..200 -> return Bundesland.Burgenland
    }
    GeneralErrorDialog(R.string.ages_api_error, R.string.ages_api_validation_error).show(childFragmentManager, GeneralErrorDialog::class.java.name)
    return null
}

fun CovidFaelleGKZ.incidenceValue(): Double {
    // SOURCE: // https://covid19-dashboard.ages.at/data/JsonData.json
    // Caluclation is from the script2.js from https://covid19-dashboard.ages.at/
    // line 194 -> rel7d: e.AnzahlFaelle7Tage/e.AnzEinwohner*100000
    return this.anzahlFaelle7Tage.toDouble() / this.anzEinwohner.toDouble() * 100000.toDouble()
}

fun Double.incidenceIcon(): Int {
    if (this > 0) {
        return R.drawable.ic_statistic_up
    } else if (this < 0) {
        return R.drawable.ic_statistic_down
    } else {
        return R.drawable.ic_statistic_unchanged
    }
}

fun Double.incidenceColorMark(): Int {
    when {
        this <= 0.0 -> return R.drawable.statistics_legend_0
        this in 0.1..99.9 -> return R.drawable.statistics_legend_less_100
        this in 100.0..199.9 -> return R.drawable.statistics_legend_more_100
        this in 200.0..399.9 -> return R.drawable.statistics_legend_200
        else -> {
            return R.drawable.statistics_legend_400
        }
    }
}

fun Double.incidenceColor(): Int {
    when {
        this <= 0.0 -> return R.color.white
        this in 0.1..99.9 -> return R.color.yellow
        this in 100.0..199.9 -> return R.color.gold
        this in 200.0..399.9 -> return R.color.raspberry
        else -> {
            return R.color.brown
        }
    }
}

fun Bundesland.localize(): Int {
    when (this) {
        Bundesland.Oesterreich -> {
            return R.string.covid_statistics_state_id_austria
        }
        Bundesland.Wien -> {
            return R.string.covid_statistics_state_id_9
        }
        Bundesland.Vorarlberg -> {
            return R.string.covid_statistics_state_id_8
        }
        Bundesland.Tirol -> {
            return R.string.covid_statistics_state_id_7
        }
        Bundesland.Steiermark -> {
            return R.string.covid_statistics_state_id_6
        }
        Bundesland.Salzburg -> {
            return R.string.covid_statistics_state_id_5
        }
        Bundesland.Oberoesterreich -> {
            return R.string.covid_statistics_state_id_4
        }
        Bundesland.Niederoesterreich -> {
            return R.string.covid_statistics_state_id_3
        }
        Bundesland.Kaernten -> {
            return R.string.covid_statistics_state_id_2
        }
        Bundesland.Burgenland -> {
            return R.string.covid_statistics_state_id_1
        }
        else -> {
            return R.string.covid_statistics_state_id_all
        }
    }
}