package at.roteskreuz.stopcorona.di

import at.roteskreuz.stopcorona.HuaweiExposureClient
import at.roteskreuz.stopcorona.commonexposure.CommonExposureClient
import com.huawei.hms.api.HuaweiApiAvailability
import com.huawei.hms.contactshield.ContactShield
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module.module

val exposureModule = module {

        single { HuaweiApiAvailability.getInstance() }
        single { ContactShield.getContactShieldEngine(androidApplication()) }

        single<CommonExposureClient> {
            HuaweiExposureClient(androidApplication(), get(), get())
        }
}

