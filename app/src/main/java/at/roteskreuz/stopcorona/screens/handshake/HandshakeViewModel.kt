package at.roteskreuz.stopcorona.screens.handshake

import androidx.fragment.app.FragmentActivity
import at.roteskreuz.stopcorona.constants.Constants.Nearby.LOADING_INDICATOR_DELAY_MILLIS
import at.roteskreuz.stopcorona.model.repositories.NearbyHandshakeState
import at.roteskreuz.stopcorona.model.repositories.NearbyRepository
import at.roteskreuz.stopcorona.model.repositories.NearbyResult
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.messages.MessagesClient
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Handles contact discovery and handshakes between devices
 */
class HandshakeViewModel(
    appDispatchers: AppDispatchers,
    private val googleApiClientBuilder: GoogleApiClient.Builder,
    private val nearbyRepository: NearbyRepository
) : ScopedViewModel(appDispatchers) {

    val personalIdentification = nearbyRepository.personalIdentification

    private var messagesClient: MessagesClient? = null
    private var googleApiClient: GoogleApiClient? = null
    private var messagesDisposable: Disposable? = null
    private var loadingIndicatorDisposable: Disposable? = null
    private val contactSubject = NonNullableBehaviorSubject(ArrayList<NearbyResult>())
    private val loadingIndicatorSubject: NonNullableBehaviorSubject<LoadingIndicatorState> =
        NonNullableBehaviorSubject(LoadingIndicatorState.Invisible)

    fun observeContacts() = contactSubject
    fun observeConnection() = nearbyRepository.observeConnection()
    fun observeLoadingIndicator() = loadingIndicatorSubject

    fun observeHandshakeState(): Observable<NearbyHandshakeState> {
        return nearbyRepository.observeHandshakeState()
            .map { state ->
                if (state == NearbyHandshakeState.Active) {
                    NearbyHandshakeState.Active
                } else {
                    if (contactSubject.value.isEmpty()) {
                        loadingIndicatorSubject.onNext(LoadingIndicatorState.Invisible)
                        NearbyHandshakeState.Expired
                    } else {
                        NearbyHandshakeState.SuccessfullyFinished
                    }
                }
            }
    }

    fun observeSelectAllButtonState(): Observable<SelectAllButtonState> {
        return contactSubject
            .map { it.filter { result -> result.selected } }
            .map { selectedResults ->
                if (selectedResults.size == contactSubject.value.size) {
                    SelectAllButtonState.Checked
                } else {
                    SelectAllButtonState.Unchecked
                }
            }
    }

    fun observeSaveButtonState(): Observable<SaveButtonState> {
        return contactSubject
            .map { it.filter { result -> result.selected && result.saved.not() } }
            .map { selectedResults ->
                when {
                    selectedResults.isEmpty() -> SaveButtonState.Disabled
                    else -> SaveButtonState.Enabled
                }
            }
    }

    private fun startLoadingIndicatorObservation() {
        loadingIndicatorDisposable = contactSubject.doOnSubscribe {
            Observable.timer(LOADING_INDICATOR_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                .subscribe {
                    loadingIndicatorSubject.onNext(
                        when {
                            contactSubject.value.isEmpty() -> LoadingIndicatorState.Visible
                            else -> LoadingIndicatorState.Invisible
                        }
                    )
                }
        }.map { list ->
            if (list.isNotEmpty()) {
                loadingIndicatorSubject.onNext(LoadingIndicatorState.Invisible)
            }
        }.subscribe()

        disposables += loadingIndicatorDisposable!!
    }

    private fun startResultObservation() {
        messagesDisposable = nearbyRepository.observeMessages()
            .filter { result -> result is NearbyResult.Found }
            .map { result ->
                with(contactSubject.value) {
                    if (contains(result)) {
                        val indexOf = indexOf(result)
                        result.selected = get(indexOf).selected
                        this[indexOf] = result
                    } else {
                        add(result)
                    }
                }

                contactSubject.onNext(contactSubject.value)
            }
            .subscribe()

        disposables += messagesDisposable!!
    }

    fun stopConnection() {
        if (googleApiClient?.isConnected == true) {
            messagesClient?.unpublish(nearbyRepository.message)
            messagesClient?.unsubscribe(nearbyRepository.messageListener)

            messagesDisposable?.dispose()
            messagesDisposable = null

            loadingIndicatorDisposable?.dispose()
            loadingIndicatorDisposable = null
        }
    }

    private fun publish() {
        messagesClient?.publish(nearbyRepository.message, nearbyRepository.publishOptions)
            ?.addOnSuccessListener {
                Timber.e("publish success")
            }
            ?.addOnCanceledListener {
                Timber.e("publish canceled")
            }
            ?.addOnFailureListener {
                Timber.e("publish failed: $it")
            }
    }

    private fun subscribe() {
        messagesClient?.subscribe(nearbyRepository.messageListener, nearbyRepository.subscribeOptions)
            ?.addOnSuccessListener {
                Timber.e("subscribe success")
            }
            ?.addOnCanceledListener {
                Timber.e("subscribe canceled")
            }
            ?.addOnFailureListener {
                Timber.e("subscribe failed: $it")
            }
    }

    fun startConnection(messagesClient: MessagesClient) {
        if (googleApiClient?.isConnected == true) {
            this.messagesClient = messagesClient
            publish()
            subscribe()

            startResultObservation()
            startLoadingIndicatorObservation()
        }
    }

    fun resume(activity: FragmentActivity) {
        googleApiClient = googleApiClientBuilder.addApi(Nearby.MESSAGES_API)
            .addConnectionCallbacks(nearbyRepository.connectionCallbacks)
            .enableAutoManage(activity, nearbyRepository.connectionFailedListener)
            .build()
    }

    fun pause(activity: FragmentActivity) {
        googleApiClient?.stopAutoManage(activity)
        googleApiClient?.disconnect()
    }

    fun selectContact(contactSelected: Boolean, result: NearbyResult) {
        with(contactSubject.value) {
            val indexOf = indexOf(result)
            get(indexOf).selected = contactSelected
            contactSubject.onNext(this)
        }
    }

    fun selectAllContacts(selected: Boolean) {
        with(contactSubject.value) {
            filter { result -> result.saved.not() }
                .forEach { result ->
                    result.selected = selected
                }
            contactSubject.onNext(this)
        }
    }

    fun saveSelectedContacts() {
        launch {
            with(contactSubject.value) {
                filter { result -> result.selected && result.saved.not() }
                    .forEach { result ->
                        nearbyRepository.savePublicKey(result.publicKey, detectedAutomatically = false)
                        result.saved = true
                    }
                contactSubject.onNext(this)
            }
        }
    }

    fun retry() {
        publish()
        subscribe()

        startResultObservation()
        startLoadingIndicatorObservation()
    }

    fun permissionDenied() {
        messagesClient = null
        googleApiClient = null
    }
}

sealed class SaveButtonState {

    object Enabled : SaveButtonState()
    object Disabled : SaveButtonState()
}

sealed class SelectAllButtonState {

    object Checked : SelectAllButtonState()
    object Unchecked : SelectAllButtonState()
}

sealed class LoadingIndicatorState {
    object Visible : LoadingIndicatorState()
    object Invisible : LoadingIndicatorState()
}
