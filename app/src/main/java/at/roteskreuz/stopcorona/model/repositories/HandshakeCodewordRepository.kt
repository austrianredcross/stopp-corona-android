package at.roteskreuz.stopcorona.model.repositories

import androidx.annotation.StringRes
import at.roteskreuz.stopcorona.constants.Constants.Nearby.RANDOM_IDENTIFICATION_MAX
import at.roteskreuz.stopcorona.constants.Constants.Nearby.RANDOM_IDENTIFICATION_MIN
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor
import timber.log.Timber
import kotlin.random.Random

/**
 * Repository to create and convert the random identifier.
 */
interface HandshakeCodewordRepository {

    /**
     * Random identification value which is sent to all nearby contacts.
     * Has a value from 1000 to 9999
     */
    val identification: String

    /**
     * Returns the related codeword for a random number.
     *
     * [identification] the received random identification number with a value from 1000 to 9999.
     */
    fun getCodeword(identification: String): String
}

class HandshakeCodewordRepositoryImpl(
    private val contextInteractor: ContextInteractor
) : HandshakeCodewordRepository {

    override val identification: String = Random.nextInt(from = RANDOM_IDENTIFICATION_MIN, until = RANDOM_IDENTIFICATION_MAX).toString()

    override fun getCodeword(identification: String): String {
        try {
            Integer.parseInt(identification).apply {
                if (this !in RANDOM_IDENTIFICATION_MIN..RANDOM_IDENTIFICATION_MAX) {
                    Timber.e(SilentError("Identification number is invalid: $identification!"))
                    return identification
                }
            }
        } catch (exc: NumberFormatException) {
            Timber.e(SilentError(exc))
            return identification
        }

        val parts = listOf(identification.substring(0, 2), identification.substring(2))

        return with(getResourceId(parts[0])) {
            "${contextInteractor.getString(this)}${parts[1]}"
        }
    }

    @StringRes
    private fun getResourceId(identifier: String): Int {
        return contextInteractor.resources.getIdentifier("handshake_code_$identifier", "string", contextInteractor.packageName)
    }
}
