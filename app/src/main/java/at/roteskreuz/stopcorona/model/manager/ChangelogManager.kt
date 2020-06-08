package at.roteskreuz.stopcorona.model.manager

import android.content.SharedPreferences
import android.text.SpannableString
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.Constants.Prefs.CHANGELOG_MANAGER_PREFIX
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor
import at.roteskreuz.stopcorona.skeleton.core.utils.intSharedPreferencesProperty

/**
 * Manages changelog information.
 */
interface ChangelogManager {

    /**
     * Checks if there is an unseen changelog for the given [version].
     */
    fun unseenChangelogForVersionAvailable(version: String): Boolean

    /**
     * Get the changelog for the given [version].
     *
     * @return null when there is no changelog for the given [version]
     * or the changelog has already been displayed.
     */
    fun getChangelogForVersion(version: String): Changelog?
}

class ChangelogManagerImpl(
    preferences: SharedPreferences,
    private val contextInteractor: ContextInteractor
) : ChangelogManager {

    companion object {
        private const val PREF_LAST_SEEN_CHANGELOG_ID = CHANGELOG_MANAGER_PREFIX + "last_seen_changelog_id"
    }

    /**
     * Changelog content for a list of specific versions.
     *
     * [Changelog.id] has to be increased for every new changelog.
     * [Changelog.versions] contains the list of versions this changelog applies to.
     */
    private val changelog = Changelog(
        id = 1,
        versions = listOf("2.0.0", "2.0.1"),
        title = R.string.changelog_title_v2_0_0,
        description = SpannableString(contextInteractor.getString(R.string.changelog_description_v2_0_0)),
        callToAction = R.string.changelog_cta_v2_0_0,
        image = R.drawable.ic_changelog
    )

    private var lastSeenChangelogId: Int by preferences.intSharedPreferencesProperty(PREF_LAST_SEEN_CHANGELOG_ID, 0)

    private val hasBeenDisplayed: Boolean
        get() = changelog.id <= lastSeenChangelogId

    override fun unseenChangelogForVersionAvailable(version: String): Boolean {
        return changelog.versions.contains(convertVersionName(version)) && hasBeenDisplayed.not()
    }

    override fun getChangelogForVersion(version: String): Changelog? {
        return if (unseenChangelogForVersionAvailable(convertVersionName(version))) {
//            lastSeenChangelogId = changelog.id
            changelog
        } else {
            null
        }
    }

    /**
     * Convert the version name which is in this project in the manner of "2.0.0.12-TAG-ID-HASH-FLAVOR"
     * to only use the first three version-values.
     */
    private fun convertVersionName(version: String): String {
        return version.split(".").take(3).joinToString(".")
    }
}

data class Changelog(
    val id: Int,
    val versions: List<String>,
    @StringRes val title: Int,
    val description: SpannableString,
    @StringRes val callToAction: Int,
    @DrawableRes val image: Int
)
