package at.roteskreuz.stopcorona.model.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.repositories.DiagnosisKeysRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.threeten.bp.LocalTime
import timber.log.Timber

class ExposureMatchingService : Service() {
    val scope = CoroutineScope(Dispatchers.Main)

    /**
     * 6:15
     */
    private val firstAvailableTime =
        Constants.Behavior.EXPOSURE_MATCHING_START_TIME.minusMinutes(Constants.Behavior.EXPOSURE_MATCHING_FLEX_DURATION.toMinutes() / 2)


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            Timber.d("Running exposure matching work")

            val diagnosisKeysRepository: DiagnosisKeysRepository by inject()

            if (LocalTime.now().isBefore(firstAvailableTime).not()) {
                try {
                    diagnosisKeysRepository.fetchAndForwardNewDiagnosisKeysToTheExposureNotificationFramework()
                } catch (ex: Exception) {
                    // we agreed to silently fail in case of errors here
                    Timber.e(SilentError(ex))
                }

            } else {
                Timber.d("Skipping exposure matching before ${firstAvailableTime}")
            }

            // reschedule the alarm to repeating alarm in doze mode
            ExposureMatchingAlarmService.enqueueExposurePeriodicMatching(applicationContext)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}