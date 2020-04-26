package at.roteskreuz.stopcorona.screens.reporting

import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.repositories.ReportingState
import at.roteskreuz.stopcorona.model.repositories.ReportingRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import io.reactivex.Observable

/**
 * Handles the user interaction and provides data for [ReportingActivity].
 */
class ReportingViewModel(
    appDispatchers: AppDispatchers,
    private val reportingRepository: ReportingRepository,
    messageType: MessageType
) : ScopedViewModel(appDispatchers) {

    init {
        reportingRepository.setMessageType(messageType)
    }

    fun observeReportingState(): Observable<ReportingState> {
        return reportingRepository.observeReportingState()
    }
}
