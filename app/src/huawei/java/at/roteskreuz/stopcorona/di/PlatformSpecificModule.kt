package at.roteskreuz.stopcorona.di

import at.roteskreuz.stopcorona.hms.ContactShieldServiceProcessor
import at.roteskreuz.stopcorona.ContactShieldServiceProcessorImpl
import org.koin.dsl.module.module

/**
 * Module for providing repositories.
 */
val platformDependentModule = at.roteskreuz.stopcorona.hms.di.getExposureModule()

val bridgeModule = module {
    single<ContactShieldServiceProcessor> {
        ContactShieldServiceProcessorImpl()
    }
}



