package at.roteskreuz.stopcorona.skeleton.core.utils

import android.content.SharedPreferences
import at.roteskreuz.stopcorona.skeleton.core.model.api.converters.Rfc3339InstantAdapter
import at.roteskreuz.stopcorona.skeleton.core.model.api.converters.Rfc3339ZonedDateTimeAdapter
import com.github.dmstocking.optional.java.util.Optional
import io.reactivex.Observable
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime
import java.util.Collections
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Extensions to [SharedPreferences]
 */

/**
 * Keep a strong referenfce to shared preference change listeners. Otherwise they'l be garbage collected sooner or later.
 *
 * See [SharedPreferences.registerOnSharedPreferenceChangeListener]
 */
private val sharedPreferenceListeners = Collections.synchronizedList(mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>())

/**
 * Extension function to [SharedPreferences] that will put string and immediately apply changes
 */
fun SharedPreferences.putAndApply(key: String, value: String?) = edit().putString(key, value).apply()

/**
 * Extension function to [SharedPreferences] that will put int and immediately apply changes
 */
fun SharedPreferences.putAndApply(key: String, value: Int) = edit().putInt(key, value).apply()

/**
 * Extension function to [SharedPreferences] that will put boolean and immediately apply changes
 */
fun SharedPreferences.putAndApply(key: String, value: Boolean) = edit().putBoolean(key, value).apply()

/**
 * Extension function to [SharedPreferences] that will put float and immediately apply changes
 */
fun SharedPreferences.putAndApply(key: String, value: Float) = edit().putFloat(key, value).apply()

/**
 * Extension function to [SharedPreferences] that will put long and immediately apply changes
 */
fun SharedPreferences.putAndApply(key: String, value: Long) = edit().putLong(key, value).apply()

/**
 * Extension function to [SharedPreferences] that will put set of strings and immediately apply changes
 */
fun SharedPreferences.putAndApply(key: String, value: Set<String>) = edit().putStringSet(key, value).apply()

/**
 * Extension function to [SharedPreferences] that will remove a key and immediately apply changes
 */
fun SharedPreferences.removeAndApply(key: String) = edit().remove(key).apply()

/**
 * Long property that is stored directly into the given [SharedPreferences]
 */
fun SharedPreferences.longSharedPreferencesProperty(key: String, defaultValue: Long): ReadWriteProperty<Any?, Long> {
    return sharedPreferencesProperty(
        key,
        defaultValue,
        SharedPreferences::getLong,
        SharedPreferences.Editor::putLong
    )
}

/**
 * Int property that is stored directly into the given [SharedPreferences]
 */
fun SharedPreferences.intSharedPreferencesProperty(key: String, defaultValue: Int): ReadWriteProperty<Any?, Int> {
    return sharedPreferencesProperty(
        key,
        defaultValue,
        SharedPreferences::getInt,
        SharedPreferences.Editor::putInt
    )
}

/**
 * Nullable int property that is stored directly into the given [SharedPreferences]
 */
fun SharedPreferences.nullableIntSharedPreferencesProperty(
    key: String,
    defaultValue: Int? = null
): ReadWriteProperty<Any?, Int?> {
    return sharedPreferencesNullableProperty(
        key,
        defaultValue,
        { _key: String, _defaultValue: Int? ->
            getInt(_key, _defaultValue ?: -1).let { if (it == (_defaultValue ?: -1)) null else it }
        },
        SharedPreferences.Editor::putInt
    )
}

/**
 * Boolean property that is stored directly into the given [SharedPreferences]
 */
fun SharedPreferences.booleanSharedPreferencesProperty(
    key: String,
    defaultValue: Boolean
): ReadWriteProperty<Any?, Boolean> {
    return sharedPreferencesProperty(
        key,
        defaultValue,
        SharedPreferences::getBoolean,
        SharedPreferences.Editor::putBoolean
    )
}

/**
 * Nullable long property that is stored directly into the given [SharedPreferences]
 */
fun SharedPreferences.nullableLongSharedPreferencesProperty(
    key: String,
    defaultValue: Long? = null
): ReadWriteProperty<Any?, Long?> {
    return sharedPreferencesNullableProperty(
        key,
        defaultValue,
        { _key: String, _defaultValue: Long? ->
            getLong(_key, _defaultValue ?: -1L).let { if (it == (_defaultValue ?: -1L)) null else it }
        },
        SharedPreferences.Editor::putLong
    )
}

/**
 * String property that is stored directly into the given [SharedPreferences]
 */
fun SharedPreferences.stringSharedPreferencesProperty(
    key: String,
    defaultValue: String
): ReadWriteProperty<Any?, String> {
    return sharedPreferencesProperty(
        key,
        defaultValue,
        SharedPreferences::getString,
        SharedPreferences.Editor::putString
    )
}

/**
 * Nullable string property that is stored directly into the given [SharedPreferences]
 */
fun SharedPreferences.nullableStringSharedPreferencesProperty(
    key: String,
    defaultValue: String? = null
): ReadWriteProperty<Any?, String?> {
    return sharedPreferencesNullableProperty(
        key,
        defaultValue,
        SharedPreferences::getString,
        SharedPreferences.Editor::putString
    )
}

/**
 * Any enum property that is stored directly into the given [SharedPreferences]
 */
inline fun <reified T : Enum<T>> SharedPreferences.enumSharedPreferencesProperty(
    key: String,
    defaultValue: T
): ReadWriteProperty<Any?, T> {
    return sharedPreferencesProperty(
        key,
        defaultValue,
        { _key: String, _defaultValue: T ->
            enumValues<T>()[getInt(_key, _defaultValue.ordinal)]
        },
        { _key: String, value: T ->
            putInt(_key, value.ordinal)
        }
    )
}

/**
 * Any nullable enum property that is stored directly into the given [SharedPreferences]
 */
inline fun <reified T : Enum<T>> SharedPreferences.nullableEnumSharedPreferencesProperty(
    key: String,
    defaultValue: T? = null
): ReadWriteProperty<Any?, T?> {
    return sharedPreferencesNullableProperty(
        key,
        defaultValue,
        { _key: String, _defaultValue: T? ->
            enumValues<T>().getOrNull(getInt(_key, _defaultValue?.ordinal ?: -1))
        },
        { _key: String, value: T ->
            putInt(_key, value.ordinal)
        }
    )
}

/**
 * Nullable ZonedDateTime property that is stored directly into the given [SharedPreferences]
 */
fun SharedPreferences.nullableZonedDateTimeSharedPreferencesProperty(
    key: String,
    defaultValue: ZonedDateTime? = null
): ReadWriteProperty<Any?, ZonedDateTime?> {
    return object : ReadWriteProperty<Any?, ZonedDateTime?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): ZonedDateTime? {
            val timestamp = getString(key, Rfc3339ZonedDateTimeAdapter.toJson(defaultValue))
            return Rfc3339ZonedDateTimeAdapter.fromJson(timestamp)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: ZonedDateTime?) {
            if (value == null) {
                edit().remove(key).apply()
            } else {
                edit().putString(key, Rfc3339ZonedDateTimeAdapter.toJson(value)).apply()
            }
        }
    }
}

/**
 * Nullable ZonedDateTime property that is stored directly into the given [SharedPreferences]
 */
fun SharedPreferences.nullableInstantSharedPreferencesProperty(
    key: String,
    defaultValue: Instant? = null
): ReadWriteProperty<Any?, Instant?> {
    return object : ReadWriteProperty<Any?, Instant?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Instant? {
            val timestamp = getString(key, Rfc3339InstantAdapter.toJson(defaultValue))
            return Rfc3339InstantAdapter.fromJson(timestamp)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Instant?) {
            if (value == null) {
                edit().remove(key).apply()
            } else {
                edit().putString(key, Rfc3339InstantAdapter.toJson(value)).apply()
            }
        }
    }
}

/**
 * Generic property that stores its not null value directly into the [SharedPreferences] using the given accessor functions.
 */
inline fun <T> SharedPreferences.sharedPreferencesProperty(
    key: String,
    defaultValue: T,
    crossinline retrieve: SharedPreferences.(String, T) -> T,
    crossinline put: SharedPreferences.Editor.(String, T) -> SharedPreferences.Editor
): ReadWriteProperty<Any?, T> {
    return object : ReadWriteProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return retrieve(key, defaultValue)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            edit().put(key, value).apply()
        }
    }
}

/**
 * Generic property that stores its nullable value directly into the [SharedPreferences] using the given accessor functions.
 */
inline fun <T> SharedPreferences.sharedPreferencesNullableProperty(
    key: String,
    defaultValue: T?,
    crossinline retrieve: SharedPreferences.(String, T?) -> T?,
    crossinline put: SharedPreferences.Editor.(String, T) -> SharedPreferences.Editor
): ReadWriteProperty<Any?, T?> {
    return object : ReadWriteProperty<Any?, T?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
            return retrieve(key, defaultValue)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
            if (value == null) {
                edit().remove(key).apply()
            } else {
                edit().put(key, value).apply()
            }
        }
    }
}

fun SharedPreferences.observeInt(key: String, defaultValue: Int): Observable<Int> {
    return observe(
        key,
        defaultValue
    ) { _key: String, _defValue: Int? ->
        getInt(_key, _defValue!!)
    }.map { it.get() }
}

fun SharedPreferences.observeNullableInt(key: String, defaultValue: Int? = null): Observable<Optional<Int>> {
    return observe(
        key,
        defaultValue
    ) { _key, _defValue ->
        getInt(_key, _defValue ?: -1).let { if (it == (_defValue ?: -1)) null else it }
    }
}

fun SharedPreferences.observeLong(key: String, defaultValue: Long): Observable<Long> {
    return observe(
        key,
        defaultValue
    ) { _key: String, _defValue: Long? ->
        getLong(_key, _defValue!!)
    }.map { it.get() }
}

fun SharedPreferences.observeNullableLong(key: String, defaultValue: Long? = null): Observable<Optional<Long>> {
    return observe(
        key,
        defaultValue
    ) { _key, _defValue ->
        getLong(_key, _defValue ?: -1).let { if (it == (_defValue ?: -1)) null else it }
    }
}

fun SharedPreferences.observeBoolean(key: String, defaultValue: Boolean = false): Observable<Boolean> {
    return observe(
        key,
        defaultValue
    ) { _key, _defValue ->
        getBoolean(_key, _defValue!!)
    }.map { it.get() }
}

fun SharedPreferences.observeNullableString(key: String, defaultValue: String? = null): Observable<Optional<String>> {
    return observe(
        key,
        defaultValue,
        SharedPreferences::getString
    )
}

fun SharedPreferences.observeNullableZonedDateTime(key: String, defaultValue: ZonedDateTime? = null): Observable<Optional<ZonedDateTime>> {
    return observe(
        key,
        defaultValue
    ) { _key, _defValue ->
        val timestamp = getString(_key, Rfc3339ZonedDateTimeAdapter.toJson(_defValue))
        Rfc3339ZonedDateTimeAdapter.fromJson(timestamp)
    }
}

fun SharedPreferences.observeNullableInstant(key: String, defaultValue: Instant? = null): Observable<Optional<Instant>> {
    return observe(
        key,
        defaultValue
    ) { _key, _defValue ->
        val timestamp = getString(_key, Rfc3339InstantAdapter.toJson(_defValue))
        Rfc3339InstantAdapter.fromJson(timestamp)
    }
}

/**
 * Creates observable that registers shared preferences listener when subscribed to and unregisters it when disposed
 */
fun <T> SharedPreferences.observe(
    key: String,
    defaultValue: T?,
    retrieve: SharedPreferences.(String, T?) -> T?
): Observable<Optional<T>> {
    return Observable.create<Optional<T>> { emitter ->
        emitter.onNext(Optional.ofNullable(retrieve(key, defaultValue)))
        val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, k ->
            if (k == key) {
                emitter.onNext(Optional.ofNullable(sharedPreferences.retrieve(key, defaultValue)))
            }
        }

        // Save from garbage collection
        sharedPreferenceListeners.add(prefsListener)
        registerOnSharedPreferenceChangeListener(prefsListener)

        emitter.setDisposable(Disposables.fromRunnable {
            unregisterOnSharedPreferenceChangeListener(prefsListener)
            sharedPreferenceListeners.remove(prefsListener)
        })
    }.observeOn(Schedulers.io()) // because of shared preferences listener is on main thread
        .distinctUntilChanged()
}