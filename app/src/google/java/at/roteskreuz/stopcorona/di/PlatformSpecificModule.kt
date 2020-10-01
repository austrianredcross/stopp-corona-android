package at.roteskreuz.stopcorona.di

import org.koin.dsl.module.module

/**
 * Module for providing repositories.
 */
val platformDependentModule = at.roteskreuz.stopcorona.gms.di.exposureModule

val bridgeModule = module {

}