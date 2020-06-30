package at.roteskreuz.stopcorona.model.managers

import android.content.SharedPreferences
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.Constants.Prefs.CHANGELOG_MANAGER_PREFIX
import at.roteskreuz.stopcorona.constants.VERSION_NAME
import at.roteskreuz.stopcorona.skeleton.core.utils.nullableIntSharedPreferencesProperty
import at.roteskreuz.stopcorona.skeleton.core.utils.observeNullableInt
import io.reactivex.Observable

/**
 * Manages changelog information.
 */
interface ChangelogManager {

    /**
     * Get true if changelog should be displayed.
     * False if changelog is not provided for the current version or changelog was already displayed.
     */
    val shouldDisplayChangelog: Boolean

    /**
     * Get the changelog for the current version or null if changelog is not present.
     */
    val currentChangelog: Changelog?

    /**
     * Observe true if the current changelog has been seen, otherwise false.
     */
    fun observeIsChangelogSeen(): Observable<Boolean>

    /**
     * Marks the valid changelog as seen.
     */
    fun markChangelogAsSeen()
}

class ChangelogManagerImpl(
    private val preferences: SharedPreferences
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
    private val changelogs = listOf(
        Changelog(
            id = 1,
            versions = listOf("2.0.0", "2.0.3", "2.0.4"),
            title = R.string.changelog_title_v2_0_0,
            description = listOf(
                SpanTextWrapper.NoStyle(R.string.changelog_description_1_v2_0_0),
                SpanTextWrapper.Styled(textRes = R.string.changelog_description_2_v2_0_0, colored = true, insertTrailingSpace = false),
                SpanTextWrapper.NoStyle(R.string.changelog_description_3_v2_0_0)
            ),
            callToAction = R.string.changelog_cta_v2_0_0,
            image = R.drawable.ic_changelog
        )
    )
    private val currentVersionName = VERSION_NAME

    private var lastSeenChangelogId: Int? by preferences.nullableIntSharedPreferencesProperty(PREF_LAST_SEEN_CHANGELOG_ID)

    override val shouldDisplayChangelog: Boolean
        get() {
            return currentChangelog?.let {
                (lastSeenChangelogId ?: 0) < it.id
            } ?: false
        }

    override val currentChangelog: Changelog? by lazy {
        changelogs.firstOrNull {
            it.versions.contains(convertVersionName(currentVersionName))
        }
    }

    override fun observeIsChangelogSeen(): Observable<Boolean> {
        return preferences.observeNullableInt(PREF_LAST_SEEN_CHANGELOG_ID)
            .map {
                it.isPresent && currentChangelog?.id == it.get()
            }
    }

    override fun markChangelogAsSeen() {
        lastSeenChangelogId = currentChangelog?.id
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
    val description: List<SpanTextWrapper>,
    @StringRes val callToAction: Int,
    @DrawableRes val image: Int
)

sealed class SpanTextWrapper {

    data class NoStyle(@StringRes val textRes: Int) : SpanTextWrapper()

    data class Styled(
        @StringRes val textRes: Int,
        val colored: Boolean = false,
        val insertLeadingSpace: Boolean = true,
        val insertTrailingSpace: Boolean = true
    ) : SpanTextWrapper()
}
