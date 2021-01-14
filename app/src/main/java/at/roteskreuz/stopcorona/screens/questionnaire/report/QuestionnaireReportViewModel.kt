package at.roteskreuz.stopcorona.screens.questionnaire.report

import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.repositories.DateOfInfectionData
import at.roteskreuz.stopcorona.model.repositories.ReportingRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import io.reactivex.Observable
import org.threeten.bp.ZonedDateTime


/**
 * Handles the user interaction.
 */
class QuestionnaireReportViewModel(
    appDispatchers: AppDispatchers,
    private val reportingRepository: ReportingRepository
) : ScopedViewModel(appDispatchers) {

    fun setDateOfInfection(date: ZonedDateTime) {
        reportingRepository.setDateOfInfection(date)
    }

    fun goBack(){
        reportingRepository.goBackFromReportingSuspicionScreen()
    }

    fun observeDateOfInfection(): Observable<DateOfInfectionData> {
        return reportingRepository.observeDateOfInfection()
    }
}