package at.roteskreuz.stopcorona.skeleton.core.utils

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import at.roteskreuz.stopcorona.skeleton.core.R
import com.google.android.material.snackbar.Snackbar

/**
 * Extensions for views.
 */

/**
 * Shortcuts for density independent pixels (dp).
 */
fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()

/**
 * Shortcuts for density independent pixels (dp) for floats as input, float as output.
 */
fun Context.dipif(value: Int): Float = (value * resources.displayMetrics.density)

/**
 * Shortcuts for density independent pixels (dp) for floats
 */
fun Context.dipff(value: Float): Float = (value * resources.displayMetrics.density)

/**
 * Shortcuts for density independent pixels (dp) for floats as input, int as output
 */
fun Context.dipfi(value: Float): Int = (value * resources.displayMetrics.density).toInt()

/**
 * Shortcut for pick color from resources.
 */
@ColorInt
fun Context.color(@ColorRes res: Int): Int {
    return ContextCompat.getColor(this, res)
}

/**
 * Shows keyboard on editText and when focus is lost keyboard will hide automatically.
 */
fun EditText.showKeyboard() {
    this.post {
        requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }
}

/**
 * Hide shown keyboard.
 */
fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

/**
 * Display the Snackbar with the [Snackbar.LENGTH_SHORT] duration.
 *
 * @param message the message text resource.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun View.snackbar(@StringRes message: Int) = Snackbar
    .make(this, message, Snackbar.LENGTH_SHORT)
    .apply { show() }

/**
 * Display the Snackbar with the [Snackbar.LENGTH_SHORT] duration.
 *
 * @param message the message text resource.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun View.snackbar(message: String) = Snackbar
    .make(this, message, Snackbar.LENGTH_SHORT)
    .apply { show() }

/**
 * Create circular loading. Can be used i.e. when images are loading.
// */
fun Context.createCircularProgressDrawable(stroke: Int = 2, radius: Int = 16): CircularProgressDrawable {
    return CircularProgressDrawable(this).apply {
        strokeWidth = dipif(stroke)
        centerRadius = dipif(radius)
        setColorSchemeColors(color(R.color.accent))
        start()
    }
}

/**
 * Add arguments to fragment.
 */
fun <T : Fragment> T.withArguments(vararg params: Pair<String, Any?>): T {
    arguments = bundleOf(*params)
    return this
}

/**
 * Add bundle arguments to fragment.
 */
fun <T : Fragment> T.withArguments(bundle: Bundle): T {
    arguments = bundle
    return this
}

/**
 * Sets the visibility of the View.
 */
var View.visible: Boolean
    set(value) {
        visibility = if (value) View.VISIBLE else View.GONE
    }
    get() = visibility == View.VISIBLE

/**
 * Sets the visibility of the View.
 */
var View.invisible: Boolean
    set(value) {
        visibility = if (value) View.INVISIBLE else View.VISIBLE
    }
    get() = visibility == View.INVISIBLE

/**
 * Get the raw dimension.
 */
fun Context.rawDimen(@DimenRes dimenRes: Int): Float {
    return resources.getDimension(dimenRes) / resources.displayMetrics.density
}