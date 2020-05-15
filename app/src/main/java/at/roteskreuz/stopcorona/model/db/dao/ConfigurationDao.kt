package at.roteskreuz.stopcorona.model.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import at.roteskreuz.stopcorona.model.entities.configuration.*
import at.roteskreuz.stopcorona.model.entities.configuration.ConfigurationLanguage.*
import io.reactivex.Flowable
import org.threeten.bp.ZonedDateTime

/**
 * Dao to manage [DbConfiguration].
 */
@Dao
abstract class ConfigurationDao {

    @Query("SELECT COUNT(*) = 0 FROM configuration LIMIT 1")
    abstract suspend fun isConfigurationEmpty(): Boolean

    @Insert
    protected abstract suspend fun insertConfiguration(configuration: DbConfiguration): Long

    @Insert
    protected abstract suspend fun insertQuestionnaire(questionnaire: DbQuestionnaire): Long

    @Insert
    protected abstract suspend fun insertQuestionnaireAnswer(questionnaireAnswer: DbQuestionnaireAnswer): Long

    @Insert
    protected abstract suspend fun insertPageContent(pageContent: DbPageContent): Long

    @Query("DELETE FROM configuration")
    protected abstract suspend fun deleteConfiguration(): Int

    @Transaction
    open suspend fun updateConfiguration(apiConfiguration: ApiConfiguration) {
        suspend fun List<ApiQuestionnaire>.insert(configurationId: Long, language: ConfigurationLanguage) {
            map { apiQuestionnaire ->
                val dbQuestionnaire = apiQuestionnaire.asDbEntity()
                dbQuestionnaire.configurationId = configurationId
                dbQuestionnaire.language = language
                val questionnaireId = insertQuestionnaire(dbQuestionnaire)
                apiQuestionnaire.answers?.map { apiQuestionnaireAnswer ->
                    val dbQuestionnaireAnswer = apiQuestionnaireAnswer.asDbEntity()
                    dbQuestionnaireAnswer.questionnaireId = questionnaireId
                    insertQuestionnaireAnswer(dbQuestionnaireAnswer)
                }
            }
        }

        suspend fun ApiPageContent.insert(configurationId: Long, language: ConfigurationLanguage) {
            val dbPageContent = asDbEntity()
            dbPageContent.configurationId = configurationId
            dbPageContent.language = language
            insertPageContent(dbPageContent)
        }

        deleteConfiguration()
        val dbConfiguration = apiConfiguration.asDbEntity()
        val configurationId = insertConfiguration(dbConfiguration)
        apiConfiguration.diagnosticQuestionnaire?.run {
            cz?.insert(configurationId, CZ)
            de?.insert(configurationId, DE)
            en?.insert(configurationId, EN)
            fr?.insert(configurationId, FR)
            hu?.insert(configurationId, HU)
            sk?.insert(configurationId, SK)
        }
        apiConfiguration.pageList?.run {
            cz?.insert(configurationId, CZ)
            de?.insert(configurationId, DE)
            en?.insert(configurationId, EN)
            fr?.insert(configurationId, FR)
            hu?.insert(configurationId, HU)
            sk?.insert(configurationId, SK)
        }
    }

    @Query("SELECT cacheTime FROM configuration")
    abstract suspend fun getCacheTime(): ZonedDateTime?

    @Transaction
    @Query("SELECT * FROM configuration")
    abstract fun observeConfiguration(): Flowable<DbConfiguration>

    @Transaction
    @Query("""
        SELECT * 
        FROM configuration_questionnaire AS question
        INNER JOIN configuration_questionnaire_answer AS answer
        ON (question.id = answer.questionnaireId)
        WHERE question.language = :language
    """)
    abstract fun observeQuestionnaireWithAnswers(language: ConfigurationLanguage): Flowable<List<DbQuestionnaireWithAnswers>>
}