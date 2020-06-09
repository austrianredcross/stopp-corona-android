package at.roteskreuz.stopcorona.utils

import io.reactivex.Observable

/**
 * Helper functions for rx streams.
 */

/**
 * Share the observable with multiple subscribers so the source is evaluated only once.
 * Every new subscriber will get as an initial value the last value from the stream.
 * Once all subscribers are disposed, whole the stream is closed.
 */
fun <T> Observable<T>.shareReplayLast(): Observable<T> {
    return replay(1).refCount()
}