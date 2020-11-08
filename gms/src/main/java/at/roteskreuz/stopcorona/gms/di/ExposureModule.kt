package at.roteskreuz.stopcorona.gms.di

import at.roteskreuz.stopcorona.commonexposure.CommonExposureClient
import at.roteskreuz.stopcorona.gms.GoogleExposureClient
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.nearby.Nearby
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module.Module
import org.koin.dsl.module.module

fun getExposureModule(minSupportedPlayApkVersion : Int) : Module {
    return module {

        single { GoogleApiAvailability.getInstance() }
        single { Nearby.getExposureNotificationClient(androidApplication()) }

        single<CommonExposureClient> {
            GoogleExposureClient(androidApplication(), get(), get(), minSupportedPlayApkVersion)
        }
    }
}
