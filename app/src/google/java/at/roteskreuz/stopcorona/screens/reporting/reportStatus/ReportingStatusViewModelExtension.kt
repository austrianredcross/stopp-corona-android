package at.roteskreuz.stopcorona.screens.reporting.reportStatus

import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataStateObserver
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes

fun ReportingStatusViewModel.handleFrameSpecificErrorOnUploadData(exception: Exception,
                                                                  uploadReportDataStateObserver: DataStateObserver<MessageType>,
                                                                  exposureNotificationsErrorState: DataStateObserver<ResolutionType>) : Boolean {
    if(exception is ApiException) {
        when (exception.statusCode) {
            ExposureNotificationStatusCodes.RESOLUTION_REQUIRED -> {
                exposureNotificationsErrorState.loaded(ResolutionType.GetExposureKeys(exception.status))
            }
            else -> {
                uploadReportDataStateObserver.error(exception)
            }
        }
        return true
    }

    return false
}