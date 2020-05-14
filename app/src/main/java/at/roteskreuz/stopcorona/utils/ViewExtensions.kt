package at.roteskreuz.stopcorona.utils

import android.app.Activity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.di.GlideApp
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.skeleton.core.utils.createCircularProgressDrawable
import at.roteskreuz.stopcorona.skeleton.core.utils.snackbar
import timber.log.Timber

/**
 * Extensions for views, project specific.
 */

/**
 * Snackbar for debug purposes.
 */
fun Fragment.notImplementedSnackbar() {
    view?.snackbar("Not implemented yet") ?: Timber.e(SilentError("view is null"))
}

/**
 * Load local or remote image to [ImageView] or show placeholder in case of fail.
 * Custom error image can be defined.
 */
fun ImageView.loadImageFrom(
    url: String?,
    circleCrop: Boolean = false,
    @DrawableRes placeholderImage: Int? = null,
    @DrawableRes errorImage: Int = R.drawable.ic_cloud_off
) {
    @Suppress("IMPLICIT_CAST_TO_ANY")
    val finalUrl = when (url) {
        null -> placeholderImage
        else -> url
    }
    GlideApp.with(context)
        .load(finalUrl)
        .apply {
            if (circleCrop) {
                circleCrop()
            }
        }
        .placeholder(context.createCircularProgressDrawable())
        .error(errorImage)
        .into(this)
}

fun Activity.darkTextInStatusBar() {
    with(window.decorView) {
        systemUiVisibility = systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }
}

fun Activity.lightTextInStatusBar() {
    with(window.decorView) {
        systemUiVisibility = systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
    }
}

/**
 * Sets the color provided as parameter as background color.
 */
fun View.backgroundColor(@ColorRes color: Int) {
    this.setBackgroundColor(ContextCompat.getColor(this.context, color))
}

/**
 * Apply tint to ImageView.
 */
fun ImageView.tint(@ColorRes color: Int) {
    setColorFilter(ContextCompat.getColor(this.context, color))
}

/**
 * Set app style to the alert dialog.
 */
fun AlertDialog.withCustomStyle(): AlertDialog = this.also {
    window?.setBackgroundDrawable(context.drawable(R.drawable.dialog_style))
}

/**
 * Start phone call dialer when clicked on textView.
 * The phone number is the content of the text view.
 */
fun TextView.startPhoneCallOnClick() {
    setOnClickListener {
        context.startCallWithPhoneNumber(text.toString())
    }
}