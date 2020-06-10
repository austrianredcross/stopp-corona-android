package at.roteskreuz.stopcorona.model.managers

import at.roteskreuz.stopcorona.model.receivers.Registrable
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor

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
}

class BluetoothManagerImpl(
    private val contextInteractor: ContextInteractor,
    private val bluetoothStateReceiver: Registrable
) : BluetoothManager {

    override fun startListeningForChanges() {
        bluetoothStateReceiver.register(contextInteractor.applicationContext)
    }

    override fun stopListeningForChanges() {
        try {
            bluetoothStateReceiver.unregister(contextInteractor.applicationContext)
        } catch (e: Exception) {
            // ignored, receiver is probably not registered yet
        }
    }
}