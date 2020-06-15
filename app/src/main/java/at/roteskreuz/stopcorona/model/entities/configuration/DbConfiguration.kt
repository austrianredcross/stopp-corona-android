package at.roteskreuz.stopcorona.model.entities.configuration

import androidx.room.*
import at.roteskreuz.stopcorona.constants.Constants.Questionnaire.COUNTRY_CODE_CZ
import at.roteskreuz.stopcorona.constants.Constants.Questionnaire.COUNTRY_CODE_DE
import at.roteskreuz.stopcorona.constants.Constants.Questionnaire.COUNTRY_CODE_EN
import at.roteskreuz.stopcorona.constants.Constants.Questionnaire.COUNTRY_CODE_FR
import at.roteskreuz.stopcorona.constants.Constants.Questionnaire.COUNTRY_CODE_HU
import at.roteskreuz.stopcorona.constants.Constants.Questionnaire.COUNTRY_CODE_SK
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.skeleton.core.model.db.converters.EnumTypeConverter
import at.roteskreuz.stopcorona.skeleton.core.model.entities.DbEntity
import org.threeten.bp.ZonedDateTime
import timber.log.Timber

/**
 * Describes configuration of questionnaire content.
 */
@Entity(
    tableName = "configuration"
)
data class DbConfiguration(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val warnBeforeSymptoms: Int?,
    val redWarningQuarantine: Int?,
    val yellowWarningQuarantine: Int?,
    val selfDiagnosedQuarantine: Int?,
    val uploadKeysDays: Int?,
    /**
     * Time of updating.
     */
    val cacheTime: ZonedDateTime = ZonedDateTime.now(),

    val minimumRiskScore: Int,                               // 1,
    val dailyRiskThreshold: Int,                             // 30,
    val attenuationDurationThresholds: List<Int>,            // [33, 63],
    val attenuationLevelValues: List<Int>,                   // [0, 1, 2, 2, 8, 8, 8, 8],
    val daysSinceLastExposureLevelValues: List<Int>,         // [1, 1, 1, 1, 1, 1, 1, 1],
    val durationLevelValues: List<Int>,                      // [0, 1, 2, 3, 4, 5, 6, 7],
    val transmissionRiskLevelValues: List<Int>               // [1, 1, 1, 1, 1, 1, 1, 1]
) : DbEntity

@Entity(
    tableName = "configuration_questionnaire",
    indices = [
        Index("configurationId"),
        Index("language")
    ],
    foreignKeys = [
        ForeignKey(
            entity = DbConfiguration::class,
            parentColumns = ["id"],
            childColumns = ["configurationId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DbQuestionnaire(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var configurationId: Long = 0, // will be set up during inserting to DB
    var language: ConfigurationLanguage = ConfigurationLanguage.UNDEFINED, // will be set up during inserting to DB
    val title: String?,
    val questionText: String?
) : DbEntity

enum class ConfigurationLanguage(val code: Int, val stringValue: String) {
    UNDEFINED(0, ""),
    CZ(1, COUNTRY_CODE_CZ),
    DE(2, COUNTRY_CODE_DE),
    EN(3, COUNTRY_CODE_EN),
    FR(4, COUNTRY_CODE_FR),
    HU(5, COUNTRY_CODE_HU),
    SK(6, COUNTRY_CODE_SK);

    companion object {
        fun parse(value: String): ConfigurationLanguage {
            return enumValues<ConfigurationLanguage>().firstOrNull {
                it.stringValue == value
            } ?: UNDEFINED
        }
    }
}

class ConfigurationLanguageConverter {
    @TypeConverter
    fun get(code: Int?): ConfigurationLanguage? {
        return ConfigurationLanguage.values().firstOrNull { it.code == code }.also {
            if (it == null) {
                Timber.e(SilentError("Asking for language with code $code"))
            }
        }
    }

    @TypeConverter
    fun set(language: ConfigurationLanguage?): Int? {
        return language?.code
    }
}

class ArrayOfIntegerConverter {
    companion object{
        private const val SEPARATOR = ","
    }
    @TypeConverter
    fun get(listOfIntegers: List<Int>?): String? {
        return listOfIntegers?.joinToString(SEPARATOR)
    }

    @TypeConverter
    fun set(commaSeparatedListOfIntegers: String?): List<Int>? {
        if (commaSeparatedListOfIntegers == null) {
            return null
        }
        if (commaSeparatedListOfIntegers.isEmpty()){
            return emptyList()
        }

        return commaSeparatedListOfIntegers.split(SEPARATOR).map { Integer.parseInt(it) }
    }
}

@Entity(
    tableName = "configuration_questionnaire_answer",
    indices = [
        Index("questionnaireId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = DbQuestionnaire::class,
            parentColumns = ["id"],
            childColumns = ["questionnaireId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DbQuestionnaireAnswer(
    @PrimaryKey(autoGenerate = true)
    val answerId: Long = 0,
    var questionnaireId: Long = 0, // will be set up during inserting to DB
    val text: String?,
    val decision: Decision?
) : DbEntity

class DecisionConverter : EnumTypeConverter<Decision>({ enumValueOf(it) })

@Entity(
    tableName = "page_content",
    indices = [
        Index("configurationId"),
        Index("language")
    ],
    foreignKeys = [
        ForeignKey(
            entity = DbConfiguration::class,
            parentColumns = ["id"],
            childColumns = ["configurationId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DbPageContent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var configurationId: Long = 0, // will be set up during inserting to DB
    var language: ConfigurationLanguage = ConfigurationLanguage.UNDEFINED, // will be set up during inserting to DB
    @Embedded(prefix = "allClear_")
    val allClear: DbTextContent?,
    @Embedded(prefix = "hint_")
    val hint: DbTextContent?,
    @Embedded(prefix = "selfMonitoring_")
    val selfMonitoring: DbTextContent?,
    @Embedded(prefix = "suspicion_")
    val suspicion: DbTextContent?
) : DbEntity

/**
 * Embedded entity.
 */
data class DbTextContent(
    val boldText: String?,
    val longText: String?,
    val roofLine: String?,
    val title: String?
) : DbEntity

/**
 * This entity is wrapping one question with list of possible answers.
 */
data class DbQuestionnaireWithAnswers(
    @Embedded
    var question: DbQuestionnaire,
    @Relation(
        entity = DbQuestionnaireAnswer::class,
        parentColumn = "id",
        entityColumn = "questionnaireId"
    )
    var answers: List<DbQuestionnaireAnswer> = listOf()
)