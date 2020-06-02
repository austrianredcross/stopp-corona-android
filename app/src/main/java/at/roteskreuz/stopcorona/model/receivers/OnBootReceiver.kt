package at.roteskreuz.stopcorona.model.receivers

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.repositories.BluetoothRepository
import at.roteskreuz.stopcorona.model.repositories.ExposureNotificationRepository
import at.roteskreuz.stopcorona.model.repositories.NotificationsRepository
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber

/**
 * Display a notification to turn on Bluetooth when the app is registered as the Exposure
 * Notifications application
 */
//TODO: discuss onboot notification https://tasks.pxp-x.com/browse/CTAA-1548
class OnBootReceiver : BroadcastReceiver(), KoinComponent {

    private val notificationsRepository: NotificationsRepository by inject()
    private val exposureNotificationRepository: ExposureNotificationRepository by inject()
    private val bluetoothRepository: BluetoothRepository by inject()

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        if (bluetoothRepository.isBluetoothEnabled().not()){
            try {
                if (exposureNotificationRepository.isAppRegisteredForExposureNotifications){
                    notificationsRepository.displayPleaseActivateBluetoothNotification()
                }
            }  catch (e:Exception){
                //if it goes wrong, we do not case as we can´t handle the error
                Timber.e(SilentError(e))
            }

        }

    }
}