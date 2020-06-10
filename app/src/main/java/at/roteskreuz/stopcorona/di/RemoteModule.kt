package at.roteskreuz.stopcorona.di

import android.content.Context
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.constants.Constants.API.CERTIFICATE_CHAIN_DEFAULT
import at.roteskreuz.stopcorona.constants.Constants.API.CERTIFICATE_CHAIN_TAN
import at.roteskreuz.stopcorona.constants.Constants.API.HOSTNAME
import at.roteskreuz.stopcorona.constants.Constants.API.HOSTNAME_TAN
import at.roteskreuz.stopcorona.constants.Constants.API.Header
import at.roteskreuz.stopcorona.constants.isBeta
import at.roteskreuz.stopcorona.constants.isDebug
import at.roteskreuz.stopcorona.di.CertificatePinnerTag.defaultCertificatePinnerTag
import at.roteskreuz.stopcorona.di.CertificatePinnerTag.tanCertificatePinnerTag
import at.roteskreuz.stopcorona.model.api.*
import at.roteskreuz.stopcorona.model.entities.infection.info.LocalDateNotIsoAdapter
import at.roteskreuz.stopcorona.skeleton.core.di.createApi
import at.roteskreuz.stopcorona.skeleton.core.di.createMoshi
import at.roteskreuz.stopcorona.skeleton.core.di.createOkHttpClient
import at.roteskreuz.stopcorona.skeleton.core.model.api.addHeaders
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.Cache
import okhttp3.CertificatePinner
import okhttp3.logging.HttpLoggingInterceptor.Level.BODY
import okhttp3.logging.HttpLoggingInterceptor.Level.NONE
import org.koin.dsl.module.module

object CertificatePinnerTag {
    const val defaultCertificatePinnerTag = "default"
    const val tanCertificatePinnerTag = "TAN"
}

/**
 * Module for Rest Service.
 */
val remoteModule = module {

    single {
        Cache(get<Context>().cacheDir, Constants.API.HTTP_CACHE_SIZE)
    }

    single(name = defaultCertificatePinnerTag) {
        CertificatePinner.Builder().apply {
            CERTIFICATE_CHAIN_DEFAULT.forEach { pin ->
                add(HOSTNAME, pin)
            }
        }.build()
    }

    single(name = tanCertificatePinnerTag) {
        CertificatePinner.Builder().apply {
            CERTIFICATE_CHAIN_TAN.forEach { pin ->
                add(HOSTNAME_TAN, pin)
            }
        }.build()
    }

    single(name = defaultCertificatePinnerTag) {
        createOkHttpClient(if (isDebug || isBeta) BODY else NONE) {
            addHeaders(
                Header.AUTHORIZATION_KEY to Header.AUTHORIZATION_VALUE,
                Header.APP_ID_KEY to Header.APP_ID_VALUE
            )
            cache(get())
            certificatePinner(get(defaultCertificatePinnerTag))
        }
    }

    single(name = tanCertificatePinnerTag) {
        createOkHttpClient(if (isDebug || isBeta) BODY else NONE) {
            addHeaders(
                Header.AUTHORIZATION_KEY to Header.AUTHORIZATION_VALUE,
                Header.APP_ID_KEY to Header.APP_ID_VALUE
            )
            cache(get())
            certificatePinner(get(tanCertificatePinnerTag))
        }
    }

    single {
        createMoshi {
            add(LocalDateNotIsoAdapter)
        }
    }

    single {
        createApi<ApiDescription>(
            baseUrl = Constants.API.BASE_URL,
            okHttpClient = get(defaultCertificatePinnerTag),
            moshi = get()
        )
    }

    single {
        createApi<TanApiDescription>(
            baseUrl = Constants.API.BASE_URL_TAN,
            okHttpClient = get(tanCertificatePinnerTag),
            moshi = get()
        )
    }

    single {
        createApi<TrackingKeysDescription>(
            baseUrl = Constants.API.BASE_URL_TRACKING_KEYS,
            okHttpClient = createOkHttpClient(),
            moshi = get()
        )
    }

    single<ApiInteractor> {
        ApiInteractorImpl(
            appDispatchers = get(),
            apiDescription = get(),
            tanApiDescription = get(),
            trackingKeysDescription = get(),
            dataPrivacyRepository = get()
        )
    }

    single {
        FirebaseMessaging.getInstance()
    }
}