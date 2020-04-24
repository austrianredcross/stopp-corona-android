package at.roteskreuz.stopcorona.model.assets

import at.roteskreuz.stopcorona.model.entities.configuration.ApiConfigurationHolder
import at.roteskreuz.stopcorona.model.repositories.FilesRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Interactor that can load and parse bundled assets.
 */
interface AssetInteractor {

    /**
     * Load configuration from bundled JSON.
     */
    suspend fun configuration(): ApiConfigurationHolder
}

class AssetInteractorImpl(
    private val appDispatchers: AppDispatchers,
    private val moshi: Moshi,
    private val filesRepository: FilesRepository
) : AssetInteractor,
    CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = appDispatchers.IO

    override suspend fun configuration(): ApiConfigurationHolder = loadResourceAsJson("configuration")

    /**
     * Load json file with name [name] from raw resources into an object of type [T].
     *
     * @param name file name of the resources (not including the extension (.json)
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend inline fun <reified T> loadResourceAsJson(name: String): T {
        return withContext(coroutineContext) {
            val json: String = filesRepository.loadRawResource(name)
            val adapter = moshi.adapter(T::class.java)
            // We try to load precompiled data. If we failed to provide the right data let it crash!
            adapter.fromJson(json)!!
        }
    }
}