package at.roteskreuz.stopcorona.model.repositories

import android.content.SharedPreferences
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.skeleton.core.utils.booleanSharedPreferencesProperty


/**
 * Repository for managing dashboard content.
 */
interface DashboardRepository {

    /**
     * Information if the automatic handshake was enabled automatically on the first start
     */
    var exposureFrameworkEnabledOnFirstStart: Boolean
}

class DashboardRepositoryImpl(
    preferences: SharedPreferences
) : DashboardRepository {

    companion object {
        private const val PREF_EXPOSURE_FRAMEWORK_ENABLED_ON_FIRST_START =
            Constants.Prefs.DASHBOARD_PREFIX + "exposure_framework_enabled_on_first_start"
    }

    override var exposureFrameworkEnabledOnFirstStart: Boolean
        by preferences.booleanSharedPreferencesProperty(PREF_EXPOSURE_FRAMEWORK_ENABLED_ON_FIRST_START, false)
}