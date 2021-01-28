package at.roteskreuz.stopcorona.model.managers

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

/**
 * Manages mandatory update information.
 */
interface MandatoryUpdateManager {

    /**
     * Observe true if api error force update.
     */
    fun observeDisplayMandatoryUpdate(): Observable<Boolean>

    /**
     * Set the mandatory update should be displayed.
     */
    fun setDisplayMandatoryUpdate()

}

class MandatoryUpdateManagerImpl : MandatoryUpdateManager {

    private val shouldDisplayMandatoryUpdateSubject = BehaviorSubject.create<Boolean>()

    override fun setDisplayMandatoryUpdate() {
        shouldDisplayMandatoryUpdateSubject.onNext(true)
    }

    override fun observeDisplayMandatoryUpdate(): Observable<Boolean> {
        return shouldDisplayMandatoryUpdateSubject
    }
}

