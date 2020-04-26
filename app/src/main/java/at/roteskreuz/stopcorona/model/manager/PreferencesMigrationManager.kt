package at.roteskreuz.stopcorona.model.manager

import android.content.SharedPreferences
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.skeleton.core.utils.intSharedPreferencesProperty
import at.roteskreuz.stopcorona.skeleton.core.utils.removeAndApply

/**
 * Manages migrations of the shared preferences
 */
interface PreferencesMigrationManager {

    /**
     * The current version of the sharedPreferences
     */
    val currentPreferencesVersion: Int
}

class PreferencesMigrationManagerImpl(
    private val preferences: SharedPreferences
) : PreferencesMigrationManager {

    companion object {
        private const val VERSION = 1
        private const val PREF_CURRENT_VERSION = Constants.Prefs.PREFERENCES_MIGRATION_MANAGER_PREFIX + "current_version"
    }

    private var currentVersion: Int by preferences.intSharedPreferencesProperty(PREF_CURRENT_VERSION, 0)

    init {
        val migrations = listOf(
            PreferencesMigration(0, 1) {
                preferences.removeAndApply("pref_infection_messenger_repository_client_uuid")
            }
        )

        startMigration(migrations)
    }

    private fun startMigration(_migrations: List<PreferencesMigration>) {
        val migrations = _migrations.toMutableList()
        var processingVersion = currentVersion

        while (migrations.isNotEmpty()) {
            migrations.firstOrNull { it.startVersion == processingVersion }?.let { migration ->
                migration.migration()
                migrations.remove(migration)
                processingVersion = migration.endVersion
            } ?: migrations.clear()
        }

        if (processingVersion != VERSION) {
            throw MissingMigrationException(processingVersion, VERSION)
        }

        currentVersion = VERSION
    }

    override val currentPreferencesVersion: Int
        get() = currentVersion
}

data class PreferencesMigration(
    var startVersion: Int,
    val endVersion: Int,
    val migration: () -> Unit
)

data class MissingMigrationException(val fromVersion: Int, val toVersion: Int) :
    Throwable("Missing migration from version $fromVersion to version $toVersion")
