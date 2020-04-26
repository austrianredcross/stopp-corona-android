package at.roteskreuz.stopcorona.screens.questionnaire.selfmonitoring

import at.roteskreuz.stopcorona.model.repositories.QuarantineRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel

class QuestionnaireSelfMonitoringViewModel(
    appDispatchers: AppDispatchers,
    private val quarantineRepository: QuarantineRepository
) : ScopedViewModel(appDispatchers) {

    fun reportSelfMonitoring() {
        quarantineRepository.reportSelfMonitoring()
    }
}
