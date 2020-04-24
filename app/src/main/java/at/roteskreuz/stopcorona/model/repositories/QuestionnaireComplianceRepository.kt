package at.roteskreuz.stopcorona.model.repositories

import android.content.SharedPreferences
import at.roteskreuz.stopcorona.constants.Constants.Prefs.QUESTIONNAIRE_COMPLIANCE_PREFIX
import at.roteskreuz.stopcorona.skeleton.core.utils.nullableZonedDateTimeSharedPreferencesProperty
import org.threeten.bp.ZonedDateTime

/**
 * Repository for managing the state of the questionnaire compliance state.
 */
interface QuestionnaireComplianceRepository {

    /**
     * Indicates if the compliance was accepted by the user
     */
    val complianceAccepted: Boolean

    /**
     * Sets a timestamp when the compliance is accepted
     */
    fun setComplianceAccepted()
}

class QuestionnaireComplianceRepositoryImpl(
    preferences: SharedPreferences
) : QuestionnaireComplianceRepository {

    companion object {
        private const val PREF_QUESTIONNAIRE_COMPLIANCE_ACCEPTED_TIMESTAMP = QUESTIONNAIRE_COMPLIANCE_PREFIX + "compliance_accepted_timestamp"
    }

    private var complianceAcceptedTimestamp: ZonedDateTime? by preferences.nullableZonedDateTimeSharedPreferencesProperty(
        PREF_QUESTIONNAIRE_COMPLIANCE_ACCEPTED_TIMESTAMP)

    override val complianceAccepted: Boolean
        get() = complianceAcceptedTimestamp != null

    override fun setComplianceAccepted() {
        complianceAcceptedTimestamp = ZonedDateTime.now()
    }
}
