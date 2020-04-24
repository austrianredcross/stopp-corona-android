package at.roteskreuz.stopcorona.screens.questionnaire.compliance

import at.roteskreuz.stopcorona.model.repositories.QuestionnaireComplianceRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject

class QuestionnaireComplianceViewModel(
    appDispatchers: AppDispatchers,
    private val questionnaireComplianceRepository: QuestionnaireComplianceRepository
) : ScopedViewModel(appDispatchers) {

    private val complianceAcceptedSubject = NonNullableBehaviorSubject(false)

    fun setComplianceAccepted(accepted: Boolean) {
        complianceAcceptedSubject.onNext(accepted)
    }

    fun observeComplianceAccepted() = complianceAcceptedSubject

    fun onComplianceAccepted() {
        questionnaireComplianceRepository.setComplianceAccepted()
    }
}
