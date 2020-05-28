package at.roteskreuz.stopcorona.model.manager

import android.content.SharedPreferences
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.skeleton.core.utils.intSharedPreferencesProperty
import at.roteskreuz.stopcorona.skeleton.core.utils.putAndApply
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
        private const val VERSION = 3
        private const val PREF_CURRENT_VERSION = Constants.Prefs.PREFERENCES_MIGRATION_MANAGER_PREFIX + "current_version"
    }

    private var currentVersion: Int by preferences.intSharedPreferencesProperty(PREF_CURRENT_VERSION, 0)

    init {
        val migrations = listOf(
            /**
             * Remove `pref_infection_messenger_repository_client_uuid` key.
             */
            migration(0, 1) {
                removeAndApply("pref_infection_messenger_repository_client_uuid")
            },
            /**
             * Remove `pref_questionnaire_compliance_repository_compliance_accepted_timestamp` key.
             */
            migration(1, 2) {
                removeAndApply("pref_questionnaire_compliance_repository_compliance_accepted_timestamp")
            },
            /**
             * Remove keys:
             * - `pref_dashboard_microphone_explanation_dialog_show_again`
             * - `pref_corona_detection_repository_is_service_running`
             * Rename key:
             * - from: `pref_corona_detection_repository_service_enabled_on_first_start`
             * - to: `pref_corona_detection_repository_service_enabled_on_first_start`
             */
            migration(2, 3) {
                removeAndApply("pref_dashboard_microphone_explanation_dialog_show_again")
                removeAndApply("pref_corona_detection_repository_is_service_running")

                val serviceEnabled = getBoolean("pref_corona_detection_repository_service_enabled_on_first_start", false)
                removeAndApply("pref_corona_detection_repository_service_enabled_on_first_start")
                putAndApply("pref_dashboard_service_enabled_on_first_start", serviceEnabled)
            }
        )

        startMigration(migrations)
    }

    private fun startMigration(_migrations: List<PreferencesMigration>) {
        val migrations = _migrations.toMutableList()
        var processingVersion = currentVersion

        while (migrations.isNotEmpty()) {
            migrations.firstOrNull { it.startVersion == processingVersion }?.let { migration ->
                migration.migrateProcedure(preferences)
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

private data class PreferencesMigration(
    val startVersion: Int,
    val endVersion: Int,
    val migrateProcedure: SharedPreferences.() -> Unit
)

/**
 * Helper fun to create migration instance.
 */
private fun migration(
    startVersion: Int,
    endVersion: Int,
    migrateProcedure: SharedPreferences.() -> Unit
): PreferencesMigration {
    return PreferencesMigration(startVersion, endVersion, migrateProcedure)
}

data class MissingMigrationException(val fromVersion: Int, val toVersion: Int) :
    Throwable("Missing migration from version $fromVersion to version $toVersion")
