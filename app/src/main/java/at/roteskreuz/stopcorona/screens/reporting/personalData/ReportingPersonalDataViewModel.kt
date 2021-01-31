package at.roteskreuz.stopcorona.screens.reporting.personalData

import androidx.annotation.StringRes
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.Constants.Misc.EMPTY_STRING
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.repositories.PersonalData
import at.roteskreuz.stopcorona.model.repositories.ReportingRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.StateObserver
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.utils.view.safeMap
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.launch
import org.threeten.bp.ZonedDateTime

/**
 * Handles the user interaction and provides data for [ReportingPersonalDataFragment].
 */
class ReportingPersonalDataViewModel(
    appDispatchers: AppDispatchers,
    private val reportingRepository: ReportingRepository
) : ScopedViewModel(appDispatchers) {

    private val validationResultSubject = BehaviorSubject.create<ValidationResult>()
    private val tanRequestStateObserver = StateObserver()

    private var mobileNumber: String? = null

    fun setMobileNumber(mobileNumber: String?) {
        this.mobileNumber = mobileNumber
    }

    val dateOfInfection: ZonedDateTime?
        get() = reportingRepository.dateOfInfection

    fun validate() {
        val mobileNumber = this.mobileNumber

        val validationResult = ValidationResult(
            validatePhone(mobileNumber)
        )

        if (validationResult.isValid()) {
            mobileNumber?.let { mobile ->
                requestTan(mobile)
            }
        }
        validationResultSubject.onNext(validationResult)
    }

    private fun requestTan(mobileNumber: String) {
        tanRequestStateObserver.loading()
        launch {
            try {
                reportingRepository.requestTan(mobileNumber)
                // Request is successful, save the personal data and mark the tan request as successful.
                reportingRepository.setPersonalDataAndTanRequestSuccess(
                    mobileNumber.safeMap("Mobile validation is not correct.", EMPTY_STRING)
                )
            } catch (ex: Exception) {
                tanRequestStateObserver.error(ex)
            } finally {
                tanRequestStateObserver.idle()
            }
        }
    }

    private fun validatePhone(mobileNumber: String?): ValidationError? {
        val validationErrorEmpty = validateNotEmpty(mobileNumber)
        if (validationErrorEmpty != null) {
            return validationErrorEmpty
        }
        val nonNullMobileNumber = mobileNumber.safeMap("Empty validation is not correct.", EMPTY_STRING)
        val validationErrorInvalidPhone = validatePhoneNumber(nonNullMobileNumber)
        if (validationErrorInvalidPhone != null) {
            return validationErrorInvalidPhone
        }
        return null
    }

    fun observeTanRequestState(): Observable<State> {
        return tanRequestStateObserver.observe()
    }

    fun observeValidationResult(): Observable<ValidationResult> {
        return validationResultSubject
    }

    fun observePersonalData(): Observable<PersonalData> {
        return reportingRepository.observePersonalData()
    }

    fun observeMessageType(): Observable<MessageType> {
        return reportingRepository.observeMessageType()
    }
}

fun validateNotEmpty(text: String?): ValidationError? {
    return if (text?.isNotEmpty() == true) {
        null
    } else {
        return ValidationError.FieldEmpty
    }
}

fun validatePhoneNumber(text: String): ValidationError? {
    val pattern = "^\\+[0-9]+".toRegex()

    return if (pattern.matches(text).not()) {
        ValidationError.InvalidPhoneNumber
    } else {
        null
    }
}

/**
 * Describes the result of the personal data form validation.
 */
data class ValidationResult(
    val mobileNumber: ValidationError?
) {

    fun isValid(): Boolean {
        return mobileNumber == null
    }
}

/**
 * Describes the errors that can appear in the form.
 */
sealed class ValidationError(@StringRes val error: Int) {

    /**
     * A mandatory field is empty.
     */
    object FieldEmpty : ValidationError(R.string.certificate_personal_data_field_mandatory)

    /**
     * Invalid phone number.
     */
    object InvalidPhoneNumber : ValidationError(R.string.certificate_personal_data_phone_field_invalid)
}
