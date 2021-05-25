package at.roteskreuz.stopcorona.screens.questionnaire.selfmonitoring

import at.roteskreuz.stopcorona.model.repositories.QuarantineRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import com.github.dmstocking.optional.java.util.Optional
import io.reactivex.Observable
import org.threeten.bp.ZonedDateTime
import java.util.*

class QuestionnaireSelfMonitoringViewModel(
    appDispatchers: AppDispatchers,
    private val quarantineRepository: QuarantineRepository
) : ScopedViewModel(appDispatchers) {

    fun reportSelfMonitoring() {
        quarantineRepository.reportSelfMonitoring()
    }

    fun observeDateOfLastSelfMonitoringInstruction(): Observable<Optional<ZonedDateTime>> {
        return quarantineRepository.observeDateOfLastSelfMonitoringInstruction()
    }
}
