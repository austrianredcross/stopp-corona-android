package at.roteskreuz.stopcorona.model.repositories

import android.bluetooth.BluetoothAdapter
import android.os.Build
import at.roteskreuz.stopcorona.model.managers.BluetoothManager
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import io.reactivex.Observable

/**
 * Wrapper for Bluetooth related functionality.
 */
interface BluetoothRepository {

    /**
     * Return true if bluetooth adapter exists and is supported by exposure notification framework.
     */
    val bluetoothSupported: Boolean

    /**
     * Return true if bluetooth is enabled, otherwise false.
     */
    val bluetoothEnabled: Boolean

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
    private val bluetoothAdapter: BluetoothAdapter?
) : BluetoothRepository {

    private val enabledStateSubject = NonNullableBehaviorSubject(
        bluetoothAdapter?.isEnabled == true
    )
    override val bluetoothSupported: Boolean
        get() = bluetoothAdapter != null && if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bluetoothAdapter.isLe2MPhySupported
        } else {
            true
        }

    override val bluetoothEnabled: Boolean
        get() = (bluetoothSupported && bluetoothAdapter?.isEnabled == true).also {
            enabledStateSubject.onNext(it)
        }

    override fun observeBluetoothEnabledState(): Observable<Boolean> {
        return enabledStateSubject
    }

    override fun updateEnabledState(enabled: Boolean) {
        enabledStateSubject.onNext(enabled)
    }
}