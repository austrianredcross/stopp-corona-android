package at.roteskreuz.stopcorona.screens.dashboard.dialog

import at.roteskreuz.stopcorona.model.repositories.DashboardRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel

/**
 * Handles the user interaction for [MicrophoneExplanationDialog].
 */
class MicrophoneExplanationDialogViewModel(
    appDispatchers: AppDispatchers,
    private val dashboardRepository: DashboardRepository
) : ScopedViewModel(appDispatchers) {

    fun doNotShowAgain() {
        dashboardRepository.setMicrophoneExplanationDialogShown()
    }
}
