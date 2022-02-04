package at.roteskreuz.stopcorona.model.repositories

import android.util.Log
import at.roteskreuz.stopcorona.model.api.ApiInteractor
import at.roteskreuz.stopcorona.model.entities.statistics.Bundesland
import at.roteskreuz.stopcorona.model.entities.statistics.CovidStatistics
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.BehaviourSubjectObservable
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

/**
 * Repository for managing statistics.
 */
interface StatisticsRepository {

    /**
     * Set the state from state picker
     */
    fun setSelectedState(state: Bundesland)

    /**
     * Observes the state from state picker
     */
    fun observeSelectedState(): Observable<Bundesland>

    /**
     * Observes statistics fetch
     */
    fun observeStatistics(): Observable<CovidStatistics>
}
class StatisticsRepositoryImpl(
    val appDispatchers: AppDispatchers,
    private val apiInteractor: ApiInteractor
) : StatisticsRepository,
    CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = appDispatchers.Default

    init {
        fetchStatistics()
    }

    private val selectedStateSubject = NonNullableBehaviorSubject(Bundesland.Oesterreich)
    private val statisticsSubject = BehaviorSubject.create<CovidStatistics>()

    override fun setSelectedState(state: Bundesland) {
        selectedStateSubject.onNext(state)
    }

    override fun observeSelectedState(): Observable<Bundesland> {
        return selectedStateSubject
    }

    override fun observeStatistics(): Observable<CovidStatistics> {
        return statisticsSubject
    }

    private fun fetchStatistics() {
        launch {
            val statistics = apiInteractor.downloadCovidStatistics()
            statisticsSubject.onNext(statistics)
        }
    }
}