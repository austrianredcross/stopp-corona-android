package at.roteskreuz.stopcorona.hms

import android.app.IntentService
import android.content.Intent
import com.huawei.hms.contactshield.ContactShield
import com.huawei.hms.contactshield.ContactShieldCallback
import com.huawei.hms.contactshield.ContactShieldEngine
import timber.log.Timber

class ContactShieldIntentService : IntentService("StoppCorona_ContactShieldIntentService") {

    private lateinit var contactShieldEngine: ContactShieldEngine

    private val callback: ContactShieldCallback = object : ContactShieldCallback {

        override fun onHasContact(token: String?) {
            Timber.tag(LOG_TAG).d("Has contact with '$token'.")
        }

        override fun onNoContact(token: String?) {
            Timber.tag(LOG_TAG).d("Has no contact with '$token'.")
        }

    }

    override fun onCreate() {
        super.onCreate()

        contactShieldEngine = ContactShield.getContactShieldEngine(this)

    }

    override fun onHandleIntent(intent: Intent?) {

        if (intent == null) {
            return
        }

        contactShieldEngine.handleIntent(intent, callback)

    }

    companion object {
        private const val LOG_TAG = "ContactShieldService"
    }
}