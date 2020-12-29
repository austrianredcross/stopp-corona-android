package at.roteskreuz.stopcorona.screens.reporting.reportStatus.guideline

import at.roteskreuz.stopcorona.model.repositories.QuarantineRepository
import at.roteskreuz.stopcorona.model.repositories.QuarantineStatus
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import io.reactivex.Observable

/**
 * Handles the user interaction and provides data for [CertificateReportGuidelinesFragment].
 */
class CertificateReportGuidelinesViewModel(
    appDispatchers: AppDispatchers,
    private val quarantineRepository: QuarantineRepository
) : ScopedViewModel(appDispatchers) {

    fun observeQuarantineStatus(): Observable<QuarantineStatus> {
        return quarantineRepository.observeQuarantineState()
    }
}
