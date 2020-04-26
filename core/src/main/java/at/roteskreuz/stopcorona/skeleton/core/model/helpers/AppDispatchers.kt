package at.roteskreuz.stopcorona.skeleton.core.model.helpers

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

/**
 * Collection of coroutine dispatchers that can be overridden in Unit tests.
 */
interface AppDispatchers {

    @Suppress("PropertyName")
    val Default: CoroutineContext
    @Suppress("PropertyName")
    val Main: CoroutineContext
    @Suppress("PropertyName")
    val IO: CoroutineContext
}

class AppDispatchersImpl : AppDispatchers {
    override val Default: CoroutineContext
        get() = Dispatchers.Default
    override val Main: CoroutineContext
        get() = Dispatchers.Main
    override val IO: CoroutineContext
        get() = Dispatchers.IO
}