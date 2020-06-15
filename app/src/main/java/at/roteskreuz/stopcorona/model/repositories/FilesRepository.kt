package at.roteskreuz.stopcorona.model.repositories

import androidx.annotation.RawRes
import at.roteskreuz.stopcorona.BuildConfig
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import java.io.*
import kotlin.coroutines.CoroutineContext

/**
 * Repository for working with files stored in the app's private storage.
 */
interface FilesRepository {

    /**
     * Writes text into a new file in the app's storage. If the destination file already
     * exists, it will not be overwritten.
     *
     * @param text: The content to write.
     * @param fileName a file name, relative to the apps storage
     *
     * @throws IOException
     */
    suspend fun createTextFileFromString(text: String, fileName: String)

    /**
     * Write input stream into a new file in the cache storage. If the destination file already
     * exists, it will not be overwritten.
     *
     * @param inputStream: The content to write.
     * @param fileName a file name, relative to the apps storage
     *
     * @throws IOException
     */
    suspend fun createCacheFileFromInputStream(inputStream: InputStream, fileName: String)

    /**
     * Loads a raw resource file and returns it's content as string
     */
    suspend fun loadRawResource(fileName: String): String

    /**
     * Get [InputStream] for file on device storage
     *
     * @param fileName
     */
    fun getInputStream(fileName: String): InputStream

    /**
     * Writes text into a file in the app's storage. This implementation uses atomic
     * replace to avoid leaving a corrupted file around.
     * @param text: The content to write.
     * @param fileName a file name, relative to the apps storage
     *
     * @throws IOException
     */
    suspend fun saveTextFile(text: String, fileName: String)

    /**
     * Get url of file on app storage.
     */
    fun getFileUrl(absoluteUrl: String): String

    /**
     * Get a [File] for file with path [fileName], relative to the applciation's base folder.
     */
    fun getFile(fileName: String): File

    /**
     * Get a [File] for file with path [fileName], relative to the cache base folder.
     */
    fun getCacheFile(fileName: String): File

    /**
     * Remove file in cache folder.
     */
    suspend fun removeCacheFile(fileName: String)
}

class FilesRepositoryImpl(
    private val appDispatchers: AppDispatchers,
    private val contextInteractor: ContextInteractor
) : FilesRepository,
    CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = appDispatchers.IO

    /**
     * Path to /data/data/packageName/files/ or equivalent /data/user/0/packageName/files/.
     */
    private val applicationInternalBaseFolder: File
        get() = contextInteractor.filesDir

    private val cacheFolder: File
        get() = contextInteractor.applicationContext.cacheDir

    override suspend fun createTextFileFromString(text: String, fileName: String) {
        withContext(coroutineContext) {
            val destFile = getFile(fileName)
            if (destFile.exists().not()) {
                text.saveTo(destFile)
            }
        }
    }

    override suspend fun createCacheFileFromInputStream(inputStream: InputStream, fileName: String) {
        withContext(coroutineContext) {
            val destFile = getCacheFile(fileName)
            if (destFile.exists().not()) {
                inputStream.saveTo(destFile)
            }
        }
    }

    override suspend fun loadRawResource(fileName: String): String {
        val resId = contextInteractor.resources.getIdentifier(fileName, "raw", BuildConfig.APPLICATION_ID)
        if (resId == 0) {
            throw FileNotFoundException(fileName)
        }
        return loadRawResource(resId) ?: throw FileNotFoundException(fileName)
    }

    override fun getInputStream(fileName: String): InputStream {
        return FileInputStream(getFile(fileName))
    }

    override suspend fun saveTextFile(text: String, fileName: String) {
        val file = getFile(fileName)
        text.saveTo(file)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun String.saveTo(file: File) {
        withContext(coroutineContext) {
            val tmpDestFile: File = File.createTempFile(file.name, null, file.parentFile)
            // Write to temp file and only move to the requested file name when we know
            // we succeeded. Otherwise we might leave a half written file around which will
            // never be replaced because ´if (!fileExists(destFilename))´ thinks everything is ok
            FileOutputStream(tmpDestFile).use { output ->
                output.write(toByteArray())
                output.flush() // Flush buffers to OS
                output.fd.sync() // Make sure OS writes all the way to disc
            }

            if (!tmpDestFile.renameTo(file)) {
                throw IOException("Could not move temporary file to final destination ${tmpDestFile.name}")
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun InputStream.saveTo(file: File) {
        withContext(coroutineContext) {
            val tmpDestFile: File = File.createTempFile(file.name, null, file.parentFile)
            // Write to temp file and only move to the requested file name when we know
            // we succeeded. Otherwise we might leave a half written file around which will
            // never be replaced because ´if (!fileExists(destFilename))´ thinks everything is ok
            use { input ->
                FileOutputStream(tmpDestFile).use { output ->
                    input.copyTo(output)
                    output.flush() // Flush buffers to OS
                    output.fd.sync() // Make sure OS writes all the way to disc
                }
            }

            if (!tmpDestFile.renameTo(file)) {
                throw IOException("Could not move temporary file to final destination ${tmpDestFile.name}")
            }
        }
    }

    override fun getFile(fileName: String): File {
        return File(applicationInternalBaseFolder, fileName)
    }

    override fun getCacheFile(fileName: String): File {
        return File(cacheFolder, fileName)
    }

    private suspend fun loadRawResource(@RawRes resId: Int): String? {
        return withContext(coroutineContext) {
            contextInteractor.resources.openRawResource(resId).use { inputStream ->
                val reader = BufferedReader(inputStream.reader())
                var content: String? = null
                reader.use { content = it.readText() }

                return@withContext content
            }
        }
    }

    override fun getFileUrl(absoluteUrl: String): String {
        return "file://${absoluteUrl}"
    }

    override suspend fun removeCacheFile(fileName: String) {
        withContext(coroutineContext) {
            getCacheFile(fileName).delete()
        }
    }
}