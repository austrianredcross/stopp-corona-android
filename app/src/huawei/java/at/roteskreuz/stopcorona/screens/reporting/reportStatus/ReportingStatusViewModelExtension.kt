package at.roteskreuz.stopcorona.screens.reporting.reportStatus

import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataStateObserver
import com.huawei.hms.common.ApiException
import com.huawei.hms.contactshield.StatusCode

fun ReportingStatusViewModel.handleFrameSpecificErrorOnUploadData(exception: Exception,
                                                                  uploadReportDataStateObserver: DataStateObserver<MessageType>,
                                                                  exposureNotificationsErrorState: DataStateObserver<ResolutionType>) : Boolean {
    if(exception is ApiException && exception.statusCode == StatusCode.STATUS_UNAUTHORIZED) {
        //user didn't accept share period key dialog, do nothing
        return true
    }

    return false
}