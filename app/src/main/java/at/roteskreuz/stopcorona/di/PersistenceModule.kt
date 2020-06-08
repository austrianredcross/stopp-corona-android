package at.roteskreuz.stopcorona.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.model.db.DefaultDatabase
import at.roteskreuz.stopcorona.model.manager.*
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module.module

/**
 * Module for providing persistence related dependencies.
 */
internal val persistenceModule = module {

    single<SharedPreferences> {
        androidApplication().getSharedPreferences(Constants.Prefs.FILE_NAME, Context.MODE_PRIVATE)
    }

    single {
        Room.databaseBuilder(androidContext(), DefaultDatabase::class.java, Constants.DB.FILE_NAME)
            .addMigrations(*DefaultDatabase.migrations)
            .build()
    }


    single<PreferencesMigrationManager>(createOnStart = true) {
        PreferencesMigrationManagerImpl(
            preferences = get()
        )
    }

    single<DatabaseCleanupManager>(createOnStart = true) {
        DatabaseCleanupManagerImpl(
            appDispatchers = get(),
            configurationRepository = get(),
            infectionMessageDao = get()
        )
    }

    single<ChangelogManager> {
        ChangelogManagerImpl(
            preferences = get(),
            contextInteractor = get()
        )
    }

    // DAOs

    single {
        get<DefaultDatabase>().configurationDao()
    }

    single {
        get<DefaultDatabase>().infectionMessageDao()
    }
}