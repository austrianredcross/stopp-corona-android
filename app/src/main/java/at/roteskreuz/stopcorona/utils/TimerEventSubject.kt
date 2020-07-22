package at.roteskreuz.stopcorona.utils

import at.roteskreuz.stopcorona.utils.TimerEventSubject.NextEventTime.Delay
import at.roteskreuz.stopcorona.utils.TimerEventSubject.NextEventTime.Never
import io.reactivex.Observable
import org.threeten.bp.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * An Observable that will emit a [Unit] at a previousely set time.
 *
 * The next emission can be set at any time by calling [setNextEvent].
 * A previousely scheduled event will be canceled if not already emitted.
 */
class TimerEventSubject : NonNullableBehaviorSubject<Unit>(Unit) {

    private val nextEventTimeSubject = NonNullableBehaviorSubject<NextEventTime>(Never)

    init {
        /**
         * Whenever [nextEventTimeSubject] emits a new event time, we want to reschedule emission.
         */
        nextEventTimeSubject.switchMap { nextEvent ->
            when (nextEvent) {
                Never -> {
                    Observable.empty<Unit>()
                }
                is Delay -> {
                    Observable.timer(nextEvent.delay, TimeUnit.MILLISECONDS).map { Unit }
                }
            }
        }.subscribe(this)
    }

    /**
     * Set time of next event.
     *
     * When [time] is in the past, the Observable will emit imediately.
     * When [time] is null the observable will not emit at all.
     * A previousely scheduled event will be canceled if not already emitted.
     *
     * @param time Time for next event. When [time] is null the observable will not emit at all.
     */
    fun setNextEvent(time: ZonedDateTime?) {
        val delay = time?.let {
            (time - ZonedDateTime.now()).toMillis()
        }
        setNextEvent(delay)
    }

    /**
     * Set delay until next event.
     *
     * When [delay] is negative, the Observable will emit imediately.
     * When [delay] is null the observable will not emit at all.
     * A previousely scheduled event will be canceled if not already emitted.
     *
     * @param delay Delay until next event.
     */
    fun setNextEvent(delay: Long?) {
        nextEventTimeSubject.onNext(
            when {
                delay != null -> Delay(delay.coerceAtLeast(0))
                else -> Never
            }
        )
    }

    sealed class NextEventTime {
        object Never : NextEventTime()
        class Delay(val delay: Long) : NextEventTime()
    }
}