package at.roteskreuz.stopcorona.hms

import android.app.IntentService
import android.content.Context
import android.content.Intent
import com.huawei.hms.contactshield.ContactShieldEngine
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber

abstract class AbstractContactShieldIntentService : IntentService("StoppCorona_ContactShieldIntentService"), KoinComponent {

    abstract fun onExposureStateUpdated(context: Context, token: String)

    private val contactShieldEngine: ContactShieldEngine by inject()
    private val callback: com.huawei.hms.contactshield.ContactShieldCallback = object : com.huawei.hms.contactshield.ContactShieldCallback {

        override fun onHasContact(token: String?) {

            Timber.tag(LOG_TAG).d("Has contact with '$token'.")

            if (token != null) {
                onExposureStateUpdated(this@AbstractContactShieldIntentService, token)
            }

        }

        override fun onNoContact(token: String?) {

            Timber.tag(LOG_TAG).d("Has no contact with '$token'.")

            if (token != null) {
                onExposureStateUpdated(this@AbstractContactShieldIntentService, token)
            }
        }

    }

    override fun onHandleIntent(intent: Intent?) {

        if (intent == null) {
            Timber.tag(LOG_TAG).w("Received intent 'null'.")
            return
        }

        contactShieldEngine.handleIntent(intent, callback)

    }


    companion object {
        private const val LOG_TAG = "ContactShieldService"
    }
}