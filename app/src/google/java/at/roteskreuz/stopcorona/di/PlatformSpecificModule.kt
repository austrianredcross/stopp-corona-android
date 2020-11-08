package at.roteskreuz.stopcorona.di

import at.roteskreuz.stopcorona.constants.Constants.ExposureNotification.MIN_SUPPORTED_GOOGLE_PLAY_APK_VERSION
import org.koin.dsl.module.module

/**
 * Module for providing repositories.
 */
val platformDependentModule = at.roteskreuz.stopcorona.gms.di.getExposureModule(MIN_SUPPORTED_GOOGLE_PLAY_APK_VERSION)

val bridgeModule = module {

}