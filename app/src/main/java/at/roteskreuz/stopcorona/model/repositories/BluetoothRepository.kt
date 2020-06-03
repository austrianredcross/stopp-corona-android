package at.roteskreuz.stopcorona.model.repositories

import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import androidx.core.app.ActivityCompat.startActivityForResult

interface BluetoothRepository {

    val enableBluetoothIntent: Intent

    fun isBluetoothEnabled(): Boolean

    fun enableBluetoothPendingIntent(context: Context): PendingIntent
}

/**
 * wrapper for Bluetooth related functionality
 */
class BluetoothRepositoryImpl(
    private val bluetoothAdapter: BluetoothAdapter
) : BluetoothRepository {

    override val enableBluetoothIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

    override fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter.isEnabled
    }

    override fun enableBluetoothPendingIntent(context: Context): PendingIntent {
        val enableBtIntent = enableBluetoothIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, 0, enableBtIntent, PendingIntent.FLAG_ONE_SHOT)
    }
}