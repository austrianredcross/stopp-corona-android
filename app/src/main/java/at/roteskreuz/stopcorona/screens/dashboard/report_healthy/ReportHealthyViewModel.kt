package at.roteskreuz.stopcorona.screens.dashboard.report_healthy

import at.roteskreuz.stopcorona.model.repositories.QuarantineRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel

/**
 * Handles the user interaction.
 */
class ReportHealthyViewModel(
    appDispatchers: AppDispatchers,
    val quarantineRepository: QuarantineRepository
) : ScopedViewModel(appDispatchers) {

    fun revokeMedicalConfirmation(){
        quarantineRepository.revokeMedicalConfirmation()
    }

}