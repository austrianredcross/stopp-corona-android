package at.roteskreuz.stopcorona.model.repositories

import at.roteskreuz.stopcorona.model.api.ApiInteractor
import at.roteskreuz.stopcorona.model.assets.AssetInteractor
import at.roteskreuz.stopcorona.model.db.dao.ConfigurationDao
import at.roteskreuz.stopcorona.model.entities.configuration.DbConfiguration
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.utils.asDbObservable
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Repository to download, cache and provide configuration of questionnaires.
 * Cache is max 1h old.
 */
interface ConfigurationRepository {

    /**
     * Import configuration from app's resources.
     */
    suspend fun populateConfigurationIfEmpty()

    /**
     * Download configuration and persist them for later observing.
     *
     * Direct API call
     */
    suspend fun fetchAndStoreConfiguration()

    /**
     * Observe cached version of configuration.
     */
    fun observeConfiguration(): Observable<DbConfiguration>
}

class ConfigurationRepositoryImpl(
    private val appDispatchers: AppDispatchers,
    private val apiInteractor: ApiInteractor,
    private val configurationDao: ConfigurationDao,
    private val assetInteractor: AssetInteractor
) : ConfigurationRepository,
    CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = appDispatchers.Default

    override suspend fun populateConfigurationIfEmpty() {
        withContext(coroutineContext) {
            if (configurationDao.isConfigurationEmpty()) {
                val apiConfigurationHolder = assetInteractor.configuration()
                configurationDao.updateConfiguration(apiConfigurationHolder.configuration)
            }
        }
    }

    override suspend fun fetchAndStoreConfiguration() {
        withContext(coroutineContext) {
            val apiConfiguration = apiInteractor.getConfiguration()
            configurationDao.updateConfiguration(apiConfiguration)
        }
    }

    override fun observeConfiguration(): Observable<DbConfiguration> {
        return configurationDao.observeConfiguration().asDbObservable()
    }
}