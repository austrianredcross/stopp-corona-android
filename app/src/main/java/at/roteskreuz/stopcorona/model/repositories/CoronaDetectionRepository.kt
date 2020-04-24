package at.roteskreuz.stopcorona.model.repositories

import android.content.SharedPreferences
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.constants.Constants.Domain.INTENSIVE_CONTACT_DETECTION_INTERVAL
import at.roteskreuz.stopcorona.constants.Constants.Domain.INTENSIVE_CONTACT_SCORE
import at.roteskreuz.stopcorona.constants.Constants.Domain.PROXIMITY_SCORE_WEIGHT
import at.roteskreuz.stopcorona.model.db.dao.AutomaticDiscoveryDao
import at.roteskreuz.stopcorona.model.entities.discovery.DbAutomaticDiscoveryEvent
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.receivers.Registrable
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor
import at.roteskreuz.stopcorona.model.services.CoronaDetectionService
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.utils.booleanSharedPreferencesProperty
import at.roteskreuz.stopcorona.skeleton.core.utils.observeBoolean
import at.roteskreuz.stopcorona.utils.asDbObservable
import ch.uepaa.p2pkit.discovery.DiscoveryListener
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoUnit
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * Repository for managing [CoronaDetectionService] logic.
 */
interface CoronaDetectionRepository {

    /**
     * Information if service is running or not.
     */
    var isServiceRunning: Boolean

    /**
     * Information if the automatic handshake was enabled automatically on the first start
     */
    var serviceEnabledOnFirstStart: Boolean

    /**
     * Observe information if service is running or not.
     */
    fun observeIsServiceRunning(): Observable<Boolean>

    /**
     * Start automatic detecting.
     * Register receivers to handle edge situations.
     */
    fun startListening()

    /**
     * Stop automatic detection.
     * Unregister receivers of edge situations.
     */
    fun stopListening()

    /**
     * Deletes old automatic discovered events.
     */
    suspend fun deleteOldEvents()
}

class CoronaDetectionRepositoryImpl(
    private val appDispatchers: AppDispatchers,
    private val contextInteractor: ContextInteractor,
    private val preferences: SharedPreferences,
    private val bluetoothStateReceiver: Registrable,
    private val batterySaverStateReceiver: Registrable,
    private val discoveryRepository: DiscoveryRepository,
    private val automaticDiscoveryDao: AutomaticDiscoveryDao,
    private val nearbyRepository: NearbyRepository,
    private val cryptoRepository: CryptoRepository
) : CoronaDetectionRepository,
    CoroutineScope {

    companion object {
        private const val PREF_IS_SERVICE_RUNNING = Constants.Prefs.CORONA_DETECTION_REPOSITORY_PREFIX + "is_service_running"
        private const val PREF_SERVICE_ENABLED_ON_FIRST_START = Constants.Prefs.CORONA_DETECTION_REPOSITORY_PREFIX + "service_enabled_on_first_start"

        private const val PROXIMITY_LOST = 0
    }

    private var disposables = CompositeDisposable()

    /**
     * Checking new discovery events and storing them into DB.
     */
    private val discoveryEventsObservable = discoveryRepository.observeDiscoveryResult()

        .doOnNext { event ->
            when (event) {
                is DiscoveryResult.PeerDiscovered -> {
                    saveEvent(event.discoveryInfo, event.proximityStrength)
                    Timber.d("### New event: ${cryptoRepository.getPublicKeyPrefix(event.discoveryInfo)}, Proximity: ${event.proximityStrength}")
                }
                is DiscoveryResult.ProximityStrengthChanged -> {
                    saveEvent(event.discoveryInfo, event.proximityStrength)
                    Timber.d("### Next event: ${cryptoRepository.getPublicKeyPrefix(event.discoveryInfo)}, Proximity: ${event.proximityStrength}")
                }
                is DiscoveryResult.PeerLost -> {
                    // Start event with null signal strength. This event will not count into the risk score
                    saveEvent(event.discoveryInfo, PROXIMITY_LOST)
                    Timber.d("### New event: ${cryptoRepository.getPublicKeyPrefix(event.discoveryInfo)}, LOST")
                }
                is DiscoveryResult.StateChanged -> {
                    Timber.d("### Discovery state changed: ${event.state}")
                    if (event.state == DiscoveryListener.STATE_OFF) {
                        // If we stopped scanning, we have to assume all peers are lost.
                        Timber.d("### Saving 'lost' events for all peers")
                        saveEventForAllPeers(PROXIMITY_LOST)
                    }
                }
            }
        }
        .ignoreResult()

    /**
     * Chain procedure to observe discovery events.
     * Each sequence with a high enough score is marked as handshake.
     * Events completely outside the detection window are deleted from DB
     */
    private val discoveryDbObservable: Observable<Unit> = Observable.interval(0, 1, TimeUnit.MINUTES)
        .doOnNext {
            Timber.d("### Timer tick")
        }
        .switchMap { automaticDiscoveryDao.observeAllEvents().asDbObservable() }
        .doOnNext {
            Timber.d("### Calculating scores")
        }
        .observeOn(Schedulers.computation())
        // group by user (public key)
        .map { events ->
            events.groupBy {
                // Byte arrays make no good hash keys
                it.publicKey.hexString
            }
        }
        // iterate over groups
        .flatMapIterable {
            it.asIterable()
        }
        // calculate score for public key
        .map { (_, events) ->
            events.first().publicKey to calculateTotalScore(events)
        }
        .doOnNext {
            Timber.d("### ${cryptoRepository.getPublicKeyPrefix(it.first)} Score: ${it.second}")
        }
        // filter intesive contacts
        .filter { (_, score) ->
            score >= INTENSIVE_CONTACT_SCORE
        }
        .map { it.first }
        // mark public keys as handshakes and store them to DB
        .updateHandshake()
        // remove old events from DB
        .cleanUp()
        .ignoreResult()

    private fun calculateTotalScore(
        events: List<DbAutomaticDiscoveryEvent>): Double {
        return events
            .map { calculateScore(it) }
            .sum()
    }

    override var isServiceRunning: Boolean by preferences.booleanSharedPreferencesProperty(PREF_IS_SERVICE_RUNNING, false)

    override var serviceEnabledOnFirstStart: Boolean by preferences.booleanSharedPreferencesProperty(PREF_SERVICE_ENABLED_ON_FIRST_START, false)

    init {
        isServiceRunning = CoronaDetectionService.isRunning(contextInteractor.applicationContext) // update the state
        if (isServiceRunning) {
            Timber.e(SilentError("Service should not be running at this time"))
        }

        // We do not know how we died the last time. We have to assume the whole DB is stale.
        launch {
            Timber.d("### Clearing database")
            automaticDiscoveryDao.deleteAllEvents()
        }
    }

    override fun observeIsServiceRunning(): Observable<Boolean> {
        return preferences.observeBoolean(PREF_IS_SERVICE_RUNNING, false)
    }

    override val coroutineContext: CoroutineContext
        get() = appDispatchers.Default

    override fun startListening() {
        with(contextInteractor.applicationContext) {
            bluetoothStateReceiver.register(this)
            batterySaverStateReceiver.register(this)
        }

        disposables += discoveryEventsObservable.subscribe()
        disposables += discoveryDbObservable.subscribe()

        Timber.d("### Started observing")

        discoveryRepository.start()
    }

    override fun stopListening() {
        discoveryRepository.stop()

        disposables.dispose()
        disposables = CompositeDisposable()

        Timber.d("### Stopped observing")

        with(contextInteractor.applicationContext) {
            bluetoothStateReceiver.unregister(this)
            batterySaverStateReceiver.unregister(this)
        }
    }

    override suspend fun deleteOldEvents() {
        automaticDiscoveryDao.deleteOldEvents(ZonedDateTime.now() - INTENSIVE_CONTACT_DETECTION_INTERVAL)
    }

    private fun detectionIntervalStart(now: ZonedDateTime): ZonedDateTime {
        return now - INTENSIVE_CONTACT_DETECTION_INTERVAL
    }

    private fun calculateScore(event: DbAutomaticDiscoveryEvent): Double {
        val now = ZonedDateTime.now()
        val detectionIntervalStart = detectionIntervalStart(now) // now - 1h

        // Ongoing events are counted as if they terminate now.
        val end = event.endTime ?: now

        // Events fully before the detection interval are ignored.
        if (end < detectionIntervalStart) return 0.0

        // Events that started before the detection interval are truncated to the interval.
        val start = if (event.startTime < detectionIntervalStart) detectionIntervalStart else event.startTime

        return ChronoUnit.SECONDS.between(start, end) / 60.0 * PROXIMITY_SCORE_WEIGHT[event.proximity]
    }

    /**
     * Save [DbAutomaticDiscoveryEvent] to DB.
     */
    private fun saveEvent(publicKey: ByteArray, proximity: Int) {
        launch {
            automaticDiscoveryDao.insertEventAndTerminatePrevious(
                publicKey = publicKey,
                proximity = proximity
            )
        }
    }

    /**
     * Save an [DbAutomaticDiscoveryEvent] for all known peers to DB.
     */
    private fun saveEventForAllPeers(proximity: Int) {
        launch {
            automaticDiscoveryDao.insertEventForAllPeersAndTerminatePrevious(
                proximity = proximity
            )
        }
    }

    /**
     * Update handshake for given public keys to DB.
     */
    private fun Observable<ByteArray>.updateHandshake(): Observable<ByteArray> {
        return doOnNext { publicKey ->
            launch {
                Timber.d("### Handshake with ${cryptoRepository.getPublicKeyPrefix(publicKey)}")
                nearbyRepository.savePublicKey(publicKey, detectedAutomatically = true)
            }
        }
    }

    /**
     * Delete old events from DB.
     */
    private fun Observable<ByteArray>.cleanUp(): Observable<ByteArray> {
        return doOnNext { _ ->
            launch {
                deleteOldEvents()
            }
        }
    }

    /**
     * Ignore result of the observable.
     * Useful to not propagate it to subscribe().
     */
    private fun <T> Observable<T>.ignoreResult(): Observable<Unit> {
        return map { Unit }
    }

    private val ByteArray.hexString: String
        get() = joinToString("") {
            String.format("%x00d", it)
        }
}