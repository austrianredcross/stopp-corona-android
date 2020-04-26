package at.roteskreuz.stopcorona.skeleton.core.model.helpers

import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject

/**
 * State observer which can propagate [State] states.
 */
class StateObserver : BaseStateObserver<Nothing, State>()

/**
 * State observer which can propagate [State] and [DataState] added using [loaded] method.
 */
class DataStateObserver<T>(default: DataState<T>? = State.Idle) : BaseStateObserver<T, DataState<T>>() {

    override val stateSubject: BehaviorSubject<DataState<T>> = if (default == null) {
        BehaviorSubject.create()
    } else {
        BehaviorSubject.createDefault(default)
    }

    fun loaded(data: T) {
        stateSubject.onNext(DataState.Loaded(data))
    }
}

/**
 * Base generic definition of state observer.
 */
@Suppress("UNCHECKED_CAST")
abstract class BaseStateObserver<E, T : DataState<E>> {

    protected open val stateSubject: BehaviorSubject<T> = BehaviorSubject.createDefault(State.Idle as T)

    val currentState: T?
        get() = stateSubject.value

    fun idle() {
        stateSubject.onNext(State.Idle as T)
    }

    fun loading() {
        stateSubject.onNext(State.Loading as T)
    }

    fun error(err: Throwable) {
        stateSubject.onNext(State.Error(err) as T)
    }

    fun observe(): Observable<T> {
        return stateSubject
    }
}

/**
 * State holder acquiring states Idle, Loading or Error.
 */
sealed class State : DataState<Nothing>() {

    /**
     * Nothing to do.
     */
    object Idle : State()

    /**
     * Data are loading.
     */
    object Loading : State()

    /**
     * Some error has happened.
     */
    data class Error(val error: Throwable) : State()
}

/**
 * State data holder which includes also [State] states.
 */
sealed class DataState<out T> {

    /**
     * Some data are loaded.
     */
    data class Loaded<out T>(val data: T) : DataState<T>()
}

/**
 * Returns observable only with data part.
 */
fun <T> Observable<DataState<T>>.filterData(): Observable<T> {
    return filter { it is DataState.Loaded }.map { (it as DataState.Loaded).data }
}

/**
 * Returns observable with only state part (idle, loading, error).
 */
fun <T> Observable<DataState<T>>.filterNotData(): Observable<State> {
    return filter { it !is DataState.Loaded }.map { it as State }
}

/**
 * Helpers for [StateObserver]s to combine them into one result.
 */
object StateObservables {

    /**
     * Get single result of two states.
     * - if any error -> error
     * - else if any loading -> loading
     * - else everything is ok -> idle
     */
    private fun State.combineWith(other: State): State {
        return when {
            this is State.Error -> State.Error(this.error)
            other is State.Error -> State.Error(other.error)
            this is State.Loading || other is State.Loading -> State.Loading
            else -> State.Idle
        }
    }

    /**
     * Get combined result as state observable.
     * - if any error -> error
     * - else if any loading -> loading
     * - else everything is ok -> idle
     */
    fun combineLatest(
        stateObservable1: Observable<State>,
        stateObservable2: Observable<State>
    ): Observable<State> {
        return Observables.combineLatest(
            stateObservable1,
            stateObservable2
        ).map { (state1, state2) ->
            state1.combineWith(state2)
        }
    }

    /**
     * Get combined result as state observable.
     * - if any error -> error
     * - else if any loading -> loading
     * - else everything is ok -> idle
     */
    fun combineLatest(
        stateObservable1: Observable<State>,
        stateObservable2: Observable<State>,
        stateObservable3: Observable<State>
    ): Observable<State> {
        return Observables.combineLatest(
            stateObservable1,
            stateObservable2,
            stateObservable3
        ).map { (state1, state2, state3) ->
            state1.combineWith(state2).combineWith(state3)
        }
    }

    /**
     * Get combined result as state observable.
     * - if any error -> error
     * - else if any loading -> loading
     * - else everything is ok -> idle
     */
    fun combineLatest(
        stateObservable1: Observable<State>,
        stateObservable2: Observable<State>,
        stateObservable3: Observable<State>,
        stateObservable4: Observable<State>
    ): Observable<State> {
        return Observables.combineLatest(
            stateObservable1,
            stateObservable2,
            stateObservable3,
            stateObservable4
        ) { state1, state2, state3, state4 ->
            state1.combineWith(state2).combineWith(state3).combineWith(state4)
        }
    }

    /**
     * Get combined result as state observable.
     * - if any error -> error
     * - else if any loading -> loading
     * - else everything is ok -> idle
     */
    fun combineLatest(
        stateObservable1: Observable<State>,
        stateObservable2: Observable<State>,
        stateObservable3: Observable<State>,
        stateObservable4: Observable<State>,
        stateObservable5: Observable<State>
    ): Observable<State> {
        return Observables.combineLatest(
            stateObservable1,
            stateObservable2,
            stateObservable3,
            stateObservable4,
            stateObservable5
        ) { state1, state2, state3, state4, state5 ->
            state1.combineWith(state2).combineWith(state3).combineWith(state4).combineWith(state5)
        }
    }
}