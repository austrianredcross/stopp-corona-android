package at.roteskreuz.stopcorona.screens.routing

import android.net.Uri
import at.roteskreuz.stopcorona.model.repositories.InfectionMessengerRepository
import at.roteskreuz.stopcorona.model.repositories.OnboardingRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel

/**
 * Handles router actions.
 *
 * Routing strategies:
 * 1) deeplinking
 *      - at a later point of time
 * 2) onboarding on if has not been seen by user yet
 * 3) else dashboard.
 */
class RouterViewModel(
    appDispatchers: AppDispatchers,
    private val onboardingRepository: OnboardingRepository,
    private val infectionMessengerRepository: InfectionMessengerRepository
) : ScopedViewModel(appDispatchers) {

    fun enqueuePeriodExposureMatching() {
        infectionMessengerRepository.enqueuePeriodExposureMatching()
    }

    fun route(deepLinkUri: Uri?): RouterAction {
        return when {
            onboardingRepository.shouldShowOnboarding -> RouterAction.Onboarding
            else -> RouterAction.Dashboard
        }
    }
}

sealed class RouterAction {

    object Onboarding : RouterAction()
    object Dashboard : RouterAction()
}
