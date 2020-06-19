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
     * Write input stream into a new file in the app's storage. If the destination file already
     * exists, it will not be overwritten.
     *
     * @param inputStream: The content to write.
     * @param fileName a file name, relative to the apps storage
     *
     * @throws IOException
     */
    suspend fun createFileFromInputStream(inputStream: InputStream, folder: String, fileName: String)

    /**
     * Loads a raw resource file and returns it's content as string
     */
    suspend fun loadRawResource(fileName: String): String

    /**
     * Get a [File] with path [folder]/[fileName], relative to the application's base folder.
     */
    suspend fun getFile(folder: String, fileName: String): File

    /**
     * Remove a [File] on path [folder]/[fileName], relative to the application's base folder.
     */
    suspend fun removeFile(folder: String, fileName: String)

    /**
     * Remove all files in [folder] based in application's folder.
     */
    suspend fun removeFolder(folder: String)
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

    override suspend fun createFileFromInputStream(inputStream: InputStream, folder: String, fileName: String) {
        return withContext(coroutineContext) {
            val destFile = getFile(folder, fileName)
            if (destFile.exists().not()) {
                inputStream.saveTo(destFile)
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
                throw IOException("Could not move temporary file to final destination ${file.canonicalPath}")
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

    override suspend fun getFile(folder: String, fileName: String): File {
        return withContext(coroutineContext) {
            File(getFolder(folder).mkdirsIfNeeded(), fileName)
        }
    }

    private fun getFolder(folder: String): File {
        return File(applicationInternalBaseFolder, folder)
    }

    override suspend fun removeFile(folder: String, fileName: String) {
        withContext(coroutineContext) {
            getFile(folder, fileName).apply {
                if (exists()) {
                    delete()
                }
            }
        }
    }

    override suspend fun removeFolder(folder: String) {
        withContext(coroutineContext) {
            getFolder(folder).apply {
                if (exists()) {
                    delete()
                }
            }
        }
    }
}

private fun File.mkdirsIfNeeded(): File {
    if (!exists()) {
        mkdirs()
    }
    return this
}