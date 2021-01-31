package at.roteskreuz.stopcorona.di

import android.content.Context
import at.roteskreuz.stopcorona.HuaweiExposureClient
import at.roteskreuz.stopcorona.commonexposure.CommonExposureClient
import com.huawei.hms.api.HuaweiApiAvailability
import com.huawei.hms.contactshield.ContactShield
import com.huawei.hms.contactshield.ContactShieldEngine
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module.module

val exposureModule = module {

    single { HuaweiApiAvailability.getInstance() }
    single { ContactShield.getContactShieldEngine(androidApplication()) }

    single<CommonExposureClient> {
        HuaweiExposureClient(androidApplication(),
            get<HuaweiApiAvailability>(),
            get<ContactShieldEngine>(),
            androidApplication().getSharedPreferences(PREFS_HMS_DEBUG, Context.MODE_PRIVATE))
    }
}

const val PREFS_HMS_DEBUG = "PREFS_HMS_DEBUG"

