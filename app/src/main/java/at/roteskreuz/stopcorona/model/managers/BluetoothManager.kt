package at.roteskreuz.stopcorona.model.managers

import at.roteskreuz.stopcorona.model.receivers.Registrable
import at.roteskreuz.stopcorona.model.repositories.BluetoothRepository
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

/**
 * Manager to start/stop listening for bluetooth enabled state.
 */
interface BluetoothManager {

    /**
     * Start listening for bluetooth changes.
     */
    fun startListeningForChanges()

    /**
     * Stop listening for bluetooth changes.
     */
    fun stopListeningForChanges()

    /**
     * listening subject if bluetooth manager is starting to listen to bluetooth enabled changes
     */
    val listeningSubject : Subject<Boolean>
}

class BluetoothManagerImpl(
    private val contextInteractor: ContextInteractor,
    private val bluetoothStateReceiver: Registrable
) : BluetoothManager {

    override fun startListeningForChanges() {
        bluetoothStateReceiver.register(contextInteractor.applicationContext)
        listeningSubject.onNext(true)
    }

    override fun stopListeningForChanges() {
        try {
            bluetoothStateReceiver.unregister(contextInteractor.applicationContext)
            listeningSubject.onNext(false)
        } catch (e: Exception) {
            // ignored, receiver is probably not registered yet
        }
    }

    override val listeningSubject: Subject<Boolean> = PublishSubject.create()
}