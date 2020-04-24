package at.roteskreuz.stopcorona.skeleton.core.model.scope

import io.reactivex.disposables.CompositeDisposable
import org.koin.core.scope.ScopeCallback
import org.koin.standalone.KoinComponent
import org.koin.standalone.StandAloneContext

/**
 * Scope lifecycle checker.
 */
abstract class Scope(scopeName: String) : KoinComponent {

    protected var disposables = CompositeDisposable()

    init {
        StandAloneContext.registerScopeCallback(object : ScopeCallback {
            override fun onClose(id: String, uuid: String) {
                if (id == scopeName) {
                    onClose()
                }
            }
        })
    }

    /**
     * Called when scope is destroyed.
     */
    open fun onClose() {
        disposables.clear()
    }
}