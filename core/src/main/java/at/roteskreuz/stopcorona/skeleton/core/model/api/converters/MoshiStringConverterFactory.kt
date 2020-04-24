package at.roteskreuz.stopcorona.skeleton.core.model.api.converters

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

/**
 * Uses Moshi adapters to create string converters which can be used to convert @Query params and other kinds of params
 */
class MoshiStringConverterFactory(private val moshi: Moshi) : Converter.Factory() {

    override fun stringConverter(
        type: Type,
        annotations: Array<Annotation>?,
        retrofit: Retrofit?
    ): Converter<*, String?> {
        val adapter: JsonAdapter<in Any> = moshi.adapter(type)
        return StringConverter(adapter)
    }

    inner class StringConverter<T>(val adapter: JsonAdapter<T>) : Converter<T, String?> {

        override fun convert(value: T): String? {

            val jsonValue = adapter.toJson(value)

            return if (jsonValue.startsWith("\"") && jsonValue.endsWith("\"")) {
                // Strip enclosing quotes for json String types
                jsonValue.substring(1, jsonValue.length - 1)
            } else {
                jsonValue
            }
        }
    }
}