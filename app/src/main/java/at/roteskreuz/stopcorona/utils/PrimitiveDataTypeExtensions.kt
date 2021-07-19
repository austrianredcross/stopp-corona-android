package at.roteskreuz.stopcorona.utils

import java.text.NumberFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.round

var numberFormatter: NumberFormat = NumberFormat.getNumberInstance(Locale.GERMAN)

// https://discuss.kotlinlang.org/t/how-do-you-round-a-number-to-n-decimal-places/8843/2
fun Double.roundTo(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}

fun Double.formatDecimal(): String {
    return numberFormatter.format(this)
}

fun Long.formatDecimal(): String {
    return numberFormatter.format(this)
}