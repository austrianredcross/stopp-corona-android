package at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel

import androidx.lifecycle.ViewModel
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * ViewModel with support for Coroutine Scope.
 */
open class ScopedViewModel(protected val appDispatchers: AppDispatchers) : ViewModel(), CoroutineScope {

    private val job = Job()

    protected var disposables = CompositeDisposable()

    /**
     * Default coroutine dispatcher is based on Main thread.
     * All coroutine task belongs to one [job].
     */
    override val coroutineContext: CoroutineContext = appDispatchers.Main + job

    override fun onCleared() {
        job.cancel()
        disposables.clear()
        super.onCleared()
    }
}