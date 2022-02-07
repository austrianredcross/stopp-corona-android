package at.roteskreuz.stopcorona.model.repositories

import android.content.SharedPreferences
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.skeleton.core.utils.booleanSharedPreferencesProperty

interface SunDownerRepository {

    /**
     * Indicated if the sun downer should be shown.
     */
    val sunDownerShown: Boolean

    /**
     * SunDowner was seen and accepted so we don't want to show it anymore.
     */
    fun setSunDownerShown()

    /**
     * Indicated if the sun downer notification should be shown.
     */
    val sunDownerNotificationShown: Boolean

    /**
     * SunDowner notification was seen so we don't want to show it anymore.
     */
    fun setSunDownerNotificationShown()

    /**
     * Indicated if the sun downer last day notification should be shown.
     */
    val sunDownerLastDayNotificationShown: Boolean

    /**
     * SunDowner last day notification was seen so we don't want to show it anymore.
     */
    fun setSunDownerLastDayNotificationShown()
}
class SunDownerRepositoryImpl(
    preferences: SharedPreferences
) : SunDownerRepository {

    companion object {
        private const val PREF_SUN_DOWNER_SHOWN = Constants.Prefs.SUN_DOWNER_PREFIX + "sun_downer_shown"
        private const val PREF_SUN_DOWNER_NOTIFICATION_SHOWN = Constants.Prefs.SUN_DOWNER_PREFIX + "sun_downer_notification_shown"
        private const val PREF_SUN_DOWNER_LAST_DAY_NOTIFICATION_SHOWN = Constants.Prefs.SUN_DOWNER_PREFIX + "sun_downer_last_day_notification_shown"
    }

    private var wasSunDownerShown: Boolean
            by preferences.booleanSharedPreferencesProperty(PREF_SUN_DOWNER_SHOWN, false)

    private var wasSunDownerNotificationShown: Boolean
            by preferences.booleanSharedPreferencesProperty(PREF_SUN_DOWNER_NOTIFICATION_SHOWN, false)

    private var wasSunDownerLastDayNotificationShown: Boolean
            by preferences.booleanSharedPreferencesProperty(PREF_SUN_DOWNER_LAST_DAY_NOTIFICATION_SHOWN, false)


    override val sunDownerShown: Boolean
        get() = wasSunDownerShown

    override val sunDownerNotificationShown: Boolean
        get() = wasSunDownerNotificationShown

    override val sunDownerLastDayNotificationShown: Boolean
        get() = wasSunDownerLastDayNotificationShown

    override fun setSunDownerShown() {
        wasSunDownerShown = true
    }

    override fun setSunDownerNotificationShown() {
        wasSunDownerNotificationShown = true
    }

    override fun setSunDownerLastDayNotificationShown() {
        wasSunDownerLastDayNotificationShown = true
    }

}