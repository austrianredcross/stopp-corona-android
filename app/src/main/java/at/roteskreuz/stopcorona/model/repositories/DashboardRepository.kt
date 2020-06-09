package at.roteskreuz.stopcorona.model.repositories

import android.content.SharedPreferences
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.skeleton.core.utils.booleanSharedPreferencesProperty
import at.roteskreuz.stopcorona.skeleton.core.utils.observeBoolean
import io.reactivex.Observable

/**
 * Repository for managing dashboard content.
 */
interface DashboardRepository {

    /**
     * Indicate a user intent to register.
     * True still doesn't mean, that the framework is successfully registered,
     * use [ExposureNotificationRepository.isAppRegisteredForExposureNotificationsLastState] instead.
     */
    var userWantsToRegisterAppForExposureNotifications: Boolean

    /**
     * Information if the automatic handshake was enabled automatically on the first start
     */
    var exposureFrameworkEnabledOnFirstStart: Boolean

    /**
     * Indicate a user intent to register.
     * True still doesn't mean, that the framework is successfully registered,
     * use [ExposureNotificationRepository.isAppRegisteredForExposureNotificationsLastState] instead.
     */
    fun observeUserWantsToRegisterAppForExposureNotification(): Observable<Boolean>
}

class DashboardRepositoryImpl(
    private val preferences: SharedPreferences
) : DashboardRepository {

    companion object {
        private const val PREF_EXPOSURE_FRAMEWORK_ENABLED_ON_FIRST_START =
            Constants.Prefs.DASHBOARD_PREFIX + "exposure_framework_enabled_on_first_start"
        private const val PREF_WANTED_STATE_OF_APP_EXPOSURE_NOTIFICATION_REGISTRATION =
            Constants.Prefs.DASHBOARD_PREFIX + "wanted_state_of_app_exposure_notification_registration"
    }

    override var userWantsToRegisterAppForExposureNotifications: Boolean
        by preferences.booleanSharedPreferencesProperty(PREF_WANTED_STATE_OF_APP_EXPOSURE_NOTIFICATION_REGISTRATION, false)

    override var exposureFrameworkEnabledOnFirstStart: Boolean
        by preferences.booleanSharedPreferencesProperty(PREF_EXPOSURE_FRAMEWORK_ENABLED_ON_FIRST_START, false)

    override fun observeUserWantsToRegisterAppForExposureNotification(): Observable<Boolean> {
        return preferences.observeBoolean(PREF_WANTED_STATE_OF_APP_EXPOSURE_NOTIFICATION_REGISTRATION, false)
    }
}