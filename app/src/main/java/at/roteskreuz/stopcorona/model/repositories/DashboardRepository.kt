package at.roteskreuz.stopcorona.model.repositories

import android.content.SharedPreferences
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.skeleton.core.utils.booleanSharedPreferencesProperty
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables

/**
 * Repository for managing dashboard content.
 */
interface DashboardRepository {

    var userWantsToRegisterAppForExposureNotifications: Boolean

    /**
     * Information if the automatic handshake was enabled automatically on the first start
     */
    var exposureFrameworkEnabledOnFirstStart: Boolean

    fun observeCombinedExposureNotificationsState(): Observable<CombinedExposureNotificationsState>

    fun refreshCombinedExposureNotificationsState()
}

class DashboardRepositoryImpl(
    private val exposureNotificationRepository: ExposureNotificationRepository,
    private val preferences: SharedPreferences
) : DashboardRepository {

    companion object {
        private const val PREF_EXPOSURE_FRAMEWORK_ENABLED_ON_FIRST_START =
            Constants.Prefs.DASHBOARD_PREFIX + "exposure_framework_enabled_on_first_start"
    }

    override var exposureFrameworkEnabledOnFirstStart: Boolean
        by preferences.booleanSharedPreferencesProperty(PREF_EXPOSURE_FRAMEWORK_ENABLED_ON_FIRST_START, false)

    override fun observeCombinedExposureNotificationsState(): Observable<CombinedExposureNotificationsState> {
        return Observables.combineLatest(
            exposureNotificationRepository.observeUserWantsToRegisterAppForExposureNotificationsState(),
            exposureNotificationRepository.observeAppIsRegisteredForExposureNotifications()
        ).map { (wantedState, realState) ->
            CombinedExposureNotificationsState.from(wantedState, realState)
        }
    }

    override fun refreshCombinedExposureNotificationsState() {
        TODO("Not yet implemented")
        // refresh the state, do what it takes
        //not this exposureNotificationRepository.refreshExposureNotificationAppRegisteredState()
        //check errors
        // trigger register when all is fine
    }
}

sealed class CombinedExposureNotificationsState{
    object UserWantsItEnabled : CombinedExposureNotificationsState()
    object ItIsEnabledAndRunning : CombinedExposureNotificationsState()
    object Disabled : CombinedExposureNotificationsState()

    companion object {
        fun from(wantedState: Boolean, realState:Boolean): CombinedExposureNotificationsState{
            return when{
                realState -> ItIsEnabledAndRunning
                wantedState && realState.not() -> UserWantsItEnabled
                else -> Disabled
            }
        }
    }
}