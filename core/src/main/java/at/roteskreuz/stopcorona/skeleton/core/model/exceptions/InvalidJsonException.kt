package at.roteskreuz.stopcorona.skeleton.core.model.exceptions

/**
 * Exception indicating some required values are not present in a backend response
 */
data class InvalidJsonException(override val cause: Throwable) :
    RuntimeException("JSON is not as expected", cause)