package at.roteskreuz.stopcorona.model.repositories

import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import io.reactivex.Observable

/**
 * Wrapper for Bluetooth related functionality.
 */
interface BluetoothRepository {

    /**
     * Observe true if bluetooth is enabled.
     * To have an updates,
     */
    fun observeBluetoothEnabledState(): Observable<Boolean>

    /**
     * Update enabled state of the bluetooth to be observed by [observeBluetoothEnabledState].
     */
    fun updateEnabledState(enabled: Boolean)

    /**
     * Get an intent to display dialog to enable bluetooth.
     */
    fun getEnableBluetoothIntent(): Intent

    /**
     * Get a pending intent to display dialog to enable bluetooth.
     * Flag for new task.
     */
    fun getEnableBluetoothPendingIntent(context: Context): PendingIntent
}

class BluetoothRepositoryImpl(
    bluetoothAdapter: BluetoothAdapter
) : BluetoothRepository {

    private val enabledStateSubject = NonNullableBehaviorSubject(
        bluetoothAdapter.isEnabled
    )

    override fun observeBluetoothEnabledState(): Observable<Boolean> {
        return enabledStateSubject
    }

    override fun updateEnabledState(enabled: Boolean) {
        enabledStateSubject.onNext(enabled)
    }

    override fun getEnableBluetoothIntent(): Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

    override fun getEnableBluetoothPendingIntent(context: Context): PendingIntent {
        val enableBtIntent = getEnableBluetoothIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, 0, enableBtIntent, PendingIntent.FLAG_ONE_SHOT)
    }
}