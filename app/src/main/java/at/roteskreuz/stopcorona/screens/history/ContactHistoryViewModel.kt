package at.roteskreuz.stopcorona.screens.history

import at.roteskreuz.stopcorona.model.entities.nearby.DbNearbyRecord
import at.roteskreuz.stopcorona.model.repositories.NearbyRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import io.reactivex.Observable

/**
 * Handles the user interaction and provides data for [ContactHistoryFragment].
 */
class ContactHistoryViewModel(
    appDispatchers: AppDispatchers,
    private val nearbyRepository: NearbyRepository
) : ScopedViewModel(appDispatchers) {

    fun observeAllNearbyRecords(): Observable<List<NearbyRecordWrapper>> {
        return nearbyRepository.observeAllNearbyRecords().map { nearbyRecordsList ->
            nearbyRecordsList
                .sortedBy { it.timestamp }
                .mapIndexed { index, nearby ->
                    NearbyRecordWrapper(nearby, index)
                }
                .sortedByDescending { it.index }
        }
    }

}

data class NearbyRecordWrapper(val record: DbNearbyRecord, val index: Int)

