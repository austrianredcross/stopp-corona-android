package at.roteskreuz.stopcorona.skeleton.core.model.helpers

import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

/**
 * The wrapper for all observables which stores the last value inside and
 * can provide it using [currentData] similar to the [BehaviorSubject].
 * It is useful for handling screen rotation with using epoxy controller.
 * The [RecyclerView] automatically stores the current view state (including
 * scroll state) but after screen rotation the epoxy controller is
 * recreated and it is empty.
 * It is needed to set up the data to the controller before the view
 * is rendered.
 *
 * Here is recommended usage:
 *
 * In ViewModel:
 * ```
 * private val dataObserver = BehaviourSubjectObservable { observeDataFromRepository() }
 *
 * fun observeData(): Observable<Data> = dataObserver
 * fun getLastData(): Data? = dataObserver.currentData
 * ```
 *
 * In Fragment:
 * ```
 * override fun onCreate(savedInstanceState: Bundle?) {
 *   super.onCreate(savedInstanceState)
 *
 *   if (savedInstanceState != null) {
 *     dataViewModel.getLastData()?.let { data ->
 *       controller.setData(data)
 *     }
 *   }
 * }
 *
 * override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *   super.onViewCreated(view, savedInstanceState)
 *
 *   disposables += dataViewModel.observeData()
 *     .observeOnMainThread()
 *     .skipIf { savedInstanceState != null }
 *     .subscribe { data ->
 *       controller.setData(data)
 *     }
 *
 *  ```
 */
class BehaviourSubjectObservable<T>(
    private val initialObservable: () -> Observable<T>
) : Observable<T>(), Disposable {

    /**
     * The source of the data.
     */
    private val upstream by lazy { initialObservable() }
    /**
     * The output using [BehaviorSubject].
     */
    private val downstream = BehaviorSubject.create<T>()
    private var dataDisposable: Disposable? = null

    override fun subscribeActual(observer: Observer<in T>) {
        if (dataDisposable == null) {
            dataDisposable = upstream.subscribe(downstream::onNext, downstream::onError)
        }
        downstream.subscribe(observer)
    }

    override fun isDisposed(): Boolean {
        return dataDisposable != null && dataDisposable?.isDisposed == true
    }

    override fun dispose() {
        dataDisposable?.dispose()
        dataDisposable = null
    }

    val currentData: T?
        get() = downstream.value
}

/**
 * Skip [numberOfSkips] emits if [condition] is satisfied.
 * @param numberOfSkips The default value is 1.
 */
fun <T> Observable<T>.skipIf(numberOfSkips: Long = 1, condition: () -> Boolean): Observable<T> {
    return if (condition()) skip(numberOfSkips) else this
}