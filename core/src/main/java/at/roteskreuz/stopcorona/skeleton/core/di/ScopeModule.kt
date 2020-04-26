package at.roteskreuz.stopcorona.skeleton.core.di

import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchersImpl
import at.roteskreuz.stopcorona.skeleton.core.model.scope.ScopeConnector
import org.koin.dsl.module.module

/**
 * Module for providing scopes.
 */
internal val scopeModule = module {

    /**
     * Provide testable coroutine dispatchers.
     */
    single<AppDispatchers> {
        AppDispatchersImpl()
    }

    single { ScopeConnector() }
}