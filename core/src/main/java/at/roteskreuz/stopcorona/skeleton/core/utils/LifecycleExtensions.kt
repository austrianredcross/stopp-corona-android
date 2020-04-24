package at.roteskreuz.stopcorona.skeleton.core.utils

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer

/**
 * Extensions for [LifecycleOwner].
 */

/**
 * Do once an action when fragment view is ready. It can be called whenever the fragment lifecycle is, because
 * there is used viewLifecycle which will send first emit after the view is ready.
 */
fun Fragment.onViewReady(onViewCreated: () -> Unit) {
    viewLifecycleOwnerLiveData.observe(this, object : Observer<LifecycleOwner?> {
        override fun onChanged(t: LifecycleOwner?) {
            onViewCreated()
            viewLifecycleOwnerLiveData.removeObserver(this)
        }
    })
}

/**
 * Do once an action when lifecycle (activity/fragment) owner is created.
 */
fun LifecycleOwner.onCreated(action: () -> Unit) = onEvent(Lifecycle.Event.ON_CREATE, action)

/**
 * Do once an action when lifecycle (activity/fragment) owner is started.
 */
fun LifecycleOwner.onStarted(action: () -> Unit) = onEvent(Lifecycle.Event.ON_START, action)

/**
 * Do once an action when lifecycle (activity/fragment) owner is resumed.
 */
fun LifecycleOwner.onResumed(action: () -> Unit) = onEvent(Lifecycle.Event.ON_RESUME, action)

/**
 * Do once an action when lifecycle (activity/fragment) owner is paused.
 */
fun LifecycleOwner.onPaused(action: () -> Unit) = onEvent(Lifecycle.Event.ON_PAUSE, action)

/**
 * Do once an action when lifecycle (activity/fragment) owner is stopped.
 */
fun LifecycleOwner.onStopped(action: () -> Unit) = onEvent(Lifecycle.Event.ON_STOP, action)

/**
 * Do once an action when lifecycle (activity/fragment) owner is destroyed.
 */
fun LifecycleOwner.onDestroyed(action: () -> Unit) = onEvent(Lifecycle.Event.ON_DESTROY, action)

private fun LifecycleOwner.onEvent(event: Lifecycle.Event, action: () -> Unit) {
    lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, _event: Lifecycle.Event) {
            if (_event == event) {
                action()
                source.lifecycle.removeObserver(this)
            }
        }
    })
}