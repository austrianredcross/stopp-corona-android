package at.roteskreuz.stopcorona.screens.onboarding

import at.roteskreuz.stopcorona.model.repositories.DataPrivacyRepository
import at.roteskreuz.stopcorona.model.repositories.OnboardingRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables

/**
 * Handles the user interaction and provides data for [OnboardingFragment].
 */
class OnboardingViewModel(
    appDispatchers: AppDispatchers,
    private val onboardingRepository: OnboardingRepository,
    private val dataPrivacyRepository: DataPrivacyRepository
) : ScopedViewModel(appDispatchers) {

    companion object {
        const val INDEX_LAST_PAGE = 5
        const val INDEX_LAST_PAGE_WITH_TERMS_AND_CONDITIONS = INDEX_LAST_PAGE + 1
    }

    private val lastPage
        get() = if (dataPrivacyRepository.dataPrivacyAccepted) {
            INDEX_LAST_PAGE
        } else {
            INDEX_LAST_PAGE_WITH_TERMS_AND_CONDITIONS
        }

    var currentPage: Int
        get() = currentPageSubject.value
        set(value) = currentPageSubject.onNext(value)

    val dataPrivacyAccepted = dataPrivacyRepository.dataPrivacyAccepted

    private val currentPageSubject = NonNullableBehaviorSubject(0)
    private val dataPrivacyCheckedSubject = NonNullableBehaviorSubject(false)

    val dataPrivacyChecked
        get() = dataPrivacyCheckedSubject.value

    fun getNextPage(): Int {
        return if (currentPage < lastPage) {
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

    fun onboardingFinished() {
        onboardingRepository.onboardingFinished()

        if (currentPage == INDEX_LAST_PAGE_WITH_TERMS_AND_CONDITIONS && dataPrivacyCheckedSubject.value) {
            dataPrivacyRepository.setDataPrivacyAccepted()
            dataPrivacyRepository.setNewDataPrivacyAccepted()
        }
    }

    fun isLastPage(pageNumber: Int): Boolean {
        return pageNumber == lastPage
    }

    fun setDataPrivacyChecked(isChecked: Boolean) {
        dataPrivacyCheckedSubject.onNext(isChecked)
    }

    fun observeButtonEnabledState(): Observable<Boolean> {
        return Observables.combineLatest(
            currentPageSubject,
            dataPrivacyCheckedSubject
        ).map { (currentPage, termsAndConditionsChecked) ->
            when (currentPage == INDEX_LAST_PAGE_WITH_TERMS_AND_CONDITIONS) {
                true -> termsAndConditionsChecked
                else -> true
            }
        }
    }

    fun observeDataPrivacyPageShown(): Observable<Boolean> {
        return currentPageSubject.map { page ->
            when (page) {
                INDEX_LAST_PAGE_WITH_TERMS_AND_CONDITIONS -> true
                else -> false
            }
        }
    }

    fun observeLastPage(): Observable<Boolean> {
        return currentPageSubject.map { currentPage ->
            currentPage == lastPage
        }
    }

    fun isFirstPage() = currentPage == 0
}
