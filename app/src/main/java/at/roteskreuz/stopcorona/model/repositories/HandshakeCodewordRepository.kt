package at.roteskreuz.stopcorona.model.repositories

import androidx.annotation.StringRes
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor

/**
 * Repository to convert a random number into a readable identification word.
 */
interface HandshakeCodewordRepository {

    /**
     * Returns the related codeword resource id for a random number.
     */
    @StringRes
    fun getCodeword(identification: String): Int
}

class HandshakeCodewordRepositoryImpl(
    private val contextInteractor: ContextInteractor
) : HandshakeCodewordRepository {

    @StringRes
    override fun getCodeword(identification: String): Int {
        return contextInteractor.resources.getIdentifier("handshake_code_$identification", "string", contextInteractor.packageName)
    }
}
