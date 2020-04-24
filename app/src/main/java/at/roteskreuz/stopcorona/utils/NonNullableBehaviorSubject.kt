package at.roteskreuz.stopcorona.utils

import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

/**
 * Helper class with functionality same as [BehaviorSubject] with default non nullable value.
 */
class NonNullableBehaviorSubject<T : Any>(defaultValue: T) : Subject<T>() {

    private val behaviourSubject = BehaviorSubject.createDefault<T>(defaultValue)

    /**
     * Get non nullable value.
     */
    val value: T
        get() = behaviourSubject.value!! // has default value

    override fun hasThrowable(): Boolean {
        return behaviourSubject.hasThrowable()
    }

    override fun hasObservers(): Boolean {
        return behaviourSubject.hasObservers()
    }

    override fun onComplete() {
        return behaviourSubject.onComplete()
    }

    override fun onSubscribe(d: Disposable) {
        return behaviourSubject.onSubscribe(d)
    }

    override fun onError(e: Throwable) {
        return behaviourSubject.onError(e)
    }

    override fun getThrowable(): Throwable? {
        return behaviourSubject.throwable
    }

    override fun subscribeActual(observer: Observer<in T>) {
        behaviourSubject.subscribe(observer)
    }

    override fun onNext(t: T) {
        return behaviourSubject.onNext(t)
    }

    override fun hasComplete(): Boolean {
        return behaviourSubject.hasComplete()
    }
}