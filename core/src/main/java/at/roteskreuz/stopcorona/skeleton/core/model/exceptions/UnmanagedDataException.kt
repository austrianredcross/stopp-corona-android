package at.roteskreuz.stopcorona.skeleton.core.model.exceptions

/**
 * Exception when someone tries to read a data from a scoped repository whose scope is not prepared for the read yet.
 */
object UnmanagedDataException : Exception()