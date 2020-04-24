package at.roteskreuz.stopcorona.screens.reporting.reportStatus.guideline

import at.roteskreuz.stopcorona.model.repositories.QuarantineRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import com.github.dmstocking.optional.java.util.Optional
import io.reactivex.Observable
import org.threeten.bp.ZonedDateTime

/**
 * Handles the user interaction and provides data for [CertificateReportGuidelinesFragment].
 */
class CertificateReportGuidelinesViewModel(
    appDispatchers: AppDispatchers,
    private val quarantineRepository: QuarantineRepository
) : ScopedViewModel(appDispatchers) {

    fun observeDateOfFirstMedicalConfirmation(): Observable<Optional<ZonedDateTime>> {
        return quarantineRepository.observeDateOfFirstMedicalConfirmation()
    }
}
