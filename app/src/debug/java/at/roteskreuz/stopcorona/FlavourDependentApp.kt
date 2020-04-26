package at.roteskreuz.stopcorona

import com.facebook.stetho.Stetho
import timber.log.Timber

/**
 * Debug initialization.
 */
fun App.onPostCreateFlavourDependent() {
    // logging
    Timber.plant(Timber.DebugTree())

    // debug tool - show database, shared preferences, network requests etc.
    Stetho.initializeWithDefaults(this)
}