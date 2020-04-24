package at.roteskreuz.stopcorona.di

import at.roteskreuz.stopcorona.model.repositories.ReportingRepository
import at.roteskreuz.stopcorona.model.repositories.ReportingRepositoryImpl
import org.koin.dsl.module.module

/**
 * Module for providing scopes.
 */
val scopeModule = module {

    /**
     * Living only during the sending of the sickness certificate.
     */
    scope<ReportingRepository>(
        ReportingRepository.SCOPE_NAME) {
        ReportingRepositoryImpl(
            appDispatchers = get(),
            apiInteractor = get(),
            configurationRepository = get(),
            nearbyRecordDao = get(),
            infectionMessageDao = get(),
            cryptoRepository = get(),
            infectionMessengerRepository = get(),
            quarantineRepository = get(),
            databaseCleanupManager = get()
        )
    }

}