package at.roteskreuz.stopcorona.model.repositories

import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent

/**
 * Wrapper for Bluetooth related functionality.
 */
interface BluetoothRepository {

    /**
     * @return True if bluetooth is enabled.
     */
    fun isBluetoothEnabled(): Boolean

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
    private val bluetoothAdapter: BluetoothAdapter
) : BluetoothRepository {

    override fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter.isEnabled
    }

    override fun getEnableBluetoothIntent(): Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

    override fun getEnableBluetoothPendingIntent(context: Context): PendingIntent {
        val enableBtIntent = getEnableBluetoothIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, 0, enableBtIntent, PendingIntent.FLAG_ONE_SHOT)
    }
}