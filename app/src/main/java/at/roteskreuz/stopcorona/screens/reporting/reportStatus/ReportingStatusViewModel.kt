package at.roteskreuz.stopcorona.screens.reporting.reportStatus

import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.repositories.AgreementData
import at.roteskreuz.stopcorona.model.repositories.ExposureNotificationRepository
import at.roteskreuz.stopcorona.model.repositories.QuarantineRepository
import at.roteskreuz.stopcorona.model.repositories.ReportingRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataState
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataStateObserver
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import kotlinx.coroutines.launch
import org.threeten.bp.ZonedDateTime
import timber.log.Timber

/**
 * Handles the user interaction and provides data for [ReportingStatusFragment].
 */
class ReportingStatusViewModel(
    appDispatchers: AppDispatchers,
    private val reportingRepository: ReportingRepository,
    private val quarantineRepository: QuarantineRepository,
    private val exposureNotificationRepository: ExposureNotificationRepository
) : ScopedViewModel(appDispatchers) {
    private val uploadReportDataStateObserver = DataStateObserver<MessageType>()
    private val exposureNotificationsErrorState = DataStateObserver<Pair<Status, ExposureNotificationRepository.ResolutionAction>>()

    private suspend fun uploadData(temporaryTracingKeys: List<TemporaryExposureKey>) {
        try {
            val reportedInfectionLevel = reportingRepository.uploadReportInformation(temporaryTracingKeys)
            uploadReportDataStateObserver.loaded(reportedInfectionLevel)
        } catch (ex: Exception) {
            uploadReportDataStateObserver.error(ex)
        } finally {
            uploadReportDataStateObserver.idle()
        }
    }

    fun setUserAgreement(agreement: Boolean) {
        reportingRepository.setUserAgreement(agreement)
    }

    fun uploadData() {
        if (uploadReportDataStateObserver.currentState is State.Loading) {
            Timber.e(SilentError("We are already uploading data."))
            return
        }
        uploadReportDataStateObserver.loading()
        launch {
            try {
                if (checkIfAppIsRegistered().not()) {
                    uploadReportDataStateObserver.idle()
                    return@launch
                }
                val temporaryTracingKeys = exposureNotificationRepository.getTemporaryExposureKeys()
                uploadData(temporaryTracingKeys)
            } catch (apiException: ApiException) {
                when (apiException.statusCode) {
                    ExposureNotificationStatusCodes.RESOLUTION_REQUIRED -> {
                        Timber.e(apiException, "Expected Error, RESOLUTION_REQUIRED in result - attempt to handle it")
                        exposureNotificationsErrorState.loaded(
                            Pair(apiException.status, ExposureNotificationRepository.ResolutionAction.REQUEST_EXPOSURE_KEYS))
                    }
                    else -> {
                        uploadReportDataStateObserver.error(apiException)
                        return@launch
                    }
                }
                uploadReportDataStateObserver.idle()
            } catch (exception: java.lang.Exception) {
                Timber.e(exception, "Unknown error when attempting to start API")
                uploadReportDataStateObserver.error(exception)
            }
        }
    }

    private suspend fun checkIfAppIsRegistered(): Boolean {
        val appIsRegistered = exposureNotificationRepository.isAppRegisteredForExposureNotificationsCurrentState()
        if (appIsRegistered) {
            return true
        } else {
            try {
                exposureNotificationRepository.registerAppForExposureNotificationsNow()
            } catch (apiException: ApiException) {
                when (apiException.statusCode) {
                    ExposureNotificationStatusCodes.RESOLUTION_REQUIRED -> {
                        Timber.e(apiException, "Expected Error, RESOLUTION_REQUIRED in result when registering - attempt to handle it")
                        exposureNotificationsErrorState.loaded(
                            Pair(apiException.status, ExposureNotificationRepository.ResolutionAction.REGISTER_WITH_FRAMEWORK))
                    }
                    else -> {
                        uploadReportDataStateObserver.error(apiException)
                    }
                }
            }
            return false
        }
    }

    fun goBack() {
        reportingRepository.goBackFromReportingAgreementScreen()
    }

    fun observeUploadReportDataState(): Observable<DataState<MessageType>> {
        return uploadReportDataStateObserver.observe()
    }

    fun observeReportingStatusData(): Observable<ReportingStatusData> {
        return Observables.combineLatest(
            reportingRepository.observeAgreementData(),
            reportingRepository.observeMessageType(),
            quarantineRepository.observeDateOfFirstSelfDiagnose(),
            quarantineRepository.observeDateOfFirstMedicalConfirmation()
        ) { agreementData, infectionLevel, dateOfFirstSelfDiagnose, dateOfFirstMedicalConfirmation ->
            ReportingStatusData(
                agreementData,
                infectionLevel,
                dateOfFirstSelfDiagnose.orElse(null),
                dateOfFirstMedicalConfirmation.orElse(null)
            )
        }
    }

    fun observeMessageType(): Observable<MessageType> {
        return reportingRepository.observeMessageType()
    }

    fun observeResolutionError(): Observable<DataState<Pair<Status, ExposureNotificationRepository.ResolutionAction>>> {
        return exposureNotificationsErrorState.observe()
    }

    fun resolutionForRegistrationSucceeded() {
        uploadReportDataStateObserver.loading()
        launch {
            //we need to do this as the framework is slow and does not know about the resolution yet
            Thread.sleep(2000)
            uploadReportDataStateObserver.idle()
            uploadData()
        }
    }

    fun resolutionForRegistrationFailed() {
        Timber.e(SilentError("User declined app registration with Exposure Notification Framework"))
        uploadReportDataStateObserver.idle()
    }

    fun resolutionForExposureKeyHistorySucceeded() {
        uploadData()
    }

    fun resolutionForExposureKeyHistoryFailed() {
        Timber.e(SilentError("User declined app access to the TemporaryExposureKeys from the  Exposure Notification Framework"))
        uploadReportDataStateObserver.idle()
    }
}

data class ReportingStatusData(
    val agreementData: AgreementData,
    val messageType: MessageType,
    val dateOfFirstSelfDiagnose: ZonedDateTime?,
    val dateOfFirstMedicalConfirmation: ZonedDateTime?
)
