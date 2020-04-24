package at.roteskreuz.stopcorona.skeleton.core.utils

import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

// Extensions for applying schedulers

// Observable

fun <T> Observable<T>.observeOnMainThread(): Observable<T> {
    return this.observeOn(AndroidSchedulers.mainThread())
}

fun <T> Observable<T>.subscribeOnIO(): Observable<T> {
    return this.subscribeOn(Schedulers.io())
}

fun <T> Observable<T>.subscribeOnNewThread(): Observable<T> {
    return this.subscribeOn(Schedulers.newThread())
}

fun <T> Observable<T>.subscribeOnComputation(): Observable<T> {
    return this.subscribeOn(Schedulers.computation())
}

// Completable

fun Completable.observeOnMainThread(): Completable {
    return this.observeOn(AndroidSchedulers.mainThread())
}

fun Completable.subscribeOnIO(): Completable {
    return this.subscribeOn(Schedulers.io())
}

fun Completable.subscribeOnNewThread(): Completable {
    return this.subscribeOn(Schedulers.newThread())
}

fun Completable.subscribeOnComputation(): Completable {
    return this.subscribeOn(Schedulers.computation())
}

// Maybe

fun <T> Maybe<T>.observeOnMainThread(): Maybe<T> {
    return this.observeOn(AndroidSchedulers.mainThread())
}

fun <T> Maybe<T>.subscribeOnIO(): Maybe<T> {
    return this.subscribeOn(Schedulers.io())
}

fun <T> Maybe<T>.subscribeOnNewThread(): Maybe<T> {
    return this.subscribeOn(Schedulers.newThread())
}

fun <T> Maybe<T>.subscribeOnComputation(): Maybe<T> {
    return this.subscribeOn(Schedulers.computation())
}

// Single

fun <T> Single<T>.observeOnMainThread(): Single<T> {
    return this.observeOn(AndroidSchedulers.mainThread())
}

fun <T> Single<T>.subscribeOnIO(): Single<T> {
    return this.subscribeOn(Schedulers.io())
}

fun <T> Single<T>.subscribeOnNewThread(): Single<T> {
    return this.subscribeOn(Schedulers.newThread())
}

fun <T> Single<T>.subscribeOnComputation(): Single<T> {
    return this.subscribeOn(Schedulers.computation())
}

// Flowable

fun <T> Flowable<T>.observeOnMainThread(): Flowable<T> {
    return this.observeOn(AndroidSchedulers.mainThread())
}

fun <T> Flowable<T>.subscribeOnIO(): Flowable<T> {
    return this.subscribeOn(Schedulers.io())
}

fun <T> Flowable<T>.subscribeOnNewThread(): Flowable<T> {
    return this.subscribeOn(Schedulers.newThread())
}

fun <T> Flowable<T>.subscribeOnComputation(): Flowable<T> {
    return this.subscribeOn(Schedulers.computation())
}
