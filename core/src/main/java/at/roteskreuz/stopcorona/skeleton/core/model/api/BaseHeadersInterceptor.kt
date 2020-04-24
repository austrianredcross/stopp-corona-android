package at.roteskreuz.stopcorona.skeleton.core.model.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException

/**
 * Interceptor that inserts base headers to every http(s) request.
 */
data class BaseHeadersInterceptor(
    private val headerParams: Map<String, String>
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        return chain.proceed(
            originalRequest
                .newBuilder()
                .apply {
                    for ((key, value) in headerParams) {
                        addHeader(key, value)
                    }
                }
                .build()
        )
    }
}

/**
 * Extension for shorter code.
 */
fun OkHttpClient.Builder.addHeaders(
    vararg headers: Pair<String, String>
): OkHttpClient.Builder {
    return addInterceptor(BaseHeadersInterceptor(headers.toMap()))
}