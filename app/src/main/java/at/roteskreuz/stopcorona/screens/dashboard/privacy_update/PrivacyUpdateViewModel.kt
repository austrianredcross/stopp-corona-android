package at.roteskreuz.stopcorona.screens.dashboard.privacy_update

import at.roteskreuz.stopcorona.model.repositories.DataPrivacyRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel

/**
 * Handles the user interaction.
 */
class PrivacyUpdateViewModel (
    appDispatchers: AppDispatchers,
    private val dataPrivacyRepository: DataPrivacyRepository
) : ScopedViewModel(appDispatchers) {

    fun markNewPrivacyAsAccepted(){
        dataPrivacyRepository.setNewDataPrivacyAccepted()
    }

}
