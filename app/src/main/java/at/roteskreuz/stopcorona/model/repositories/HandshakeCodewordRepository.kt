package at.roteskreuz.stopcorona.model.repositories

import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor

/**
 * Repository to convert a random number into a readable identification word.
 */
interface HandshakeCodewordRepository {

    /**
     * Returns the related codeword for a random number.
     */
    fun getCodeword(index: Int): String
}

class HandshakeCodewordRepositoryImpl(
    private val contextInteractor: ContextInteractor
) : HandshakeCodewordRepository {

    override fun getCodeword(index: Int): String {
        TODO("Not yet implemented")
    }
}
