package at.roteskreuz.stopcorona.model.repositories

import android.bluetooth.BluetoothAdapter
import at.roteskreuz.stopcorona.model.managers.BluetoothManager
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import io.reactivex.Observable

/**
 * Wrapper for Bluetooth related functionality.
 */
interface BluetoothRepository {

    /**
     * @return True if bluetooth is enabled.
     */
    fun isBluetoothEnabled(): Boolean

    /**
     * Observe true if bluetooth is enabled.
     * To have an updates, [BluetoothManager] had to start listening before calling this method.
     */
    fun observeBluetoothEnabledState(): Observable<Boolean>

    /**
     * Update enabled state of the bluetooth to be observed by [observeBluetoothEnabledState].
     */
    fun updateEnabledState(enabled: Boolean)
}

class BluetoothRepositoryImpl(
    private val bluetoothAdapter: BluetoothAdapter
) : BluetoothRepository {

    private val enabledStateSubject = NonNullableBehaviorSubject(
        bluetoothAdapter.isEnabled
    )

    override fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter.isEnabled.also { enabledStateSubject.onNext(it) }
    }

    override fun observeBluetoothEnabledState(): Observable<Boolean> {
        return enabledStateSubject
    }

    override fun updateEnabledState(enabled: Boolean) {
        enabledStateSubject.onNext(enabled)
    }
}