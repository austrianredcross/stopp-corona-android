package at.roteskreuz.stopcorona.model.entities.configuration

import at.roteskreuz.stopcorona.skeleton.core.model.entities.ApiEntity
import at.roteskreuz.stopcorona.utils.asEnum
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Locale

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

    @field:Json(name = "exposure_configuration")
    val exposureConfiguration: ApiExposureConfiguration
) : ApiEntity<DbConfiguration> {

    override fun asDbEntity(): DbConfiguration {
        return DbConfiguration(
            warnBeforeSymptoms = warnBeforeSymptoms,
            redWarningQuarantine = redWarningQuarantine,
            yellowWarningQuarantine = yellowWarningQuarantine,
            selfDiagnosedQuarantine = selfDiagnosedQuarantine,
            exposureConfigurationMinimumRiskScore = exposureConfiguration.minimumRiskScore,
            exposureConfigurationDailyRiskThreshold = exposureConfiguration.dailyRiskThreshold,
            exposureConfigurationAttenuationDurationThresholds = exposureConfiguration.attenuationDurationThresholds,
            exposureConfigurationAttenuationLevelValues = exposureConfiguration.attenuationLevelValues,
            exposureConfigurationDaysSinceLastExposureLevelValues = exposureConfiguration.daysSinceLastExposureLevelValues,
            exposureConfigurationDurationLevelValues = exposureConfiguration.durationLevelValues,
            exposureConfigurationTransmissionRiskLevelValues = exposureConfiguration.transmissionRiskLevelValues
        )
    }
}

@JsonClass(generateAdapter = true)
data class ApiExposureConfiguration (
    @field:Json(name = "minimum_risk_score")
    val minimumRiskScore: Int,
    @field:Json(name = "daily_risk_threshold")
    val dailyRiskThreshold: Int,
    @field:Json(name = "attenuation_duration_thresholds")
    val attenuationDurationThresholds: List<Int>,
    @field:Json(name = "attenuation_level_values")
    val attenuationLevelValues: List<Int>,
    @field:Json(name = "days_since_last_exposure_level_values")
    val daysSinceLastExposureLevelValues: List<Int>,
    @field:Json(name = "duration_level_values")
    val durationLevelValues: List<Int>,
    @field:Json(name = "transmission_risk_level_values")
    val transmissionRiskLevelValues: List<Int>
)


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