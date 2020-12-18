package at.roteskreuz.stopcorona.screens.infection_info

import at.roteskreuz.stopcorona.model.repositories.CombinedWarningType
import at.roteskreuz.stopcorona.model.repositories.QuarantineRepository
import at.roteskreuz.stopcorona.model.repositories.QuarantineStatus
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import com.github.dmstocking.optional.java.util.Optional
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate

/**
 * Handles the user interaction and provides data for [InfectionInfoFragment].
 */
class InfectionInfoViewModel(
    appDispatchers: AppDispatchers,
    private val quarantineRepository: QuarantineRepository
) : ScopedViewModel(appDispatchers) {

    fun observeInfectedContacts(): Observable<InfectedContactsViewState> {
        return Observables.combineLatest(
            quarantineRepository.observeCombinedWarningType(),
            quarantineRepository.observeQuarantineState(),
            quarantineRepository.observeLastRedContactDate()
        ).map { (combinedWarningType, quarantineStatus, lastRedContactDate) ->
            InfectedContactsViewState(
                combinedWarningType = combinedWarningType,
                quarantinedUntil = if (quarantineStatus is QuarantineStatus.Jailed.Limited) quarantineStatus.end.toLocalDate()
                else null,
                lastRedContactDate = lastRedContactDate
            )
        }
    }
}

/**
 * Describes our state [combinedWarningType] based on the risk data received for our contacts.
 * The date until which we are in quarantine.
 * And the last infection contact date.
 */
data class InfectedContactsViewState(
    val combinedWarningType: CombinedWarningType,
    val quarantinedUntil: LocalDate? = null,
    val lastRedContactDate: Optional<Instant>? = null
)