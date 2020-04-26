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

    override suspend fun createTextFileFromString(text: String, fileName: String) {
        withContext(coroutineContext) {
            val destFile = getFile(fileName)
            if (!destFile.exists()) {
                saveTextFile(destFile, text)
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
        saveTextFile(file, text)
    }

    private suspend fun saveTextFile(file: File, text: String) {
        withContext(coroutineContext) {
            val tmpDestFile: File = File.createTempFile(file.name, null, file.parentFile)
            // Write to temp file and only move to the requested file name when we know
            // we succeeded. Otherwise we might leave a half written file around which will
            // never be replaced because ´if (!fileExists(destFilename))´ thinks everything is ok
            FileOutputStream(tmpDestFile).use { output ->
                output.write(text.toByteArray())
                output.flush() // Flush buffers to OS
                output.fd.sync() // Make sure OS writes all the way to disc
            }

            if (!tmpDestFile.renameTo(file)) {
                throw IOException("Could not move temporary file to final destination ${tmpDestFile.name}")
            }
        }
    }

    /**
     * Gets a [File] for file with path [fileName], relative to the applciation's base folder
     */
    private fun getFile(fileName: String): File {
        return File(applicationInternalBaseFolder, fileName)
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
}