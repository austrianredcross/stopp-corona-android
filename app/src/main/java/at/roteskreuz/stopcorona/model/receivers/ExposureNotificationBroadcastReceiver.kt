/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package at.roteskreuz.stopcorona.model.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import timber.log.Timber

/**
 * Broadcast receiver for callbacks from exposure notification API.
 */
class ExposureNotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED == action) {
            val token = intent.getStringExtra(ExposureNotificationClient.EXTRA_TOKEN)
            Timber.d("got a token '%s' which could be infected, we should check it", token)
            //TODO process the exposure notification
            // see https://github.com/google/exposure-notifications-android/blob/master/app/src/main/java/com/google/android/apps/exposurenotification/nearby/StateUpdatedWorker.java
            // and https://github.com/google/exposure-notifications-android/blob/master/app/src/main/java/com/google/android/apps/exposurenotification/nearby/ExposureNotificationBroadcastReceiver.java
        }
    }
}