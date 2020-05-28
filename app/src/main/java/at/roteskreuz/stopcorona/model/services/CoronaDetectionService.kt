package at.roteskreuz.stopcorona.model.services

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.model.repositories.CoronaDetectionRepository
import at.roteskreuz.stopcorona.model.repositories.NotificationsRepository
import at.roteskreuz.stopcorona.screens.base.DebugOnly
import org.koin.android.ext.android.inject

/**
 * Foreground service for automatic detection of bluetooth devices nearby.
 */
// TODO: 27/05/2020 dusanjencik: Remove
class CoronaDetectionService : Service() {

    companion object {
        private const val ACTION_START = "ACTION_START"
        private const val ACTION_RESUME = "ACTION_RESUME"
        private const val ACTION_UPDATE = "ACTION_UPDATE"
        private const val ACTION_PAUSE = "ACTION_PAUSE"
        private const val ACTION_STOP = "ACTION_STOP"

        private const val AUTOMATIC_DETECTION_NOTIFICATION_ID = Constants.Request.AUTOMATIC_DETECTION_NOTIFICATION_ID + 1

        fun getStartIntent(context: Context): Intent {
            return context.buildCoronaDetectionIntent {
                action = ACTION_START
            }
        }

        fun getResumeIntent(context: Context): Intent {
            return context.buildCoronaDetectionIntent {
                action = ACTION_RESUME
            }
        }

        fun getUpdateIntent(context: Context): Intent {
            return context.buildCoronaDetectionIntent {
                action = ACTION_UPDATE
            }
        }

        fun getPauseIntent(context: Context): Intent {
            return context.buildCoronaDetectionIntent {
                action = ACTION_PAUSE
            }
        }

        fun getStopIntent(context: Context): Intent {
            return context.buildCoronaDetectionIntent {
                action = ACTION_STOP
            }
        }

        /**
         * Use wisely.
         */
        @DebugOnly
        fun isRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            return manager.getRunningServices(Int.MAX_VALUE).any { service ->
                service.service.className == CoronaDetectionService::class.java.name
            }
        }

        private fun Context.buildCoronaDetectionIntent(intentParams: Intent.() -> Unit): Intent {
            val serviceIntent = Intent(this, CoronaDetectionService::class.java)
            serviceIntent.intentParams()
            return serviceIntent
        }
    }

    private val coronaDetectionRepository: CoronaDetectionRepository by inject()
    private val notificationsRepository: NotificationsRepository by inject()

    override fun onBind(intent: Intent?): IBinder? {
        return null // no binding
    }

    override fun onCreate() {
        super.onCreate()
        coronaDetectionRepository.startListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            null, // when service is restarted
            ACTION_START -> start()
            ACTION_RESUME -> resume()
            ACTION_UPDATE -> update()
            ACTION_PAUSE -> pause()
            ACTION_STOP -> stop()
        }
        return START_STICKY // service will be restarted if killed by system
    }

    private fun start() {
        resume()
    }

    private fun resume() {
        coronaDetectionRepository.isServiceRunning = true
        updateNotification()
    }

    private fun update() {
        updateNotification()
    }

    private fun pause() {
        coronaDetectionRepository.isServiceRunning = false
        updateNotification()
    }

    private fun stop() {
        pause()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            stopForeground(true)
        }
        stopSelf()
        updateNotification(show = false)
    }

    override fun onDestroy() {
        coronaDetectionRepository.stopListening()
        super.onDestroy()
    }

    private fun updateNotification(show: Boolean = true) {
        if (show) {
            val isServiceRunning = coronaDetectionRepository.isServiceRunning
            if (isServiceRunning.not()) {
                notificationsRepository.updateAndDisplayCoronaAutomaticDetectionNotification(
                    AUTOMATIC_DETECTION_NOTIFICATION_ID
                )
            } else {
                startForeground(
                    AUTOMATIC_DETECTION_NOTIFICATION_ID,
                    notificationsRepository.getCoronaAutomaticDetectionNotification()
                )
            }
        } else {
            notificationsRepository.hideNotification(AUTOMATIC_DETECTION_NOTIFICATION_ID)
        }
    }
}

fun Context.startCoronaDetectionService() {
    ContextCompat.startForegroundService(
        this,
        CoronaDetectionService.getStartIntent(this)
    )
}

fun Context.updateCoronaDetectionService() {
    startService(
        CoronaDetectionService.getUpdateIntent(this)
    )
}

fun Context.stopCoronaDetectionService() {
    startService(
        CoronaDetectionService.getStopIntent(this)
    )
}