package at.roteskreuz.stopcorona.model.repositories.other

import android.content.SharedPreferences
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import at.roteskreuz.stopcorona.constants.Constants.Behavior.OFFLINE_SYNC_INTERVAL
import at.roteskreuz.stopcorona.constants.Constants.Prefs.OFFLINE_SYNC_PREFIX
import at.roteskreuz.stopcorona.model.api.ApiError
import at.roteskreuz.stopcorona.model.exceptions.DataFetchFailedException
import at.roteskreuz.stopcorona.model.exceptions.DataPopulationFailedException
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.repositories.ConfigurationRepository
import at.roteskreuz.stopcorona.model.repositories.DataPrivacyRepository
import at.roteskreuz.stopcorona.model.repositories.InfectionMessengerRepository
import at.roteskreuz.stopcorona.screens.mandatory_update.startMandatoryUpdateFragment
import at.roteskreuz.stopcorona.screens.routing.RouterActivity
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.utils.putAndApply
import kotlinx.coroutines.*
import timber.log.Timber

/**
 * Upon instantiation the [OfflineSyncer] will populate the config databases.
 * It will then listen for lifecycle events and, when coming back to foreground and at least
 * [OFFLINE_SYNC_INTERVAL] has passed since the last sync, it updates the configuration tables from
 * backend, silently ignoring all errors.
 */
interface OfflineSyncer {

    /**
     * Wait for all databases to be populated
     */
    suspend fun awaitDatabasePopulation()
}

class OfflineSyncerImpl(
    private val appDispatchers: AppDispatchers,
    private val contextInteractor: ContextInteractor,
    private val sharedPreferences: SharedPreferences,
    /**
     * This lifecycle owner belongs to the process to check start and stop of whole application.
     */
    private val processLifecycleOwner: LifecycleOwner,
    private val configurationRepository: ConfigurationRepository,
    private val dataPrivacyRepository: DataPrivacyRepository,
    private val infectionMessengerRepository: InfectionMessengerRepository
) : OfflineSyncer {

    companion object {
        private const val PREF_LAST_CONFIG_SYNC = OFFLINE_SYNC_PREFIX + "last_config_sync_time"
        private const val PREF_LAST_FETCH_INFECTION_MESSAGES = OFFLINE_SYNC_PREFIX + "last_infection_messages_sync_time"
    }

    private val populationDeferreds: List<Deferred<Unit>>

    /**
     * Ensure all databases are populated and then start offline sync
     */
    init {
        // Use dedicated context because we do not want to be canceled unless the process dies.
        // the population will sure be needed the next time we start
        with(CoroutineScope(appDispatchers.Default)) {
            populationDeferreds = populateDatabasesAsync()

            launch {
                try {
                    // Await population of databases before starting the sync
                    awaitDatabasePopulation()
                } finally {
                    // Start listening to Lifecycle events for synchronisation
                    processLifecycleOwner.lifecycle.addObserver(LifecycleListener())
                }
            }
        }
    }

    override suspend fun awaitDatabasePopulation() {
        populationDeferreds.awaitAll()
    }

    /**
     * This lifecycle listener is used to start fetching if outdated (configuration and infection warnings) once the application is
     * started. This should be connected to [processLifecycleOwner]. Once the application is stopped (last activity is stopped),
     * the fetching is cancelled.
     *
     * There is intentionally used new coroutine scope to not cancel populating initial data to DB.
     */
    private inner class LifecycleListener : LifecycleObserver {

        private var fetcherScope: CoroutineScope? = null

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onEnterForeground() {

            // Cancel pending updates for former language
            fetcherScope?.cancel()
            // Fetch updates for current language
            fetcherScope = CoroutineScope(SupervisorJob() + appDispatchers.IO)
            fetcherScope?.fetchUpdates()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun onEnterBackground() {
            // Do not use resources when in Background
            // Next time the app is started the data might be outdated anyway
            fetcherScope?.cancel()
        }
    }

    /**
     * The purpose of this method is to populate data that is absolutely needed for the app to run.
     * [RouterActivity] will wait for all population tasks to
     * finish and block application start up for that time.
     *
     * The concept has initially been created to populate databases from JSON at first run.
     *
     * Do not put long running or network bound tasks here.
     */
    private fun CoroutineScope.populateDatabasesAsync(): List<Deferred<Unit>> {
        return listOf(
            async { runPopulationAndLogExceptions { configurationRepository.populateConfigurationIfEmpty() } },
            async { runPopulationAndLogExceptions { infectionMessengerRepository.enqueueNextExposureMatching() } }
        )
    }

    @Suppress("DeferredResultUnused")
    private fun CoroutineScope.fetchUpdates() {
        async {
            // block execution until user approved GDPR
            dataPrivacyRepository.awaitForAcceptanceState()
            // update configuration
            runFetcherIfNeeded(PREF_LAST_CONFIG_SYNC) {
                //TODO: bring back after server sync https://tasks.pxp-x.com/browse/CTAA-1640
                //configurationRepository.fetchAndStoreConfiguration()
            }
            // read all infection messages
            runFetcherIfNeeded(PREF_LAST_FETCH_INFECTION_MESSAGES) {
                infectionMessengerRepository.fetchDecryptAndStoreNewMessages()
            }
        }
    }

    // See https://youtrack.jetbrains.com/issue/KT-34039
    @Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE")
    private suspend inline fun runPopulationAndLogExceptions(populator: suspend () -> Unit) {
        try {
            populator()
        } catch (e: CancellationException) {
            throw e // needs to be rethrown to coroutine subsystem to accept it
        } catch (e: Exception) {
            Timber.e(SilentError(DataPopulationFailedException(e)))
        }
    }

    // See https://youtrack.jetbrains.com/issue/KT-34039
    @Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE")
    private suspend inline fun runFetcherIfNeeded(lastSyncKey: String, fetcher: suspend () -> Unit) {
        try {
            val lastSync = sharedPreferences.getLong(lastSyncKey, 0)
            val timeSinceLastSync = System.currentTimeMillis() - lastSync

            // Do not sync if a last successful sync was in the last OFFLINE_SYNC_INTERVAL ms.
            // If the last sync appears to be in the future, we still sync as this can only happen
            // if the clock is off or was off during the last sync
            if (timeSinceLastSync in 0..OFFLINE_SYNC_INTERVAL.toMillis()) return

            fetcher()

            sharedPreferences.putAndApply(lastSyncKey, System.currentTimeMillis())
        } catch (e: CancellationException) {
            throw e // needs to be rethrown to coroutine subsystem to accept it
        } catch (_: ApiError.Critical.ForceUpdate) {
            contextInteractor.applicationContext.startMandatoryUpdateFragment()
        } catch (e: Exception) {
            Timber.e(SilentError(DataFetchFailedException(e)))
        }
    }
}
