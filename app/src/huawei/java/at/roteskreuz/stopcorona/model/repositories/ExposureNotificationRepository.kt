package at.roteskreuz.stopcorona.model.repositories

import at.roteskreuz.stopcorona.ActivityLifeCycleHelper
import at.roteskreuz.stopcorona.screens.routing.RouterActivity
import at.roteskreuz.stopcorona.screens.routing.RouterActivity.Companion.SCREEN_DISPLAY_DURATION
import kotlinx.coroutines.delay

suspend fun ExposureNotificationRepository.handleFrameworkSpecificSituationOnAutoStart() {
    if(ActivityLifeCycleHelper.currentResumedActivity is RouterActivity) {
        delay(SCREEN_DISPLAY_DURATION) //wait until router activity is finished and has started dashboard activity
    }
}