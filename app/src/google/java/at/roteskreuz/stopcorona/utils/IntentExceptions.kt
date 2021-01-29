package at.roteskreuz.stopcorona.utils

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment

/**
 * Start Google Play Store detail of the application defined by [packageName].
 */
fun Fragment.startPlatformAppStore(packageName: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
    } catch (exc: ActivityNotFoundException) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
    }
}