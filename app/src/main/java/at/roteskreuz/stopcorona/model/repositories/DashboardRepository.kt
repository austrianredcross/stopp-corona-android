package at.roteskreuz.stopcorona.model.repositories

import android.content.SharedPreferences
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.skeleton.core.utils.booleanSharedPreferencesProperty
import at.roteskreuz.stopcorona.skeleton.core.utils.observeBoolean
import at.roteskreuz.stopcorona.utils.asDbObservable
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables

/**
 * Repository for managing dashboard content.
 */
interface DashboardRepository {

    val showMicrophoneExplanationDialog: Boolean

    var userWantsToRegisterAppForExposureNotifications: Boolean

    /**
     * Information if the automatic handshake was enabled automatically on the first start
     */
    var exposureFrameworkEnabledOnFirstStart: Boolean

    /**
     * Observes the number of met people.
     */
    fun observeSavedEncountersNumber(): Observable<Int>

    /**
     * Information if the automatic handshake was enabled automatically on the first start
     */
    fun setMicrophoneExplanationDialogShown()

    fun observeCombinedExposureNotificationsState(): Observable<CombinedExposureNotificationsState>

    fun refreshCombinedExposureNotificationsState()
}

class DashboardRepositoryImpl(
    private val nearbyRecordDao: NearbyRecordDao,
    private val exposureNotificationRepository: ExposureNotificationRepository,
    private val preferences: SharedPreferences
) : DashboardRepository {

    companion object {
        private const val PREF_MICROPHONE_EXPLANATION_DIALOG_SHOW_AGAIN =
            Constants.Prefs.DASHBOARD_PREFIX + "microphone_explanation_dialog_show_again"
        private const val PREF_EXPOSURE_FRAMEWORK_ENABLED_ON_FIRST_START =
            Constants.Prefs.DASHBOARD_PREFIX + "exposure_framework_enabled_on_first_start"
    }

    override var showMicrophoneExplanationDialog: Boolean
        by preferences.booleanSharedPreferencesProperty(PREF_MICROPHONE_EXPLANATION_DIALOG_SHOW_AGAIN, true)
        private set

    override var userWantsToRegisterAppForExposureNotifications: Boolean
        get(){
            return exposureNotificationRepository.userWantsToRegisterAppForExposureNotifications
        }
        set(value) {
            exposureNotificationRepository.userWantsToRegisterAppForExposureNotifications = value
            if (value.not()){
                exposureNotificationRepository.unregisterAppFromExposureNotifications()
            }
        }

    override var exposureFrameworkEnabledOnFirstStart: Boolean by preferences.booleanSharedPreferencesProperty(
        PREF_EXPOSURE_FRAMEWORK_ENABLED_ON_FIRST_START, false)

    override fun observeSavedEncountersNumber(): Observable<Int> {
        return nearbyRecordDao.observeNumberOfRecords().asDbObservable()
    }

    override fun setMicrophoneExplanationDialogShown() {
        showMicrophoneExplanationDialog = false
    }

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