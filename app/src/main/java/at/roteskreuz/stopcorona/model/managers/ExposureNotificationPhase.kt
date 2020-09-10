package at.roteskreuz.stopcorona.model.managers

import at.roteskreuz.stopcorona.commonexposure.CommonExposureClient
import at.roteskreuz.stopcorona.commonexposure.ExposureServiceStatus
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.repositories.BluetoothRepository
import at.roteskreuz.stopcorona.model.repositories.ExposureNotificationRepository
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor
import at.roteskreuz.stopcorona.model.toErrorPhaseOrNull
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.CancellationException

/**
 * State machine to manage state of the exposure notification framework and checking all dependencies
 * needed for running.
 */
// TODO: Break-up god-class and move GMS- and HMS-only logic into corresponding platform-specific module
sealed class ExposureNotificationPhase : ExposureServiceStatus {

    data class DependencyHolder(
        val exposureClient: CommonExposureClient,
        val exposureNotificationManager: ExposureNotificationManager,
        val contextInteractor: ContextInteractor,
        val exposureNotificationRepository: ExposureNotificationRepository,
        val bluetoothRepository: BluetoothRepository
    )

    protected abstract val dependencyHolder: DependencyHolder

    protected val disposables = CompositeDisposable()

    abstract fun onCreate(moveToNextState: (ExposureNotificationPhase) -> Unit)

    fun onCleared() {
        disposables.dispose()
    }


    /**
     * Initial state. Waiting until user clicked to enable switch button.
     */
    data class WaitingForWantedState(
        override val dependencyHolder: DependencyHolder
    ) : ExposureNotificationPhase() {

        override fun onCreate(moveToNextState: (ExposureNotificationPhase) -> Unit) {
            disposables += dependencyHolder.exposureNotificationManager.observeUserWantsToRegisterAppForExposureNotification()
                .subscribeOn(Schedulers.single())
                .observeOn(Schedulers.single())
                .subscribe { wantedState ->
                    if (wantedState) {
                        moveToNextState(CheckPrerequisitesError(dependencyHolder))
                    }
                }
        }
    }

    /**
     * Checking valid google play services.
     */
    data class CheckPrerequisitesError(
        override val dependencyHolder: DependencyHolder
    ) : ExposureNotificationPhase() {

        override fun onCreate(moveToNextState: (ExposureNotificationPhase) -> Unit) {
            with(dependencyHolder) {

                val serviceStatus: ExposureServiceStatus = exposureClient.getServiceStatus()
                val serviceErrorPhase: ExposureNotificationPhase? = serviceStatus.toErrorPhaseOrNull(dependencyHolder = dependencyHolder)

                val nextState: ExposureNotificationPhase = when {
                    serviceErrorPhase != null -> {
                        serviceErrorPhase
                    }
                    bluetoothRepository.bluetoothSupported.not() -> {
                        PrerequisitesError.BluetoothNotSupported(dependencyHolder)
                    }
                    else -> {
                        RegisterToFramework(dependencyHolder, true)
                    }
                }

                moveToNextState(nextState)

            }
        }

    }

    /**
     * If prerequisites not met, this is class for explaining the errors.
     */
    sealed class PrerequisitesError : ExposureNotificationPhase() {

        protected lateinit var moveToNextState: (ExposureNotificationPhase) -> Unit

        override fun onCreate(moveToNextState: (ExposureNotificationPhase) -> Unit) {
            this.moveToNextState = moveToNextState
            disposables += dependencyHolder.exposureNotificationManager.observeUserWantsToRegisterAppForExposureNotification()
                .subscribe { wantedState ->
                    if (wantedState.not()) {
                        moveToNextState(WaitingForWantedState(dependencyHolder))
                    }
                }
        }

        /**
         * Will check prerequisites again.
         */
        fun refresh() {
            moveToNextState(CheckPrerequisitesError(dependencyHolder))
        }

        /**
         * Google play services are not available on the phone or there is some error.
         * Some of errors user can resolve.
         */
        // TODO: Break up sealed class and move Google-Error logic into 'GMS' module
        sealed class UnavailableGooglePlayServices : PrerequisitesError() {

            abstract val googlePlayAvailability: GoogleApiAvailability
            abstract val googlePlayServicesStatusCode: Int

            data class ServiceMissing(
                override val dependencyHolder: DependencyHolder,
                override val googlePlayAvailability: GoogleApiAvailability,
                override val googlePlayServicesStatusCode: Int
            ) : UnavailableGooglePlayServices()

            data class ServiceUpdating(
                override val dependencyHolder: DependencyHolder,
                override val googlePlayAvailability: GoogleApiAvailability,
                override val googlePlayServicesStatusCode: Int
            ) : UnavailableGooglePlayServices()

            data class ServiceVersionUpdateRequired(
                override val dependencyHolder: DependencyHolder,
                override val googlePlayAvailability: GoogleApiAvailability,
                override val googlePlayServicesStatusCode: Int
            ) : UnavailableGooglePlayServices()

            data class ServiceDisabled(
                override val dependencyHolder: DependencyHolder,
                override val googlePlayAvailability: GoogleApiAvailability,
                override val googlePlayServicesStatusCode: Int
            ) : UnavailableGooglePlayServices()

            data class ServiceInvalid(
                override val dependencyHolder: DependencyHolder,
                override val googlePlayAvailability: GoogleApiAvailability,
                override val googlePlayServicesStatusCode: Int
            ) : UnavailableGooglePlayServices()
        }

        // TODO: Break up sealed class and move Huawei-Error logic into 'HMS' module
        sealed class HuaweiErrorPhase : PrerequisitesError() {

            abstract val huaweiServicesStatusCode: Int

            data class DeviceTooOld(
                override val dependencyHolder: DependencyHolder,
                override val huaweiServicesStatusCode: Int
            ) : HuaweiErrorPhase()

            data class HmsCoreNotFound(
                override val dependencyHolder: DependencyHolder,
                override val huaweiServicesStatusCode: Int
            ) : HuaweiErrorPhase()

            data class OutOfDate(
                override val dependencyHolder: DependencyHolder,
                override val huaweiServicesStatusCode: Int
            ) : HuaweiErrorPhase()

            data class Unavailable(
                override val dependencyHolder: DependencyHolder,
                override val huaweiServicesStatusCode: Int
            ) : HuaweiErrorPhase()

            data class UnofficialVersion(
                override val dependencyHolder: DependencyHolder,
                override val huaweiServicesStatusCode: Int
            ) : HuaweiErrorPhase()

            data class UnknownStatus(
                override val dependencyHolder: DependencyHolder,
                override val huaweiServicesStatusCode: Int
            ) : HuaweiErrorPhase()
        }

        /**
         * The current Google play services version is not matching Exposure notification minimum version.
         */
        data class InvalidVersionOfGooglePlayServices(
            override val dependencyHolder: DependencyHolder
        ) : PrerequisitesError()

        /**
         * Bluetooth adapter doesn't exist or is not supported by exposure notification framework.
         */
        data class BluetoothNotSupported(
            override val dependencyHolder: DependencyHolder
        ) : PrerequisitesError()
    }

    /**
     * Registering to the exposure notification framework.
     * @param register True to start it, false to stop it.
     */
    data class RegisterToFramework(
        override val dependencyHolder: DependencyHolder,
        val register: Boolean
    ) : ExposureNotificationPhase() {

        override fun onCreate(moveToNextState: (ExposureNotificationPhase) -> Unit) {
            with(dependencyHolder) {
                disposables += exposureNotificationRepository.observeAppIsRegisteredForExposureNotifications()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.single())
                    .skip(1) // will skip the last value
                    .doOnSubscribe {
                        exposureNotificationRepository.refreshExposureNotificationAppRegisteredState()
                    }
                    .take(1)
                    .subscribe { currentFrameworkRegisteredState ->
                        when {
                            register -> {
                                if (currentFrameworkRegisteredState.not()) {
                                    exposureNotificationRepository.registerAppForExposureNotifications()
                                    moveToNextState(CheckingFrameworkError(dependencyHolder, register))
                                } else {
                                    moveToNextState(CheckingFrameworkRunning(dependencyHolder))
                                }
                            }
                            register.not() -> {
                                if (currentFrameworkRegisteredState) {
                                    exposureNotificationRepository.unregisterAppFromExposureNotifications()
                                    moveToNextState(CheckingFrameworkError(dependencyHolder, register))
                                } else {
                                    moveToNextState(WaitingForWantedState(dependencyHolder))
                                }
                            }
                        }
                    }
            }
        }
    }

    /**
     * Checking possible errors from registration process.
     * @param register True to start it, false to stop it.
     */
    data class CheckingFrameworkError(
        override val dependencyHolder: DependencyHolder,
        val register: Boolean
    ) : ExposureNotificationPhase() {

        override fun onCreate(moveToNextState: (ExposureNotificationPhase) -> Unit) {
            with(dependencyHolder) {
                disposables += exposureNotificationRepository.observeRegistrationState()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.single())
                    .subscribe { state ->
                        when (state) {
                            State.Idle -> {
                                moveToNextState(
                                    if (register) {
                                        CheckingFrameworkRunning(dependencyHolder)
                                    } else {
                                        WaitingForWantedState(dependencyHolder)
                                    }
                                )
                            }
                            is State.Error -> {
                                when (state.error) {
                                    is ApiException -> {
                                        val apiException = state.error as ApiException
                                        moveToNextState(
                                            when (apiException.statusCode) {
                                                CommonStatusCodes.SIGN_IN_REQUIRED -> {
                                                    Timber.e(SilentError("SIGN_IN_REQUIRED", apiException))
                                                    FrameworkError.Critical.SignInRequired(dependencyHolder, apiException, register)
                                                }
                                                CommonStatusCodes.INVALID_ACCOUNT -> {
                                                    Timber.e(SilentError("INVALID_ACCOUNT", apiException))
                                                    FrameworkError.Critical.InvalidAccount(dependencyHolder, apiException, register)
                                                }
                                                CommonStatusCodes.RESOLUTION_REQUIRED -> {
                                                    // no logging of error, this state is expect-able
                                                    FrameworkError.Critical.ResolutionRequired(dependencyHolder, apiException, register)
                                                }
                                                CommonStatusCodes.NETWORK_ERROR -> {
                                                    Timber.e(SilentError("NETWORK_ERROR", apiException))
                                                    FrameworkError.Critical.NetworkError(dependencyHolder, apiException, register)
                                                }
                                                CommonStatusCodes.INTERNAL_ERROR -> {
                                                    Timber.e(SilentError("INTERNAL_ERROR", apiException))
                                                    FrameworkError.Critical.InternalError(dependencyHolder, apiException, register)
                                                }
                                                CommonStatusCodes.DEVELOPER_ERROR -> {
                                                    Timber.e(SilentError("DEVELOPER_ERROR", apiException))
                                                    FrameworkError.Critical.DeveloperError(dependencyHolder, apiException, register)
                                                }
                                                CommonStatusCodes.ERROR -> {
                                                    Timber.e(SilentError("ERROR", apiException))
                                                    FrameworkError.Critical.Error(dependencyHolder, apiException, register)
                                                }
                                                CommonStatusCodes.INTERRUPTED -> {
                                                    Timber.e(SilentError("INTERRUPTED", apiException))
                                                    FrameworkError.Critical.Interrupted(dependencyHolder, apiException, register)
                                                }
                                                CommonStatusCodes.TIMEOUT -> {
                                                    Timber.e(SilentError("TIMEOUT", apiException))
                                                    FrameworkError.Critical.Timeout(dependencyHolder, apiException, register)
                                                }
                                                CommonStatusCodes.CANCELED -> {
                                                    Timber.e(SilentError("CANCELED", apiException))
                                                    FrameworkError.Critical.Canceled(dependencyHolder, apiException, register)
                                                }
                                                CommonStatusCodes.API_NOT_CONNECTED -> {
                                                    Timber.e(SilentError("API_NOT_CONNECTED", apiException))
                                                    FrameworkError.Critical.ApiNotConnected(dependencyHolder, apiException, register)
                                                }
                                                else -> {
                                                    FrameworkError.Critical.Unknown(dependencyHolder, apiException, register)
                                                }
                                            }
                                        )
                                    }
                                    is CancellationException -> {
                                        moveToNextState(WaitingForWantedState(dependencyHolder))
                                    }
                                    else -> {
                                        moveToNextState(FrameworkError.Critical.Unknown(dependencyHolder, state.error, register))
                                    }
                                }
                            }
                        }
                    }
                disposables += exposureNotificationManager.observeUserWantsToRegisterAppForExposureNotification()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.single())
                    .subscribe { wantedState ->
                        if (wantedState.not()) {
                            moveToNextState(WaitingForWantedState(dependencyHolder))
                        }
                    }
            }
        }
    }

    /**
     * Errors from exposure notification framework.
     */
    sealed class FrameworkError : ExposureNotificationPhase() {

        protected lateinit var moveToNextState: (ExposureNotificationPhase) -> Unit
        protected abstract val register: Boolean

        override fun onCreate(moveToNextState: (ExposureNotificationPhase) -> Unit) {
            this.moveToNextState = moveToNextState
            disposables += dependencyHolder.exposureNotificationManager.observeUserWantsToRegisterAppForExposureNotification()
                .subscribeOn(Schedulers.single())
                .observeOn(Schedulers.single())
                .subscribe { wantedState ->
                    if (wantedState.not()) {
                        moveToNextState(RegisterToFramework(dependencyHolder, false))
                    }
                }
        }

        /**
         * Will refresh exposure notification registration state again.
         */
        open fun refresh() {
            moveToNextState(RegisterToFramework(dependencyHolder, register))
        }

        /**
         * Framework cannot be started with this error.
         */
        sealed class Critical : FrameworkError() {

            /**
             * Framework [ApiException] of [CommonStatusCodes.SIGN_IN_REQUIRED].
             */
            data class SignInRequired(
                override val dependencyHolder: DependencyHolder,
                val exception: ApiException,
                override val register: Boolean
            ) : Critical()

            /**
             * Framework [ApiException] of [CommonStatusCodes.INVALID_ACCOUNT].
             */
            data class InvalidAccount(
                override val dependencyHolder: DependencyHolder,
                val exception: ApiException,
                override val register: Boolean
            ) : Critical()

            /**
             * Framework [ApiException] of [CommonStatusCodes.RESOLUTION_REQUIRED].
             * User must confirm the registration app to the framework.
             */
            data class ResolutionRequired(
                override val dependencyHolder: DependencyHolder,
                val exception: ApiException,
                override val register: Boolean
            ) : Critical() {

                fun onResolutionOk() {
                    dependencyHolder.exposureNotificationRepository.onExposureNotificationRegistrationResolutionResultOk()
                    moveToNextState(CheckingFrameworkError(dependencyHolder, register))
                }

                fun onResolutionNotOk() {
                    dependencyHolder.exposureNotificationRepository.onExposureNotificationRegistrationResolutionResultNotOk()
                    moveToNextState(ResolutionDeclined(dependencyHolder, register))
                }
            }

            /**
             * Registration dialog to the framework was declined by user.
             */
            data class ResolutionDeclined(
                override val dependencyHolder: DependencyHolder,
                override val register: Boolean
            ) : Critical()

            /**
             * Framework [ApiException] of [CommonStatusCodes.NETWORK_ERROR].
             */
            data class NetworkError(
                override val dependencyHolder: DependencyHolder,
                val exception: ApiException,
                override val register: Boolean
            ) : Critical()

            /**
             * Framework [ApiException] of [CommonStatusCodes.INTERNAL_ERROR].
             */
            data class InternalError(
                override val dependencyHolder: DependencyHolder,
                val exception: ApiException,
                override val register: Boolean
            ) : Critical()

            /**
             * Framework [ApiException] of [CommonStatusCodes.DEVELOPER_ERROR].
             */
            data class DeveloperError(
                override val dependencyHolder: DependencyHolder,
                val exception: ApiException,
                override val register: Boolean
            ) : Critical()

            /**
             * Framework [ApiException] of [CommonStatusCodes.ERROR].
             */
            data class Error(
                override val dependencyHolder: DependencyHolder,
                val exception: ApiException,
                override val register: Boolean
            ) : Critical()

            /**
             * Framework [ApiException] of [CommonStatusCodes.INTERRUPTED].
             */
            data class Interrupted(
                override val dependencyHolder: DependencyHolder,
                val exception: ApiException,
                override val register: Boolean
            ) : Critical()

            /**
             * Framework [ApiException] of [CommonStatusCodes.TIMEOUT].
             */
            data class Timeout(
                override val dependencyHolder: DependencyHolder,
                val exception: ApiException,
                override val register: Boolean
            ) : Critical()

            /**
             * Framework [ApiException] of [CommonStatusCodes.CANCELED].
             */
            data class Canceled(
                override val dependencyHolder: DependencyHolder,
                val exception: ApiException,
                override val register: Boolean
            ) : Critical()

            /**
             * Framework [ApiException] of [CommonStatusCodes.API_NOT_CONNECTED].
             */
            data class ApiNotConnected(
                override val dependencyHolder: DependencyHolder,
                val exception: ApiException,
                override val register: Boolean
            ) : Critical()

            /**
             * Framework caused some unknown error.
             * @param register True to start it, false to stop it.
             */
            data class Unknown(
                override val dependencyHolder: DependencyHolder,
                val exception: Throwable,
                override val register: Boolean
            ) : Critical()
        }

        /**
         * Framework can be started with this error, but it might not work properly at this time.
         */
        sealed class NotCritical : FrameworkError() {

            /**
             * Bluetooth is not enabled.
             */
            data class BluetoothNotEnabled(
                override val dependencyHolder: DependencyHolder
            ) : NotCritical() {

                override val register: Boolean
                    get() = throw IllegalAccessException("Not used in this context")

                override fun onCreate(moveToNextState: (ExposureNotificationPhase) -> Unit) {
                    super.onCreate(moveToNextState)
                    disposables += dependencyHolder.bluetoothRepository.observeBluetoothEnabledState()
                        .subscribeOn(Schedulers.single())
                        .observeOn(Schedulers.single())
                        .subscribe { enabled ->
                            if (enabled) {
                                moveToNextState(CheckingFrameworkRunning(dependencyHolder))
                            }
                        }
                }
            }
        }
    }

    /**
     * Checking if framework is registered and running.
     */
    data class CheckingFrameworkRunning(
        override val dependencyHolder: DependencyHolder
    ) : ExposureNotificationPhase() {

        override fun onCreate(moveToNextState: (ExposureNotificationPhase) -> Unit) {
            with(dependencyHolder) {
                disposables += bluetoothRepository.observeBluetoothEnabledState()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.single())
                    .subscribe { enabled ->
                        if (enabled.not()) {
                            moveToNextState(FrameworkError.NotCritical.BluetoothNotEnabled(dependencyHolder))
                        }
                    }
                disposables += exposureNotificationRepository.observeAppIsRegisteredForExposureNotifications()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.single())
                    .subscribe { realState ->
                        if (realState) {
                            moveToNextState(FrameworkRunning(dependencyHolder))
                        }
                    }
                disposables += exposureNotificationManager.observeUserWantsToRegisterAppForExposureNotification()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.single())
                    .subscribe { wantedState ->
                        if (wantedState.not()) {
                            moveToNextState(RegisterToFramework(dependencyHolder, false))
                        }
                    }
            }
        }
    }

    /**
     * Framework is registered and running.
     */
    data class FrameworkRunning(
        override val dependencyHolder: DependencyHolder
    ) : ExposureNotificationPhase() {

        override fun onCreate(moveToNextState: (ExposureNotificationPhase) -> Unit) {
            with(dependencyHolder) {
                disposables += bluetoothRepository.observeBluetoothEnabledState()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.single())
                    .subscribe { enabled ->
                        if (enabled.not()) {
                            moveToNextState(FrameworkError.NotCritical.BluetoothNotEnabled(dependencyHolder))
                        }
                    }
                disposables += exposureNotificationManager.observeUserWantsToRegisterAppForExposureNotification()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.single())
                    .subscribe { wantedState ->
                        if (wantedState.not()) {
                            moveToNextState(RegisterToFramework(dependencyHolder, false))
                        }
                    }
                disposables += exposureNotificationRepository.observeAppIsRegisteredForExposureNotifications()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.single())
                    .subscribe { realState ->
                        if (realState.not()) {
                            moveToNextState(RegisterToFramework(dependencyHolder, false))
                        }
                    }
            }
        }
    }
}