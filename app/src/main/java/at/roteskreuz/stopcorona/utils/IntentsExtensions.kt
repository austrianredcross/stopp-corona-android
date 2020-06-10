package at.roteskreuz.stopcorona.utils

import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import timber.log.Timber

/**
 * These extensions belong to the starting new actions based on intent.
 */

/**
 * Start dialer with pre-filled phone number.
 */
fun Context.startCallWithPhoneNumber(phoneNumber: String) {
    startActivity(Intent(
        Intent.ACTION_DIAL,
        Uri.parse("tel:" + phoneNumber.trim())
    ))
}

/**
 * Start composing email to [email].
 */
fun Fragment.startMailCompose(email: String) {
    startActivity(Intent(
        Intent.ACTION_SENDTO,
        Uri.parse("mailto:$email")
    ))
}

/**
 * Start browser with [url].
 */
fun Fragment.startDefaultBrowser(url: String) {
    try {
        startActivity(Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        ))
    } catch (e: ActivityNotFoundException) {
        Timber.e(SilentError("Trying to start browser for URL '$url' failed", e))
    }
}

/**
 * Start Google Play Store detail of the application defined by [packageName].
 */
fun Fragment.startGooglePlayStore(packageName: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
    } catch (exc: ActivityNotFoundException) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
    }
}

/**
 * Display dialog to enable bluetooth.
 */
fun Fragment.startDialogToEnableBluetooth() {
    startActivity(
        Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    )
}

/**
 * Open system settings of this application.
 */
fun Context.startAppSystemSettings() {
    startActivity(Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ))
}

/**
 * Open system dialog to enable bluetooth.
 */
fun Fragment.enableBluetoothForResult(requestCode: Int) {
    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    startActivityForResult(enableBtIntent, requestCode)
}

/**
 * Open system sharing dialog for sharing app links to stores.
 */
fun Fragment.shareApp() {
    val sharingIntent = Intent(Intent.ACTION_SEND)
    sharingIntent.type = "text/plain"
    sharingIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_content));
    startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_app_chooser)))
}
