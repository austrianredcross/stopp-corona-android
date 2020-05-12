package at.roteskreuz.stopcorona.model.repositories

import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor

/**
 * Repository to convert a random number into a readable identification word.
 */
interface HandshakeCodewordRepository {

    /**
     * Returns the related codeword for a random number.
     */
    fun getCodeword(identification: String): String?
}

class HandshakeCodewordRepositoryImpl(
    private val contextInteractor: ContextInteractor
) : HandshakeCodewordRepository {

    override fun getCodeword(identification: String): String? {
        val parts = listOf(identification.substring(0, 2), identification.substring(2))

        return when (val resId = contextInteractor.resources.getIdentifier("handshake_code_${parts[0]}", "string", contextInteractor.packageName)) {
            0 -> null
            else -> "${contextInteractor.getString(resId)}${parts[1]}"
        }
    }
}
