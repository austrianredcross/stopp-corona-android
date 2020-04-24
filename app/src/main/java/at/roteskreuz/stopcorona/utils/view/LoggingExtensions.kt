package at.roteskreuz.stopcorona.utils.view

import at.roteskreuz.stopcorona.model.exceptions.SilentError
import timber.log.Timber

/**
 *  Extension methods that convert nullable to non-nullable
 */

/**
 * Convert nullable to [R] and in case of [T] is null log an error.
 */
inline fun <reified T, reified R> T?.safeMap(
    exception: Throwable,
    defaultValue: R,
    block: (T) -> R = { it as R }
): R = this?.let(block) ?: defaultValue.also { Timber.e(SilentError(exception)) }

/**
 * Convert nullable to [R] and in case of [T] is null log an error.
 */
inline fun <reified T, reified R> T?.safeMap(
    message: String = "Class: ${T::class.java.name} is null",
    defaultValue: R,
    block: (T) -> R = { it as R }
): R {
    return this?.let(block) ?: defaultValue.also { Timber.e(SilentError(message)) }
}

/**
 *  Extension methods that run provided lambda if the receiver is not null.
 */

/**
 * Similar to [let] but in case of null it will log an error.
 */
inline fun <reified T> T?.safeRun(
    exception: Throwable,
    block: (T) -> Unit
) = this?.let(block) ?: Timber.e(SilentError(exception))

/**
 * Similar to [let] but in case of null it will log an error.
 */
inline fun <reified T> T?.safeRun(
    message: String = "Class: ${T::class.java.name} is null",
    block: (T) -> Unit
) = this?.let(block) ?: Timber.e(SilentError(message))