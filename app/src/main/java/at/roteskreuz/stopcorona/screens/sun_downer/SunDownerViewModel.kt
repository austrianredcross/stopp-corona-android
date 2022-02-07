package at.roteskreuz.stopcorona.screens.sun_downer

import at.roteskreuz.stopcorona.model.repositories.SunDownerRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import io.reactivex.Observable
import kotlin.math.min
import kotlin.math.max

class SunDownerViewModel (
    appDispatchers: AppDispatchers,
    private val sunDownerRepository: SunDownerRepository
) : ScopedViewModel(appDispatchers) {

    private val indexLastPage = 2

    private val currentPageSubject = NonNullableBehaviorSubject(0)

    var currentPage: Int
        get() = currentPageSubject.value
        set(value) = currentPageSubject.onNext(value)

    fun markSunDownerShown(){
        sunDownerRepository.setSunDownerShown()
    }

    fun observeLastPage(): Observable<Boolean> {
        return currentPageSubject.map { currentPage ->
            currentPage == indexLastPage
        }
    }

    fun isLastPage(): Boolean {
        return currentPage == indexLastPage
    }

    fun isFirstPage() = currentPage == 0

    fun getNextPage(): Int {
        return min(currentPage + 1, indexLastPage)
    }

    fun getPreviousPage(): Int {
        return max(currentPage - 1, 0)
    }

}