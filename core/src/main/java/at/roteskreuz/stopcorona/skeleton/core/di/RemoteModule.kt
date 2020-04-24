package at.roteskreuz.stopcorona.skeleton.core.di

import at.roteskreuz.stopcorona.skeleton.core.model.api.converters.*
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BODY
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Creates basic Ok http client with enabled logging.
 */
fun createOkHttpClient(
    loggingLevel: HttpLoggingInterceptor.Level = BODY,
    builder: OkHttpClient.Builder.() -> OkHttpClient.Builder = { this }
): OkHttpClient {
    return OkHttpClient.Builder()
        .builder()
        .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(loggingLevel))
        .build()
}

/**
 * Creates basic retrofit API service.
 */
inline fun <reified T> createApi(
    baseUrl: String,
    okHttpClient: OkHttpClient,
    moshi: Moshi
): T {
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(UnitConverterFactory)
        .addConverterFactory(
            MoshiConverterFactory.create(
                moshi
            ).withNullSerialization()
        )
        .addConverterFactory(
            MoshiStringConverterFactory(
                moshi
            )
        )
        .client(okHttpClient)
        .build()
        .create(T::class.java)
}

/**
 * Creates json parser.
 */
fun createMoshi(
    builder: Moshi.Builder.() -> Moshi.Builder = { this }
): Moshi {
    return Moshi.Builder()
        .builder()
        .add(InstantAdapter)
        .add(LocalDateAdapter)
        .add(Rfc3339ZonedDateTimeAdapter)
        .build()
}