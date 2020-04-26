package at.roteskreuz.stopcorona.utils

import at.roteskreuz.stopcorona.model.exceptions.SilentError
import timber.log.Timber

/**
 * Extensions for enums.
 */

/**
 * Returns an enum entry with the specified name or null if no such entry was found.
 * In case of not found enum value the error [EnumValueNotFoundException] is logged.
 */
inline fun <reified T : Enum<T>> String.asEnum(): T? {
    return enumValues<T>().firstOrNull { it.name == this }.also {
        it ?: Timber.e(SilentError(EnumValueNotFoundException(T::class.java.name, this)))
    }
}

class EnumValueNotFoundException(enumName: String, enumValue: String?) : Throwable("Enum value $enumValue was not found in $enumName")