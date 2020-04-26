package at.roteskreuz.stopcorona.screens.questionnaire

import android.util.SparseArray
import androidx.core.util.contains
import androidx.core.util.set
import at.roteskreuz.stopcorona.model.api.ApiInteractor
import at.roteskreuz.stopcorona.model.entities.configuration.ApiConfiguration
import at.roteskreuz.stopcorona.model.entities.configuration.Decision
import at.roteskreuz.stopcorona.model.repositories.QuestionnaireComplianceRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataState
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataStateObserver
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.launch

/**
 * Handles the user interaction and provides data for [OnboardingFragment].
 */
class QuestionnaireViewModel(
    appDispatchers: AppDispatchers,
    private val apiInteractor: ApiInteractor,
    questionnaireComplianceRepository: QuestionnaireComplianceRepository
) : ScopedViewModel(appDispatchers) {

    companion object {
        const val INDEX_LAST_PAGE = 4
    }

    private val currentPageSubject = NonNullableBehaviorSubject(0)
    private val decisionSubject = NonNullableBehaviorSubject(SparseArray<Decision>())
    private val executeDecisionSubject = BehaviorSubject.create<Decision>()
    private val questionnaireDataStateObserver = DataStateObserver<ApiConfiguration>()

    val complianceAccepted = questionnaireComplianceRepository.complianceAccepted

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

    private fun fetchQuestionnaire() {
        questionnaireDataStateObserver.loading()
        launch {
            try {
                val configuration = apiInteractor.getConfiguration()
                questionnaireDataStateObserver.loaded(configuration)
            } catch (ex: Exception) {
                questionnaireDataStateObserver.error(ex)
            } finally {
                questionnaireDataStateObserver.idle()
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

    fun observeDecision() = executeDecisionSubject

    fun observeQuestionnaireDataState(): Observable<DataState<ApiConfiguration>> {
        return questionnaireDataStateObserver.observe()
    }
}
