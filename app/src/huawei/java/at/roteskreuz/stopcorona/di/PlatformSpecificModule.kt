package at.roteskreuz.stopcorona.di

import at.roteskreuz.stopcorona.ContactShieldService

/**
 * Module for providing repositories.
 */
val platformDependentModule = at.roteskreuz.stopcorona.hms.di.getExposureModule(ContactShieldService::class.java)



