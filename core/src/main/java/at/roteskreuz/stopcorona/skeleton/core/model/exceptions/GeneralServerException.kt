package at.roteskreuz.stopcorona.skeleton.core.model.exceptions

/**
 * Exception on server that application does not know how to handle.
 */
data class GeneralServerException(val code: Int) : Exception()