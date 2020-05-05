package at.roteskreuz.stopcorona.model.repositories

import at.roteskreuz.stopcorona.constants.Constants.P2PDiscovery.APPLICATION_KEY
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import at.roteskreuz.stopcorona.utils.view.safeRun
import ch.uepaa.p2pkit.P2PKit
import ch.uepaa.p2pkit.P2PKitStatusListener
import ch.uepaa.p2pkit.StatusResult
import ch.uepaa.p2pkit.discovery.DiscoveryListener
import ch.uepaa.p2pkit.discovery.DiscoveryPowerMode
import ch.uepaa.p2pkit.discovery.Peer
import ch.uepaa.p2pkit.discovery.ProximityStrength
import com.github.dmstocking.optional.java.util.Optional
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

/**
 * Repository for managing incoming and outgoing p2p messages
 */
interface DiscoveryRepository {

    /**
     * Start P2P discovery (async).
     */
    fun start()

    /**
     * Stop P2P discovery (async).
     */
    fun stop()

    /**
     * Observes the state of the current discovery process.
     */
    fun observeP2PKitState(): Observable<Optional<P2PKitState>>

    /**
     * Observes the results and updates of the current discovery process.
     */
    fun observeDiscoveryResult(): Observable<DiscoveryResult>
}

class DiscoveryRepositoryImpl(
    private val appDispatchers: AppDispatchers,
    private val contextInteractor: ContextInteractor,
    private val cryptoRepository: CryptoRepository,
    private val dataPrivacyRepository: DataPrivacyRepository
) : DiscoveryRepository,
    CoroutineScope {

    private val p2pKitStateSubject = NonNullableBehaviorSubject<Optional<P2PKitState>>(Optional.ofNullable(null))
    private val discoveryResultSubject = BehaviorSubject.create<DiscoveryResult>()

    private val statusListener = object : P2PKitStatusListener {
        override fun onEnabled() {
            p2pKitStateSubject.onNext(Optional.of(P2PKitState.Enabled))
        }

        override fun onException(throwable: Throwable?) {
            p2pKitStateSubject.onNext(Optional.of(P2PKitState.Disabled.Exception(throwable)))
        }

        override fun onError(result: StatusResult?) {
            p2pKitStateSubject.onNext(Optional.of(P2PKitState.Disabled.Error(result)))
        }

        override fun onDisabled() {
            p2pKitStateSubject.onNext(Optional.of(P2PKitState.Disabled.Disabled))
            p2pKitStateSubject.onNext(Optional.ofNullable(null))
        }
    }

    private val discoveryListener = object : DiscoveryListener {
        private fun Peer?.safeRunWithParams(block: (Peer) -> Unit) {
            this?.safeRun(message = "peer is null") {
                peerId.safeRun(message = "peerId is null") {
                    discoveryInfo.safeRun(message = "discoveryInfo is null") {
                        proximityStrength.safeRun(message = "proximityStrength is null") {
                            block(this)
                        }
                    }
                }
            }
        }

        override fun onPeerUpdatedDiscoveryInfo(peer: Peer?) {
            peer.safeRunWithParams {
                discoveryResultSubject.onNext(DiscoveryResult.PeerUpdated(it.discoveryInfo, it.proximityStrength))
            }
        }

        override fun onPeerLost(peer: Peer?) {
            peer.safeRunWithParams {
                discoveryResultSubject.onNext(DiscoveryResult.PeerLost(it.discoveryInfo))
            }
        }

        override fun onStateChanged(state: Int) {
            discoveryResultSubject.onNext(DiscoveryResult.StateChanged(state))
        }

        override fun onProximityStrengthChanged(peer: Peer?) {
            peer.safeRunWithParams {
                // Ignore unknown proximity strength
                if (it.proximityStrength != ProximityStrength.UNKNOWN) {
                    discoveryResultSubject.onNext(DiscoveryResult.ProximityStrengthChanged(it.discoveryInfo, it.proximityStrength))
                } else {
                    Timber.d("Got unknown proximity strength")
                }
            }
        }

        override fun onPeerDiscovered(peer: Peer?) {
            peer.safeRunWithParams {
                discoveryResultSubject.onNext(DiscoveryResult.PeerDiscovered(it.discoveryInfo, it.proximityStrength))
                // Ignore unknown proximity strength
                if (it.proximityStrength != ProximityStrength.UNKNOWN) {
                    discoveryResultSubject.onNext(DiscoveryResult.ProximityStrengthChanged(it.discoveryInfo, it.proximityStrength))
                } else {
                    Timber.d("### Got unknown proximity strength")
                }
            }
        }
    }

    override val coroutineContext: CoroutineContext
        get() = appDispatchers.Default

    init {
        // We never want to dispose. This runs for all our life time
        val disposable = observeP2PKitState().filter { it.isPresent }.subscribe { p2pKitState ->
            when (p2pKitState.get()) {
                is P2PKitState.Enabled -> Timber.d("### P2PKit state chaged: Enabled (${cryptoRepository.publicKeyPrefix})\n")
                is P2PKitState.Disabled.Disabled -> Timber.d("### P2PKit state chaged: Disabled\n")
                is P2PKitState.Disabled.Error -> Timber.d("### P2PKit state chaged: Error\n")
                is P2PKitState.Disabled.Exception -> Timber.d("### P2PKit state chaged: Exception\n")
            }
        }
    }

    override fun start() {
        dataPrivacyRepository.assertDataPrivacyAccepted()
        launch {
            try {
                enableP2PKit()
                when (val p2pKitState = observeP2PKitState().filter { it.isPresent }.blockingFirst().get()) {
                    P2PKitState.Enabled -> {
                        startDiscovery()
                    }
                    P2PKitState.Disabled.Disabled -> {
                        Timber.e(SilentError("Cannot restart P2PKit because it's still disabled!"))
                    }
                    is P2PKitState.Disabled.Exception -> {
                        Timber.e(p2pKitState.throwable)
                    }
                    is P2PKitState.Disabled.Error -> {
                        Timber.e(p2pKitState.toString())
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    override fun stop() {
        launch {
            try {
                when (val discoveryState = observeP2PKitState().filter { it.isPresent }.blockingFirst().get()) {
                    P2PKitState.Enabled -> {
                        disableP2PKit()
                    }
                    P2PKitState.Disabled.Disabled -> {
                        Timber.e(SilentError("Cannot disable P2PKit because it's still disabled!"))
                    }
                    is P2PKitState.Disabled.Exception -> {
                        Timber.e(discoveryState.throwable)
                    }
                    is P2PKitState.Disabled.Error -> {
                        Timber.e(discoveryState.toString())
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    /**
     * Enables P2P discovery
     */
    private fun enableP2PKit() {
        P2PKit.enable(contextInteractor.applicationContext, APPLICATION_KEY, statusListener)
    }

    /**
     * Starts P2P discovery
     *
     * @throws P2PDiscoveryException.DiscoveryNotEnabled when discovery was not enabled before
     */
    private fun startDiscovery() {
        if (p2pKitStateSubject.value.orElse(null) == P2PKitState.Enabled) {
            P2PKit.enableProximityRanging()
            P2PKit.startDiscovery(cryptoRepository.publicKeyPKCS1, DiscoveryPowerMode.HIGH_PERFORMANCE, discoveryListener)
        } else {
            throw P2PDiscoveryException.DiscoveryNotEnabled
        }
    }

    /**
     * Disable P2P discovery
     *
     * @throws P2PDiscoveryException.DiscoveryNotEnabled when discovery was not enabled before
     */
    private fun disableP2PKit() {
        if (p2pKitStateSubject.value.orElse(null) == P2PKitState.Enabled) {
            P2PKit.disable()
        } else {
            throw P2PDiscoveryException.DiscoveryNotEnabled
        }
    }

    override fun observeP2PKitState() = p2pKitStateSubject

    override fun observeDiscoveryResult() = discoveryResultSubject
}

sealed class P2PKitState {

    object Enabled : P2PKitState()

    sealed class Disabled : P2PKitState() {

        object Disabled : P2PKitState.Disabled()
        data class Exception(val throwable: Throwable?) : P2PKitState.Disabled()
        data class Error(val result: StatusResult?) : P2PKitState.Disabled()
    }
}

sealed class DiscoveryResult {

    data class PeerUpdated(val discoveryInfo: ByteArray, val proximityStrength: Int) : DiscoveryResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PeerUpdated) return false

            if (!discoveryInfo.contentEquals(other.discoveryInfo)) return false
            if (proximityStrength != other.proximityStrength) return false

            return true
        }

        override fun hashCode(): Int {
            var result = discoveryInfo.contentHashCode()
            result = 31 * result + proximityStrength
            return result
        }
    }

    data class PeerLost(val discoveryInfo: ByteArray) : DiscoveryResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PeerLost) return false

            if (!discoveryInfo.contentEquals(other.discoveryInfo)) return false

            return true
        }

        override fun hashCode(): Int {
            return discoveryInfo.contentHashCode()
        }
    }

    data class StateChanged(val state: Int) : DiscoveryResult()
    data class ProximityStrengthChanged(val discoveryInfo: ByteArray, val proximityStrength: Int) : DiscoveryResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ProximityStrengthChanged) return false

            if (!discoveryInfo.contentEquals(other.discoveryInfo)) return false
            if (proximityStrength != other.proximityStrength) return false

            return true
        }

        override fun hashCode(): Int {
            var result = discoveryInfo.contentHashCode()
            result = 31 * result + proximityStrength
            return result
        }
    }

    data class PeerDiscovered(val discoveryInfo: ByteArray, val proximityStrength: Int) : DiscoveryResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PeerDiscovered) return false

            if (!discoveryInfo.contentEquals(other.discoveryInfo)) return false
            if (proximityStrength != other.proximityStrength) return false

            return true
        }

        override fun hashCode(): Int {
            var result = discoveryInfo.contentHashCode()
            result = 31 * result + proximityStrength
            return result
        }
    }
}

/**
 * Exceptions caused by invalid discovery states
 */
sealed class P2PDiscoveryException(override val message: String) : Exception(message) {

    object DiscoveryNotEnabled : P2PDiscoveryException("P2P discovery not enabled")
}
