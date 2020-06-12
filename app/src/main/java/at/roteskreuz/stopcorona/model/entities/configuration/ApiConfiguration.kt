package at.roteskreuz.stopcorona.model.entities.configuration

import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.skeleton.core.model.entities.ApiEntity
import at.roteskreuz.stopcorona.utils.asEnum
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

/**
 * Describes configuration of questionnaire content.
 */
@JsonClass(generateAdapter = true)
data class ApiConfigurationHolder(
    val configuration: ApiConfiguration
)

@JsonClass(generateAdapter = true)
data class ApiConfiguration(
    @field:Json(name = "warn_before_symptoms")
    val warnBeforeSymptoms: Int?,
    @field:Json(name = "red_warning_quarantine")
    val redWarningQuarantine: Int?,
    @field:Json(name = "yellow_warning_quarantine")
    val yellowWarningQuarantine: Int?,
    @field:Json(name = "self_diagnosed_quarantine")
    val selfDiagnosedQuarantine: Int?,
    @field:Json(name = "diagnostic_questionnaire")
    val diagnosticQuestionnaire: ApiDiagnosticQuestionnaire?,
    @field:Json(name = "page_list")
    val pageList: ApiPageList?,
    @field:Json(name = "upload_keys_days")
    val uploadKeysDays: Int = Constants.Configuration.UPLOAD_KEYS_DAYS
) : ApiEntity<DbConfiguration> {

    override fun asDbEntity(): DbConfiguration {
        return DbConfiguration(
            warnBeforeSymptoms = warnBeforeSymptoms,
            redWarningQuarantine = redWarningQuarantine,
            yellowWarningQuarantine = yellowWarningQuarantine,
            selfDiagnosedQuarantine = selfDiagnosedQuarantine,
            uploadKeysDays = uploadKeysDays
        )
    }
}

@JsonClass(generateAdapter = true)
data class ApiDiagnosticQuestionnaire(
    val cz: List<ApiQuestionnaire>?,
    val de: List<ApiQuestionnaire>?,
    val en: List<ApiQuestionnaire>?,
    val fr: List<ApiQuestionnaire>?,
    val hu: List<ApiQuestionnaire>?,
    val sk: List<ApiQuestionnaire>?
)

@JsonClass(generateAdapter = true)
data class ApiQuestionnaire(
    val title: String?,
    val questionText: String?,
    val answers: List<ApiQuestionnaireAnswer>?
) : ApiEntity<DbQuestionnaire> {

    override fun asDbEntity(): DbQuestionnaire {
        return DbQuestionnaire(
            title = title,
            questionText = questionText
        )
    }
}

@JsonClass(generateAdapter = true)
data class ApiQuestionnaireAnswer(
    val text: String?,
    @field:Json(name = "decission")
    val _decision: String?
) : ApiEntity<DbQuestionnaireAnswer> {

    val decision: Decision? by lazy { _decision?.toUpperCase(Locale.getDefault())?.asEnum<Decision>() }

    override fun asDbEntity(): DbQuestionnaireAnswer {
        return DbQuestionnaireAnswer(
            text = text,
            decision = decision
        )
    }
}

enum class Decision {
    NEXT, HINT, SUSPICION, SELFMONITORING
}

@JsonClass(generateAdapter = true)
data class ApiPageList(
    val cz: ApiPageContent?,
    val de: ApiPageContent?,
    val en: ApiPageContent?,
    val fr: ApiPageContent?,
    val hu: ApiPageContent?,
    val sk: ApiPageContent?
)

@JsonClass(generateAdapter = true)
data class ApiPageContent(
    @field:Json(name = "ALL_CLEAR")
    val allClear: ApiTextContent?,
    @field:Json(name = "HINT")
    val hint: ApiTextContent?,
    @field:Json(name = "SELFMONITORING")
    val selfMonitoring: ApiTextContent?,
    @field:Json(name = "SUSPICION")
    val suspicion: ApiTextContent?
) : ApiEntity<DbPageContent> {

    override fun asDbEntity(): DbPageContent {
        return DbPageContent(
            allClear = allClear?.asDbEntity(),
            hint = hint?.asDbEntity(),
            selfMonitoring = selfMonitoring?.asDbEntity(),
            suspicion = suspicion?.asDbEntity()
        )
    }
}

@JsonClass(generateAdapter = true)
data class ApiTextContent(
    val boldText: String?,
    val longText: String?,
    @field:Json(name = "roofline")
    val roofLine: String?,
    val title: String?
) : ApiEntity<DbTextContent> {

    override fun asDbEntity(): DbTextContent {
        return DbTextContent(
            boldText = boldText,
            longText = longText,
            roofLine = roofLine,
            title = title
        )
    }
}