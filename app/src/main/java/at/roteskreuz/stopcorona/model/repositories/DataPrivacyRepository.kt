package at.roteskreuz.stopcorona.model.repositories

import android.content.SharedPreferences
import at.roteskreuz.stopcorona.constants.Constants.Prefs.DATA_PRIVACY_REPOSITORY_PREFIX
import at.roteskreuz.stopcorona.model.api.ApiError
import at.roteskreuz.stopcorona.skeleton.core.utils.booleanSharedPreferencesProperty
import at.roteskreuz.stopcorona.skeleton.core.utils.nullableZonedDateTimeSharedPreferencesProperty
import at.roteskreuz.stopcorona.skeleton.core.utils.observeNullableZonedDateTime
import kotlinx.coroutines.suspendCancellableCoroutine
import org.threeten.bp.ZonedDateTime
import kotlin.coroutines.resume

/**
 * Repository for managing the state of the data privacy check.
 */
interface DataPrivacyRepository {

    /**
     * Indicated if data privacy was fully accepted.
     */
    val dataPrivacyAccepted: Boolean

    /**
     * Data privacy was seen and accepted so we don't want to show it anymore.
     */
    fun setDataPrivacyAccepted()

    /**
     * Indicated if new data privacy was fully accepted.
     */
    val newDataPrivacyAccepted: Boolean

    /**
     * New data privacy was seen and accepted so we don't want to show it anymore.
     */
    fun setNewDataPrivacyAccepted()

    /**
     * Check if data privacy is accepted.
     * If not, error is thrown.
     * @throws [ApiError.Critical.DataPrivacyNotAcceptedYet]
     */
    fun assertDataPrivacyAccepted()

    /**
     * Block the current coroutine flow until we have accepted data privacy.
     * When coroutine canceled, waiting is canceled as well.
     */
    suspend fun awaitForAcceptanceState()
}

class DataPrivacyRepositoryImpl(
    private val preferences: SharedPreferences
) : DataPrivacyRepository {

    companion object {
        private const val PREF_DATA_PRIVACY_ACCEPTED_TIMESTAMP = DATA_PRIVACY_REPOSITORY_PREFIX + "_data_privacy_timestamp"
        private const val PREF_DATA_PRIVACY_ACCEPTED_TIMESTAMP_V1_1 = DATA_PRIVACY_REPOSITORY_PREFIX + "data_privacy_timestamp_v1.1"
        private const val PREF_NEW_DATA_PRIVACY_ACCEPTED = DATA_PRIVACY_REPOSITORY_PREFIX + "new_data_privacy_accepted"
    }

    private var dataPrivacyAcceptedTimestamp: ZonedDateTime?
        by preferences.nullableZonedDateTimeSharedPreferencesProperty(PREF_DATA_PRIVACY_ACCEPTED_TIMESTAMP)

    @Suppress("PrivatePropertyName")
    private var dataPrivacyAcceptedTimestampV1_1: ZonedDateTime?
        by preferences.nullableZonedDateTimeSharedPreferencesProperty(PREF_DATA_PRIVACY_ACCEPTED_TIMESTAMP_V1_1)

    private var hasAcceptedNewPrivacy: Boolean
            by preferences.booleanSharedPreferencesProperty(PREF_NEW_DATA_PRIVACY_ACCEPTED, false)

    override val dataPrivacyAccepted: Boolean
        get() = dataPrivacyAcceptedTimestamp != null && dataPrivacyAcceptedTimestampV1_1 != null

    override fun setDataPrivacyAccepted() {
        /**
         * Don't override the v1.0 timestamp when accepting terms and conditions for v1.1
         */
        if (dataPrivacyAcceptedTimestamp == null) {
            dataPrivacyAcceptedTimestamp = ZonedDateTime.now()
        }

        dataPrivacyAcceptedTimestampV1_1 = ZonedDateTime.now()
    }

    override val newDataPrivacyAccepted: Boolean
        get() = hasAcceptedNewPrivacy

    override fun setNewDataPrivacyAccepted() {
        hasAcceptedNewPrivacy = true
    }

    override fun assertDataPrivacyAccepted() {
        if (dataPrivacyAccepted.not()) {
            throw ApiError.Critical.DataPrivacyNotAcceptedYet
        }
    }

    override suspend fun awaitForAcceptanceState() {
        suspendCancellableCoroutine<Unit> { continuation ->
            val disposable = preferences.observeNullableZonedDateTime(PREF_DATA_PRIVACY_ACCEPTED_TIMESTAMP_V1_1)
                .filter { it.isPresent } // wait for an approval
                .firstOrError()
                .subscribe({
                    continuation.resume(Unit)
                }, {
                    continuation.resume(Unit)
                })
            continuation.invokeOnCancellation {
                disposable.dispose()
            }
        }
    }
}

