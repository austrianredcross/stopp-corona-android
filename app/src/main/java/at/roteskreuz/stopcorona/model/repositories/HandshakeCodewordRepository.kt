package at.roteskreuz.stopcorona.model.repositories

import androidx.annotation.IntRange
import androidx.annotation.StringRes
import at.roteskreuz.stopcorona.constants.Constants.Nearby.RANDOM_IDENTIFICATION_MAX
import at.roteskreuz.stopcorona.constants.Constants.Nearby.RANDOM_IDENTIFICATION_MIN
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor
import timber.log.Timber
import kotlin.math.abs
import kotlin.random.Random

/**
 * Repository to create and convert the random identifier.
 */
interface HandshakeCodewordRepository {

    /**
     * Random identification value which is sent to all nearby contacts.
     * Has a value from 0 to 9999.
     */
    @get:IntRange(from = RANDOM_IDENTIFICATION_MIN.toLong(), to = RANDOM_IDENTIFICATION_MAX.toLong())
    val identificationNumber: Int

    /**
     * Returns the related codeword for a random number.
     *
     * [identification] the received random identification number with a value from 0 to 9999.
     */
    fun getCodeword(
        @IntRange(from = RANDOM_IDENTIFICATION_MIN.toLong(), to = RANDOM_IDENTIFICATION_MAX.toLong())
        identification: Int
    ): String

    /**
     * Convert number lower than 1000 to string with prefixed zeros to have 4 characters.
     * Negative numbers are negated.
     * If there is number greater than 9999, the result is last 4 characters of the string value.
     */
    fun zeroPrefixed(number: Int): String
}

class HandshakeCodewordRepositoryImpl(
    private val contextInteractor: ContextInteractor
) : HandshakeCodewordRepository {

    override val identificationNumber: Int = Random.nextInt(from = RANDOM_IDENTIFICATION_MIN, until = RANDOM_IDENTIFICATION_MAX)

    override fun getCodeword(identification: Int): String {
        val value = zeroPrefixed(identification)
        val parts = arrayOf(value.substring(0, 2), value.substring(2))
        return with(getResourceId(parts[0])) {
            "${contextInteractor.getString(this)}${parts[1]}"
        }
    }

    @StringRes
    private fun getResourceId(identifier: String): Int {
        return contextInteractor.resources.getIdentifier("handshake_code_$identifier", "string", contextInteractor.packageName)
    }

    override fun zeroPrefixed(number: Int): String {
        val value = abs(number)
        if (value < RANDOM_IDENTIFICATION_MIN ||
            value > RANDOM_IDENTIFICATION_MAX ||
            number < 0) {
            Timber.e(SilentError("Invalid input number $number"))
        }
        return String.format("%04d", value).let {
            if (it.length > 4) {
                it.substring(it.length - 4)
            } else it
        }
    }
}
