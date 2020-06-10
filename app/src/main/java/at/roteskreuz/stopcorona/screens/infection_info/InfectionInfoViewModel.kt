package at.roteskreuz.stopcorona.screens.infection_info

import at.roteskreuz.stopcorona.model.entities.infection.message.DbReceivedInfectionMessage
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.repositories.QuarantineRepository
import at.roteskreuz.stopcorona.model.repositories.QuarantineStatus
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import org.threeten.bp.LocalDate

/**
 * Handles the user interaction and provides data for [InfectionInfoFragment].
 */
class InfectionInfoViewModel(
    appDispatchers: AppDispatchers
) : ScopedViewModel(appDispatchers) {

    private val tempHealthStatusDataSubject = NonNullableBehaviorSubject(InfectedContactsViewState(emptyList(), LocalDate.now()))
    fun observeInfectedContacts(): Observable<InfectedContactsViewState> {
        //TODO: bring back the on the Exposure Notification Framework
        return tempHealthStatusDataSubject
//        return Observables.combineLatest(
//            infectionMessengerRepository.observeReceivedInfectionMessages(),
//            quarantineRepository.observeQuarantineState()
//        ).map { (messages, quarantineStatus) ->
//            InfectedContactsViewState(
//                messages = messages,
//                quarantinedUntil = if (quarantineStatus is QuarantineStatus.Jailed.Limited) quarantineStatus.end.toLocalDate()
//                else null
//            )
//        }
    }
}

data class InfectedContactsViewState(
    val messages: List<DbReceivedInfectionMessage>,
    val quarantinedUntil: LocalDate? = null
) {

    val yellowMessages by lazy { messages.filter { it.messageType == MessageType.InfectionLevel.Yellow } }
    val redMessages by lazy { messages.filter { it.messageType == MessageType.InfectionLevel.Red } }
}