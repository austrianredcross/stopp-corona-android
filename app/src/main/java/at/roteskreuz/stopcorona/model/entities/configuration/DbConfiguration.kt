package at.roteskreuz.stopcorona.model.entities.configuration

import androidx.room.*
import at.roteskreuz.stopcorona.model.exceptions.SilentError
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
    /**
     * Time of updating.
     */
    val cacheTime: ZonedDateTime = ZonedDateTime.now()
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

enum class ConfigurationLanguage(val code: Int) {
    UNDEFINED(0),
    CZ(1),
    DE(2),
    EN(3),
    FR(4),
    HU(5),
    SK(6)
}

class ConfigurationLanguageConverter {
    @TypeConverter
    fun get(code: Int?): ConfigurationLanguage? {
        return ConfigurationLanguage.values().firstOrNull { it.code == code }?.also {
            Timber.e(SilentError("Asking for language with code $code"))
        }
    }

    @TypeConverter
    fun set(language: ConfigurationLanguage?): Int? {
        return language?.code
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
    val id: Long = 0,
    var questionnaireId: Long = 0, // will be set up during inserting to DB
    val text: String?,
    val decision: String?
) : DbEntity

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