package at.roteskreuz.stopcorona.skeleton.core.model.exceptions

/**
 * Object for unexpected error in API.
 */
data class UnexpectedError(override val cause: Throwable) :
    RuntimeException("Unexpected error is thrown, crash the system", cause)