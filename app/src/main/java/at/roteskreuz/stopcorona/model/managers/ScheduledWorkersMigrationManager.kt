package at.roteskreuz.stopcorona.model.managers

import android.content.SharedPreferences
import androidx.work.WorkManager
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.skeleton.core.utils.intSharedPreferencesProperty

/**
 * Manages migrations of the already scheduled background workers.
 */
interface ScheduledWorkersMigrationManager {

    /**
     * The current version of the scheduled workers manager.
     */
    val currentScheduledWorkersManagerVersion: Int
}

class ScheduledWorkersMigrationManagerImpl(
    preferences: SharedPreferences,
    private val workManager: WorkManager
) : ScheduledWorkersMigrationManager {

    companion object {

        private const val VERSION = 1
        private const val PREF_CURRENT_VERSION = Constants.Prefs.SCHEDULED_WORKERS_MIGRATION_MANAGER_PREFIX + "current_version"
    }

    private var currentVersion: Int by preferences.intSharedPreferencesProperty(PREF_CURRENT_VERSION, 0)

    init {
        val migrations = listOf(
            /**
             * Cancel old implementation of unique work. New periodic implementation will be scheduled automatically.
             */
            migration(0, 1) {
                cancelUniqueWork("ExposureMatchingWorker")
            }
        )

        startMigration(migrations)
    }

    private fun startMigration(_migrations: List<ScheduledWorkersMigration>) {
        val migrations = _migrations.toMutableList()
        var processingVersion = currentVersion

        while (migrations.isNotEmpty()) {
            migrations.firstOrNull { it.startVersion == processingVersion }?.let { migration ->
                migration.migrateProcedure(workManager)
                migrations.remove(migration)
                processingVersion = migration.endVersion
            } ?: migrations.clear()
        }

        if (processingVersion != VERSION) {
            throw MissingMigrationException(processingVersion, VERSION)
        }

        currentVersion = VERSION
    }

    override val currentScheduledWorkersManagerVersion: Int
        get() = currentVersion
}

private data class ScheduledWorkersMigration(
    val startVersion: Int,
    val endVersion: Int,
    val migrateProcedure: WorkManager.() -> Unit
)

/**
 * Helper fun to create migration instance.
 */
private fun migration(
    startVersion: Int,
    endVersion: Int,
    migrateProcedure: WorkManager.() -> Unit
): ScheduledWorkersMigration {
    return ScheduledWorkersMigration(startVersion, endVersion, migrateProcedure)
}