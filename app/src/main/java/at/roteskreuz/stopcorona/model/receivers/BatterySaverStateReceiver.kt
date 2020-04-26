package at.roteskreuz.stopcorona.model.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.services.CoronaDetectionService
import timber.log.Timber

/**
 * Update [CoronaDetectionService] when battery saver mode changed.
 */
class BatterySaverStateReceiver : BroadcastReceiver(), Registrable {

    companion object {
        private const val ACTION = PowerManager.ACTION_POWER_SAVE_MODE_CHANGED
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION) {
            Timber.e(SilentError("Warning: Battery optimizations not ignored"))
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