package at.roteskreuz.stopcorona.hms.di

import at.roteskreuz.stopcorona.commonexposure.CommonExposureClient
import at.roteskreuz.stopcorona.hms.HuaweiExposureClient
import com.huawei.hms.api.HuaweiApiAvailability
import com.huawei.hms.contactshield.ContactShield
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module.Module
import org.koin.dsl.module.module


fun getExposureModule(intentServiceClass: Class<*>): Module {

    return module {

        single { HuaweiApiAvailability.getInstance() }
        single { ContactShield.getContactShieldEngine(androidApplication()) }

        single<CommonExposureClient> {
            HuaweiExposureClient(androidApplication(), get(), get(), intentServiceClass)
        }

    }


}

