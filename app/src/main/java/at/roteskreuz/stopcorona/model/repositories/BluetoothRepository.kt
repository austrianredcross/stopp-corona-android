package at.roteskreuz.stopcorona.model.repositories

import android.bluetooth.BluetoothAdapter
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
}