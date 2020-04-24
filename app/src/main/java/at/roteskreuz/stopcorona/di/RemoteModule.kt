package at.roteskreuz.stopcorona.di

import android.content.Context
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.constants.Constants.API.Header
import at.roteskreuz.stopcorona.constants.FlavorConstants.API.CERTIFICATE_CHAIN
import at.roteskreuz.stopcorona.constants.FlavorConstants.API.HOSTNAME
import at.roteskreuz.stopcorona.constants.isBeta
import at.roteskreuz.stopcorona.constants.isDebug
import at.roteskreuz.stopcorona.model.api.ApiDescription
import at.roteskreuz.stopcorona.model.api.ApiInteractor
import at.roteskreuz.stopcorona.model.api.ApiInteractorImpl
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

/**
 * Module for Rest Service.
 */
val remoteModule = module {

    single {
        Cache(get<Context>().cacheDir, Constants.API.HTTP_CACHE_SIZE)
    }

    single {
        CertificatePinner.Builder().apply {
            CERTIFICATE_CHAIN.forEach { pin ->
                add(HOSTNAME, pin)
            }
        }.build()
    }

    single {
        createOkHttpClient(if (isDebug || isBeta) BODY else NONE) {
            addHeaders(
                Header.AUTHORIZATION_KEY to Header.AUTHORIZATION_VALUE,
                Header.APP_ID_KEY to Header.APP_ID_VALUE
            )
            cache(get())
            certificatePinner(get())
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
            okHttpClient = get(),
            moshi = get()
        )
    }

    single<ApiInteractor> {
        ApiInteractorImpl(
            appDispatchers = get(),
            apiDescription = get(),
            dataPrivacyRepository = get()
        )
    }

    single {
        FirebaseMessaging.getInstance()
    }
}