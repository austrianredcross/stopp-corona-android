package at.roteskreuz.stopcorona.model.receivers

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import at.roteskreuz.stopcorona.model.repositories.BluetoothRepository
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

/**
 * Update [BluetoothRepository] state with the enabled state.
 */
class BluetoothStateReceiver : BroadcastReceiver(), Registrable, KoinComponent {

    companion object {
        private const val ACTION = BluetoothAdapter.ACTION_STATE_CHANGED
    }

    private val bluetoothRepository: BluetoothRepository by inject()

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_ON -> {
                    bluetoothRepository.updateEnabledState(true)
                }
                else -> {
                    bluetoothRepository.updateEnabledState(false)
                }
            }
        }
    }

    override fun register(context: Context) {
        context.registerReceiver(
            this,
            IntentFilter(ACTION)
        )
    }

    override fun unregister(context: Context) {
        context.unregisterReceiver(this)
    }
}