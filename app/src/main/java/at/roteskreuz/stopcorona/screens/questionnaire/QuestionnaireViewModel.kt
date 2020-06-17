package at.roteskreuz.stopcorona.screens.questionnaire

import android.util.SparseArray
import androidx.core.util.contains
import androidx.core.util.set
import at.roteskreuz.stopcorona.model.entities.configuration.ConfigurationLanguage
import at.roteskreuz.stopcorona.model.entities.configuration.DbQuestionnaireWithAnswers
import at.roteskreuz.stopcorona.model.entities.configuration.Decision
import at.roteskreuz.stopcorona.model.repositories.ConfigurationRepository
import at.roteskreuz.stopcorona.skeleton.core.model.exceptions.NoInternetConnectionException
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.StateObserver
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.launch

/**
 * Handles the user interaction and provides data for [QuestionnaireFragment].
 */
class QuestionnaireViewModel(
    appDispatchers: AppDispatchers,
    private val configurationRepository: ConfigurationRepository
) : ScopedViewModel(appDispatchers) {

    companion object {
        const val INDEX_LAST_PAGE = 4
    }

    private val currentPageSubject = NonNullableBehaviorSubject(0)
    private val decisionSubject = NonNullableBehaviorSubject(SparseArray<Decision>())
    private val executeDecisionSubject = BehaviorSubject.create<Decision>()
    private val fetchQuestionnaireStateObserver = StateObserver()
    private val questionnaireSubject = BehaviorSubject.create<List<DbQuestionnaireWithAnswers>>()

    var currentPage: Int
        get() = currentPageSubject.value
        set(value) = currentPageSubject.onNext(value)

    init {
        fetchQuestionnaire()
    }

    fun getNextPage(): Int {
        return if (currentPage < INDEX_LAST_PAGE) {
            currentPage + 1
        } else {
            currentPage
        }
    }

    fun getPreviousPage(): Int {
        return if (currentPage > 0) {
            currentPage - 1
        } else {
            currentPage
        }
    }

    fun isFirstPage() = currentPage == 0

    /**
     * Try to fetch fresh questionnaire, but in case of error, the data is already cached in DB,
     * so we can ignore no internet connection case.
     */
    private fun fetchQuestionnaire() {
        fetchQuestionnaireStateObserver.loading()
        launch {
            try {
                configurationRepository.fetchAndStoreConfiguration()
            } catch (e: NoInternetConnectionException) {
                // ignore, this is ok
            } catch (e: Exception) {
                fetchQuestionnaireStateObserver.error(e)
            } finally {
                fetchQuestionnaireStateObserver.idle()
            }
        }
    }

    fun observeLastPage(): Observable<Boolean> {
        return currentPageSubject.map { currentPage ->
            currentPage == INDEX_LAST_PAGE
        }
    }

    fun setDecision(pageNumber: Int, decision: Decision) {
        decisionSubject.value[pageNumber] = decision
        decisionSubject.onNext(decisionSubject.value)
    }

    fun observeButtonState(): Observable<Boolean> {
        return Observables.combineLatest(
            currentPageSubject,
            decisionSubject
        ).map { (currentPage, decisionMap) ->
            decisionMap.contains(currentPage)
        }
    }

    fun executeDecision() {
        executeDecisionSubject.onNext(decisionSubject.value.get(currentPage))
    }

    fun observeDecision(): Observable<Decision> = executeDecisionSubject

    fun observeFetchState(): Observable<State> = fetchQuestionnaireStateObserver.observe()

    fun observeQuestionnaireWithQuestions(): Observable<List<DbQuestionnaireWithAnswers>> = questionnaireSubject

    fun getQuestionnaireWithAnswers(language: ConfigurationLanguage) {
        launch {
            questionnaireSubject.onNext(configurationRepository.getQuestionnaireWithAnswers(language))
        }
    }
}
