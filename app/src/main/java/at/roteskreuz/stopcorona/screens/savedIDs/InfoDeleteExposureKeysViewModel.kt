package at.roteskreuz.stopcorona.screens.savedIDs

import android.content.Intent
import at.roteskreuz.stopcorona.model.repositories.ExposureNotificationRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel

class InfoDeleteExposureKeysViewModel(
    appDispatchers: AppDispatchers,
    private val exposureNotificationRepository: ExposureNotificationRepository
) : ScopedViewModel(appDispatchers) {

    fun getExposureNotificationsSettingsIntent(): Intent {
        return exposureNotificationRepository.getExposureSettingsIntent()
    }

}