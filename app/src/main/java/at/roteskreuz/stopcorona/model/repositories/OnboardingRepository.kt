package at.roteskreuz.stopcorona.model.repositories

import android.content.SharedPreferences
import at.roteskreuz.stopcorona.constants.Constants.Prefs.ONBOARDING_REPOSITORY_PREFIX
import at.roteskreuz.stopcorona.skeleton.core.utils.booleanSharedPreferencesProperty

/**
 * Repository for managing the state of the onboarding.
 */
interface OnboardingRepository {

    /**
     * Indicates if the onboarding should be shown.
     */
    val shouldShowOnboarding: Boolean

    /**
     * Onboarding was seen and we don't want to show it anymore.
     */
    fun onboardingFinished()
}

class OnboardingRepositoryImpl(
    preferences: SharedPreferences
) : OnboardingRepository {

    companion object {
        private const val PREF_SHOW_ONBOARDING = ONBOARDING_REPOSITORY_PREFIX + "show_onboarding"
        private const val PREF_SHOW_ONBOARDING_V1_1 = ONBOARDING_REPOSITORY_PREFIX + "show_onboarding_v1.1"
    }

    private var showOnboarding: Boolean by preferences.booleanSharedPreferencesProperty(PREF_SHOW_ONBOARDING, true)
    private var showOnboardingV1_1: Boolean by preferences.booleanSharedPreferencesProperty(PREF_SHOW_ONBOARDING_V1_1, true)

    override val shouldShowOnboarding: Boolean
        get() = showOnboarding || showOnboardingV1_1

    override fun onboardingFinished() {
        showOnboarding = false
        showOnboardingV1_1 = false
    }
}
