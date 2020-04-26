package at.roteskreuz.stopcorona.screens.infection_info

import at.roteskreuz.stopcorona.model.entities.infection.message.DbInfectionMessage
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.repositories.InfectionMessengerRepository
import at.roteskreuz.stopcorona.model.repositories.QuarantineRepository
import at.roteskreuz.stopcorona.model.repositories.QuarantineStatus
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import org.threeten.bp.LocalDate

/**
 * Handles the user interaction and provides data for [InfectionInfoFragment].
 */
class InfectionInfoViewModel(
    appDispatchers: AppDispatchers,
    private val infectionMessengerRepository: InfectionMessengerRepository,
    private val quarantineRepository: QuarantineRepository
) : ScopedViewModel(appDispatchers) {

    fun observeInfectedContacts(): Observable<InfectedContactsViewState> {
        return Observables.combineLatest(
            infectionMessengerRepository.observeReceivedInfectionMessages(),
            quarantineRepository.observeQuarantineState()
        ).map { (messages, quarantineStatus) ->
            InfectedContactsViewState(
                messages = messages,
                quarantinedUntil = if (quarantineStatus is QuarantineStatus.Jailed.Limited) quarantineStatus.end.toLocalDate()
                else null
            )
        }
    }
}

data class InfectedContactsViewState(
    val messages: List<DbInfectionMessage>,
    val quarantinedUntil: LocalDate? = null
) {

    val yellowMessages by lazy { messages.filter { it.messageType == MessageType.InfectionLevel.Yellow } }
    val redMessages by lazy { messages.filter { it.messageType == MessageType.InfectionLevel.Red } }
}