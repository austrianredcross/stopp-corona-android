package at.roteskreuz.stopcorona.model.manager

import android.content.SharedPreferences
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import at.roteskreuz.stopcorona.constants.Constants.Prefs.CHANGELOG_MANAGER_PREFIX
import at.roteskreuz.stopcorona.skeleton.core.utils.intSharedPreferencesProperty

/**
 * Manages changelog information.
 */
interface ChangelogManager {

    /**
     * Get the changelog for the given [version].
     *
     * @return null when there is no changelog for the given [version]
     * or the changelog has already been displayed.
     */
    fun getChangelogForVersion(version: String): Changelog?
}

class ChangelogManagerImpl(
    preferences: SharedPreferences
) : ChangelogManager {

    companion object {
        private const val PREF_LAST_SEEN_CHANGELOG_ID = CHANGELOG_MANAGER_PREFIX + "last_seen_changelog_id"
    }

    private var lastSeenChangelogId: Int by preferences.intSharedPreferencesProperty(PREF_LAST_SEEN_CHANGELOG_ID, 0)

    override fun getChangelogForVersion(version: String): Changelog {
        TODO("Not yet implemented")
    }
}

data class Changelog(
    val id: Int,
    @StringRes val title: Int,
    @DrawableRes val image: Int,
    @StringRes val description: Int
)
