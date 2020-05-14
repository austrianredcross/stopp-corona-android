package at.roteskreuz.stopcorona.model.repositories

import android.os.Bundle
import at.roteskreuz.stopcorona.constants.Constants.Nearby.IDENTIFICATION_BYTE_LENGTH
import at.roteskreuz.stopcorona.constants.Constants.Nearby.PUBLIC_KEY_LOOKUP_THRESHOLD_MINUTES
import at.roteskreuz.stopcorona.model.db.dao.NearbyRecordDao
import at.roteskreuz.stopcorona.model.entities.nearby.DbNearbyRecord
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.utils.asDbObservable
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.nearby.messages.*
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

/**
 * Repository for managing incoming and outgoing nearby messages
 */
interface NearbyRepository {

    /**
     * Random personal identification part of the content that is published in the message.
     */
    val personalIdentificationNumber: Int

    /**
     * Random personal identification part of the content that is published in the message.
     * Converted to human readable codeword.
     */
    val personalIdentificationCodeword: String

    /**
     * Message that will be broadcasted via publish process.
     */
    val message: Message

    /**
     * Options for outgoing publications.
     */
    val publishOptions: PublishOptions

    /**
     * Options for incoming subscriptions.
     */
    val subscribeOptions: SubscribeOptions

    /**
     * Listener for incoming messages.
     */
    val messageListener: MessageListener

    /**
     * Listener for connection state.
     */
    val connectionCallbacks: GoogleApiClient.ConnectionCallbacks

    /**
     * Listener for failed connections.
     */
    val connectionFailedListener: GoogleApiClient.OnConnectionFailedListener

    /**
     * Observes incoming messages.
     */
    fun observeMessages(): Observable<NearbyResult>

    /**
     * Observes the GoogleApiClient connection state.
     */
    fun observeConnection(): Observable<ApiConnectionState>

    /**
     * Observes the state of the current handshake progress.
     */
    fun observeHandshakeState(): Observable<NearbyHandshakeState>

    /**
     * Saves a given publicKey to the database.
     */
    suspend fun savePublicKey(publicKey: ByteArray, detectedAutomatically: Boolean)

    /**
     * Observe all the nearby records stored in the database.
     */
    fun observeAllNearbyRecords(): Observable<List<DbNearbyRecord>>
}

class NearbyRepositoryImpl(
    cryptoRepository: CryptoRepository,
    private val appDispatchers: AppDispatchers,
    private val nearbyRecordDao: NearbyRecordDao,
    private val handshakeCodewordRepository: HandshakeCodewordRepository
) : NearbyRepository, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = appDispatchers.Default

    private val messageSubject = BehaviorSubject.create<NearbyResult>()
    private val connectionSubject = BehaviorSubject.create<ApiConnectionState>()
    private val handshakeStateSubject = BehaviorSubject.create<NearbyHandshakeState>()

    override val personalIdentificationNumber: Int = handshakeCodewordRepository.identificationNumber
    override val personalIdentificationCodeword: String = handshakeCodewordRepository.getCodeword(
        personalIdentificationNumber
    )

    override val message: Message = Message(
        handshakeCodewordRepository
            .zeroPrefixed(personalIdentificationNumber)
            .toByteArray()
            .plus(cryptoRepository.publicKeyPKCS1)
    )

    override val publishOptions: PublishOptions
        get() = PublishOptions.Builder().apply {
            setStrategy(publishStrategy)
            setCallback(object : PublishCallback() {

                override fun onExpired() {
                    super.onExpired()
                    Timber.e("publish expired")
                    handshakeStateSubject.onNext(NearbyHandshakeState.Expired)
                }
            })
        }.build()

    override val subscribeOptions: SubscribeOptions
        get() = SubscribeOptions.Builder().apply {
            setStrategy(subscribeStrategy)
            setCallback(object : SubscribeCallback() {

                override fun onExpired() {
                    super.onExpired()
                    Timber.e("subscribe expired")
                    handshakeStateSubject.onNext(NearbyHandshakeState.Expired)
                }
            })
        }.build()

    override val messageListener: MessageListener = object : MessageListener() {

        override fun onFound(message: Message?) {
            super.onFound(message)

            message?.let {
                launch {
                    val identification = it.content.take(IDENTIFICATION_BYTE_LENGTH)
                        .map { byte -> byte.toChar() }
                        .joinToString(separator = "")
                        .let { value ->
                            try {
                                Integer.parseInt(value)
                            } catch (exc: NumberFormatException) {
                                Timber.e(SilentError("Invalid received id", exc))
                                return@launch // ignore the message
                            }
                        }

                    val publicKey = it.content.drop(IDENTIFICATION_BYTE_LENGTH).toByteArray()
                    val publicKeySavedInLast15Minutes =
                        nearbyRecordDao.wasRecordSavedInGivenPeriod(publicKey, ZonedDateTime.now().minusMinutes(PUBLIC_KEY_LOOKUP_THRESHOLD_MINUTES))

                    messageSubject.onNext(NearbyResult.Found(
                        identification = handshakeCodewordRepository.getCodeword(identification),
                        publicKey = publicKey,
                        selected = publicKeySavedInLast15Minutes,
                        saved = publicKeySavedInLast15Minutes))
                }
            }
        }
    }

    override val connectionCallbacks = object : GoogleApiClient.ConnectionCallbacks {
        override fun onConnected(p0: Bundle?) {
            connectionSubject.onNext(ApiConnectionState.Connected)
        }

        override fun onConnectionSuspended(p0: Int) {
            connectionSubject.onNext(ApiConnectionState.Suspended)
        }
    }

    override val connectionFailedListener = GoogleApiClient.OnConnectionFailedListener { connectionSubject.onNext(ApiConnectionState.Failed) }

    override fun observeMessages() = messageSubject

    override fun observeConnection() = connectionSubject

    override suspend fun savePublicKey(publicKey: ByteArray, detectedAutomatically: Boolean) {
        nearbyRecordDao.insert(publicKey, detectedAutomatically)
    }

    override fun observeHandshakeState(): Observable<NearbyHandshakeState> {
        return handshakeStateSubject
            .doOnSubscribe {
                handshakeStateSubject.onNext(NearbyHandshakeState.Active)
            }
    }

    override fun observeAllNearbyRecords(): Observable<List<DbNearbyRecord>> {
        return nearbyRecordDao.observeAllRecords().asDbObservable()
    }

    /**
     * Strategy settings for publishing messages
     */
    private val publishStrategy = Strategy.Builder()
        .setDistanceType(Strategy.DISTANCE_TYPE_EARSHOT)
        .setDiscoveryMode(Strategy.DISCOVERY_MODE_BROADCAST)
        .build()

    /**
     * Strategy settings for subscription of broadcasted messages
     */
    private val subscribeStrategy = Strategy.Builder()
        .setDistanceType(Strategy.DISTANCE_TYPE_EARSHOT)
        .setDiscoveryMode(Strategy.DISCOVERY_MODE_SCAN)
        .build()
}

sealed class NearbyResult(open val identification: String, open val publicKey: ByteArray, open var selected: Boolean, open var saved: Boolean) {

    data class Found(
        override val identification: String,
        override val publicKey: ByteArray,
        override var selected: Boolean,
        override var saved: Boolean
    ) : NearbyResult(identification, publicKey, selected, saved) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Found

            if (!publicKey.contentEquals(other.publicKey)) return false

            return true
        }

        override fun hashCode(): Int {
            return publicKey.contentHashCode()
        }
    }
}

sealed class ApiConnectionState {

    object Failed : ApiConnectionState()
    object Connected : ApiConnectionState()
    object Suspended : ApiConnectionState()
}

sealed class NearbyHandshakeState {

    object Active : NearbyHandshakeState()
    object Expired : NearbyHandshakeState()
    object SuccessfullyFinished : NearbyHandshakeState()
}
