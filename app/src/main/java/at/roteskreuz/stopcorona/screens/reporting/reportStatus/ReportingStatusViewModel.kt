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
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
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

    fun setUserAgreement(agreement: Boolean) {
        reportingRepository.setUserAgreement(agreement)
    }

    fun uploadInfectionInformation() {
        exposureNotificationsErrorState.loading()
        disposables += exposureNotificationRepository.observeTemporaryExposureKeys()
            .observeOnMainThread()
            .subscribe{
                when (it){
                    is DataState.Loaded ->{
                        uploadData(it.data)
                        exposureNotificationsErrorState.idle()
                    }
                    is State.Loading -> { /**nothing to do here**/ }
                    is State.Error -> {

                        val exception = it.error
                        if (exception !is ApiException) {
                            Timber.e(exception, "Unknown error when attempting to start API")
                            return@subscribe
                        }
                        val apiException = exception
                        if (apiException.statusCode == ExposureNotificationStatusCodes.DEVELOPER_ERROR) {
                            disposables += exposureNotificationRepository.observeRegistrationState()
                                .subscribe{registrationWithFrameworkState ->
                                    when (registrationWithFrameworkState) {
                                        is State.Error -> {
                                            when (registrationWithFrameworkState.error) {
                                                is ApiException -> {
                                                    val apiException = registrationWithFrameworkState.error as ApiException
                                                    exposureNotificationsErrorState.loaded(
                                                        Pair(apiException.status, ExposureNotificationRepository.ResolutionAction.REGISTER_WITH_FRAMEWORK))
                                                }
                                            }
                                        }
                                    }
                                }
                            exposureNotificationRepository.registerAppForExposureNotifications()
                            exposureNotificationsErrorState.idle()
                            return@subscribe
                        }
                        if (apiException.statusCode == ExposureNotificationStatusCodes.RESOLUTION_REQUIRED) {
                            Timber.e(exception, "Error, RESOLUTION_REQUIRED in result")
                            exposureNotificationsErrorState.loaded(Pair(exception.status, ExposureNotificationRepository.ResolutionAction.REQUEST_EXPOSURE_KEYS))
                            exposureNotificationsErrorState.idle()
                        } else {
                            Timber.e(apiException, "No RESOLUTION_REQUIRED in result")
                        }
                    }
                }


            }
        exposureNotificationRepository.getTemporaryExposureKeys()
    }

    fun uploadData(temporaryTracingKeys: List<TemporaryExposureKey>) {
        if(uploadReportDataStateObserver.currentState is State.Loading){
            Timber.e(SilentError("We are already uploading data."))
            return
        }

        uploadReportDataStateObserver.loading()
        launch {
            try {
                val reportedInfectionLevel = reportingRepository.uploadReportInformation(temporaryTracingKeys)
                uploadReportDataStateObserver.loaded(reportedInfectionLevel)
            } catch (ex: Exception) {
                uploadReportDataStateObserver.error(ex)
            } finally {
                uploadReportDataStateObserver.idle()
            }
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
}

data class ReportingStatusData(
    val agreementData: AgreementData,
    val messageType: MessageType,
    val dateOfFirstSelfDiagnose: ZonedDateTime?,
    val dateOfFirstMedicalConfirmation: ZonedDateTime?
)
