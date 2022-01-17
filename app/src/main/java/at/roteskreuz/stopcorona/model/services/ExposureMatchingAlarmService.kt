package at.roteskreuz.stopcorona.model.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * AlarmManager that runs the exposure matching algorithm to detect if the user has been exposed
 * to a person infected or suspected to be infected with COVID-19.
 */
class ExposureMatchingAlarmService {

    companion object {

        /**
         * Enqueue periodic alarm to run the exposure matching algorithm.
         */
        fun enqueueExposurePeriodicMatching(
            context: Context
        ) {
            Timber.d("Scheduling every three hours exposure matching")

            val intent = Intent(context, ExposureMatchingService::class.java)
            val pendingIntent =
                PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + (12 * 60 * 60 * 1000),
                pendingIntent
            )
        }
    }
}