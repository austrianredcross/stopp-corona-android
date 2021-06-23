package at.roteskreuz.stopcorona.utils

import android.content.Context
import com.google.android.gms.common.GoogleApiAvailability

fun Context.isGmsAvailable() : Boolean {
    return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == com.google.android.gms.common.ConnectionResult.SUCCESS
}