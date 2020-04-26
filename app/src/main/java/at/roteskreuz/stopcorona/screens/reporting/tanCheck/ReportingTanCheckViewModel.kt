package at.roteskreuz.stopcorona.screens.reporting.tanCheck

import at.roteskreuz.stopcorona.constants.Constants.Misc.EMPTY_STRING
import at.roteskreuz.stopcorona.model.repositories.PersonalData
import at.roteskreuz.stopcorona.model.repositories.ReportingRepository
import at.roteskreuz.stopcorona.model.repositories.TanData
import at.roteskreuz.stopcorona.screens.reporting.personalData.ValidationError
import at.roteskreuz.stopcorona.screens.reporting.personalData.validateNotEmpty
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.StateObserver
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.utils.view.safeMap
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.launch

/**
 * Handles the user interaction and provides data for [ReportingTanCheckFragment].
 */
class ReportingTanCheckViewModel(
    appDispatchers: AppDispatchers,
    private val reportingRepository: ReportingRepository
) : ScopedViewModel(appDispatchers) {

    private val validationResultSubject = BehaviorSubject.create<ValidationResult>()
    private val tanRequestStateObserver = StateObserver()

    private var tan: String? = null

    fun setTan(tan: String?) {
        this.tan = tan
    }

    fun requestTan() {
        tanRequestStateObserver.loading()
        launch {
            try {
                val mobileNumber = reportingRepository.observePersonalData().blockingFirst().mobileNumber
                reportingRepository.requestTan(mobileNumber)
            } catch (ex: Exception) {
                tanRequestStateObserver.error(ex)
            } finally {
                tanRequestStateObserver.idle()
            }
        }
    }

    fun validate() {
        val tan = this.tan
        val validationResult = ValidationResult(validateNotEmpty(tan))
        if (validationResult.isValid()) {
            reportingRepository.setTan(tan.safeMap("Tan validation is not correct.", EMPTY_STRING))
        }
        validationResultSubject.onNext(validationResult)
    }

    fun observeTanData(): Observable<TanData> {
        return reportingRepository.observeTanData()
    }

    fun observePersonalData(): Observable<PersonalData> {
        return reportingRepository.observePersonalData()
    }

    fun observeValidationResult(): Observable<ValidationResult> {
        return validationResultSubject
    }

    fun observeTanRequestState(): Observable<State> {
        return tanRequestStateObserver.observe()
    }

    fun goBack() {
        reportingRepository.goBackFromTanEntryScreen()
    }
}

/**
 * Describes the result of the TAN field.
 */
data class ValidationResult(
    val tan: ValidationError?
) {

    fun isValid(): Boolean {
        return tan == null
    }
}

