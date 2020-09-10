package at.roteskreuz.stopcorona.gms.di

import at.roteskreuz.stopcorona.commonexposure.CommonExposureClient
import at.roteskreuz.stoppcorona.google.GoogleExposureClient
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.nearby.Nearby
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module.module


val exposureModule = module {

    single { GoogleApiAvailability.getInstance() }
    single { Nearby.getExposureNotificationClient(androidApplication()) }

    single<CommonExposureClient> {
        GoogleExposureClient(androidApplication(), get(), get())
    }

}
