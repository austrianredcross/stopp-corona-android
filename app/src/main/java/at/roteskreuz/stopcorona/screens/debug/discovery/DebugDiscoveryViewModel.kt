package at.roteskreuz.stopcorona.screens.debug.discovery

import at.roteskreuz.stopcorona.model.repositories.DiscoveryRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel

class DebugDiscoveryViewModel(
    appDispatchers: AppDispatchers,
    private val discoveryRepository: DiscoveryRepository
) : ScopedViewModel(appDispatchers) {


    fun observeP2PKitState() = discoveryRepository.observeP2PKitState()

    fun observeDiscoveryResult() = discoveryRepository.observeDiscoveryResult()
}
