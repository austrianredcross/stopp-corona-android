package at.roteskreuz.stopcorona.model.exceptions

/**
 * This class describes error which is logged to the crash collection system but not visible to user.
 */
class SilentError(message: String?, cause: Throwable?) : Throwable(message, cause) {

    constructor(message: String) : this(message, null)
    constructor(cause: Throwable) : this(null, cause)

    override fun toString(): String {
        val printedMessage = message ?: cause?.localizedMessage
        return if (printedMessage != null) {
            this::class.java.name + " - " + printedMessage
        } else super.toString()
    }
}