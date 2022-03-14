package at.roteskreuz.stopcorona.di

import android.bluetooth.BluetoothAdapter
import at.roteskreuz.stopcorona.model.managers.BluetoothManager
import at.roteskreuz.stopcorona.model.managers.ExposureNotificationManager
import at.roteskreuz.stopcorona.model.managers.ExposureNotificationManagerImpl
import at.roteskreuz.stopcorona.model.repositories.*
import org.koin.dsl.module.module

/**
 * Module for providing repositories.
 */
val repositoryModule = module {

    single<FilesRepository> {
        FilesRepositoryImpl(
            appDispatchers = get(),
            contextInteractor = get()
        )
    }

    single<OnboardingRepository> {
        OnboardingRepositoryImpl(
            preferences = get()
        )
    }

    single<ConfigurationRepository> {
        ConfigurationRepositoryImpl(
            appDispatchers = get(),
            apiInteractor = get(),
            configurationDao = get(),
            assetInteractor = get()
        )
    }

    single<DashboardRepository> {
        DashboardRepositoryImpl(
            preferences = get()
        )
    }


    single<DiagnosisKeysRepository> {
        DiagnosisKeysRepositoryImpl(
            appDispatchers = get(),
            apiInteractor = get(),
            sessionDao = get(),
            temporaryExposureKeysDao = get(),
            preferences = get(),
            quarantineRepository = get(),
            workManager = get(),
            exposureNotificationRepository = get(),
            configurationRepository = get(),
            notificationsRepository = get()
        )
    }

    single<NotificationsRepository> {
        NotificationsRepositoryImpl(
            appDispatchers = get(),
            contextInteractor = get(),
            dataPrivacyRepository = get()
        )
    }

    single<DataPrivacyRepository> {
        DataPrivacyRepositoryImpl(
            preferences = get()
        )
    }

    single<QuarantineRepository> {
        QuarantineRepositoryImpl(
            preferences = get(),
            configurationRepository = get(),
            workManager = get()
        )
    }

    single<BluetoothRepository> {
        BluetoothRepositoryImpl(BluetoothAdapter.getDefaultAdapter(), get<BluetoothManager>())
    }

    single<ExposureNotificationRepository> {
        ExposureNotificationRepositoryImpl(
            appDispatchers = get(),
            bluetoothManager = get(),
            configurationRepository = get(),
            exposureNotificationClient = get(),
            filesRepository = get()
        )
    }

    single<ExposureNotificationManager> (createOnStart = true) {
        ExposureNotificationManagerImpl(
            appDispatchers = get(),
            preferences = get(),
            exposureNotificationRepository = get(),
            bluetoothRepository = get(),
            googlePlayAvailability = get(),
            contextInteractor = get(),
            quarantineRepository = get(),
            workManager = get()
        )
    }

    single<DiaryRepository> {
        DiaryRepositoryImpl(
            appDispatchers = get(),
            diaryDao = get()
        )
    }

    single<StatisticsRepository> {
        StatisticsRepositoryImpl(
            appDispatchers = get(),
            apiInteractor = get()
        )
    }

    single<SunDownerRepository> {
        SunDownerRepositoryImpl(
            preferences = get()
        )
    }
}
