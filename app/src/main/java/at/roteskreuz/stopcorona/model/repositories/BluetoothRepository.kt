package at.roteskreuz.stopcorona.model.repositories

import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import androidx.core.app.ActivityCompat.startActivityForResult

/**
 * wrapper for Bluetooth related functionality
 */
class BluetoothRepository(
    private val bluetoothAdapter: BluetoothAdapter

){
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter.isEnabled
    }

    fun enableBluetoothIntent(): Intent {
        return Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    }

    fun enableBluetoothPendingIntent(context: Context): PendingIntent {
        val enableBtIntent = enableBluetoothIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, 0, enableBtIntent, PendingIntent.FLAG_ONE_SHOT)
    }
}