package at.roteskreuz.stopcorona.model.repositories.other

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.PowerManager
import android.text.SpannableString
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import at.roteskreuz.stopcorona.skeleton.core.utils.dip
import at.roteskreuz.stopcorona.utils.getBoldSpan
import at.roteskreuz.stopcorona.utils.string
import java.io.File
import java.util.Locale

/**
 * Interactor to provide context depending content.
 */
interface ContextInteractor {

    /**
     * Get application context. Use this method wisely.
     */
    val applicationContext: Context

    /**
     * Path to /data/data/packageName/files/ or equivalent /data/user/0/packageName/files/.
     */
    val filesDir: File

    /**
     * Android resources.
     */
    val resources: Resources

    /**
     * Get device screen width in pixels.
     */
    val screenWidth: Int

    /**
     * Get device screen height in pixels.
     */
    val screenHeight: Int

    /**
     * Get power manager.
     */
    val powerManager: PowerManager?

    /**
     * Get the package name.
     */
    val packageName: String

    /**
     * Shortcut for density independent pixels (dp).
     */
    fun dip(value: Int): Int

    /**
     * Get string from resources.
     */
    fun getString(@StringRes stringResId: Int, vararg params: Any): String

    /**
     * Get string from resources for specified locale.
     */
    fun getString(locale: Locale, @StringRes stringResId: Int, vararg params: Any): String

    /**
     * Shortcut via [ContextInteractor] to get a styled string.
     * More info in [Context.getBoldSpan]
     */
    fun getBoldSpan(
        @StringRes textRes: Int,
        colored: Boolean = false,
        insertLeadingSpace: Boolean = true,
        insertTrailingSpace: Boolean = true
    ): SpannableString

    /**
     * Check if permission is granted.
     * @return true if granted.
     */
    fun checkPermission(permissionName: String): Boolean

    /**
     * Register broadcast receiver inside some repository.
     */
    fun registerReceiver(receiver: BroadcastReceiver, intentFilter: IntentFilter)

    /**
     * Unregister broadcast receiver.
     */
    fun unregisterReceiver(receiver: BroadcastReceiver)
}

class ContextInteractorImpl(
    private val context: Context
) : ContextInteractor {

    override val applicationContext: Context
        get() = context.applicationContext

    override val filesDir: File
        get() = context.filesDir

    override val resources: Resources
        get() = context.resources

    private val displayMetrics: DisplayMetrics
        get() {
            val displayMetrics = DisplayMetrics()
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            return displayMetrics
        }

    override val screenWidth: Int
        get() = displayMetrics.widthPixels

    override val screenHeight: Int
        get() = displayMetrics.heightPixels

    override val powerManager: PowerManager?
        get() = context.getSystemService()

    override val packageName: String
        get() = context.packageName

    override fun dip(value: Int): Int {
        return context.dip(value)
    }

    override fun getString(@StringRes stringResId: Int, vararg params: Any): String {
        return context.string(stringResId, params)
    }

    override fun getString(locale: Locale, @StringRes stringResId: Int, vararg params: Any): String {
        return localizedContext(locale).string(stringResId, params)
    }

    override fun getBoldSpan(
        @StringRes textRes: Int,
        colored: Boolean,
        insertLeadingSpace: Boolean,
        insertTrailingSpace: Boolean
    ): SpannableString {
        return context.getBoldSpan(textRes, colored, insertLeadingSpace, insertTrailingSpace)
    }

    override fun checkPermission(permissionName: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permissionName) == PackageManager.PERMISSION_GRANTED
    }

    override fun registerReceiver(receiver: BroadcastReceiver, intentFilter: IntentFilter) {
        context.registerReceiver(receiver, intentFilter)
    }

    override fun unregisterReceiver(receiver: BroadcastReceiver) {
        context.unregisterReceiver(receiver)
    }

    /**
     * Create a context to get translations for the specified [locale]
     */
    private fun localizedContext(locale: Locale): Context {
        val configuration = resources.configuration
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }
}