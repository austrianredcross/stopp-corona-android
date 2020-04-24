package at.roteskreuz.stopcorona.screens.questionnaire.hint

import at.roteskreuz.stopcorona.model.repositories.QuarantineRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel

class QuestionnaireHintViewModel(
    appDispatchers: AppDispatchers,
    private val quarantineRepository: QuarantineRepository
) : ScopedViewModel(appDispatchers) {

    fun revokeSelfMonitoring() {
        quarantineRepository.revokeSelfMonitoring()
    }
}
