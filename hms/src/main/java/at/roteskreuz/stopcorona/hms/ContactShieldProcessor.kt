package at.roteskreuz.stopcorona.hms

import android.content.Context

interface ContactShieldServiceProcessor {
    fun onExposureStateUpdated(context: Context, token: String)
}