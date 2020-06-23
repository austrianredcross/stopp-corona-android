package at.roteskreuz.stopcorona.model.repositories

import at.roteskreuz.stopcorona.constants.Constants.Misc.EMPTY_STRING
import at.roteskreuz.stopcorona.model.api.ApiInteractor
import at.roteskreuz.stopcorona.model.entities.infection.info.ApiVerificationPayload
import at.roteskreuz.stopcorona.model.entities.infection.info.WarningType
import at.roteskreuz.stopcorona.model.entities.infection.info.asApiEntity
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.managers.DatabaseCleanupManager
import at.roteskreuz.stopcorona.model.repositories.ReportingRepository.Companion.SCOPE_NAME
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.scope.Scope
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import at.roteskreuz.stopcorona.utils.endOfTheUtcDay
import at.roteskreuz.stopcorona.utils.startOfTheUtcDay
import at.roteskreuz.stopcorona.utils.toRollingStartIntervalNumber
import at.roteskreuz.stopcorona.utils.view.safeMap
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import org.threeten.bp.ZonedDateTime
import java.util.UUID
import kotlin.coroutines.CoroutineContext

/**
 * Scoped repository for handling data during uploading the result of a self-testing,
 * a sickness certificate or a self-test revoke.
 */
interface ReportingRepository {

    companion object {
        const val SCOPE_NAME = "ReportingRepositoryScope"
    }

    /**
     * Sets the messageType that will be reported to authorities at the end of the reporting flow.
     */
    fun setMessageType(messageType: MessageType)

    /**
     * Sets the date for which missing temporary exposure keys need to be uploaded.
     * If a value is present report the exposure keys only for the specified date, otherwise
     * perform a regular reporting.
     */
    fun setDateWithMissingExposureKeys(dateWithMissingExposureKeys: ZonedDateTime?)

    /**
     * Request a TAN for authentication.
     */
    suspend fun requestTan(mobileNumber: String)

    /**
     * Upload the report information with the upload infection request.
     * @throws InvalidConfigurationException - in case the configuration doesn't provide
     * all the necessary data.
     *
     * @return Returns the messageType the user sent to his contacts
     */
    suspend fun uploadReportInformation(temporaryTracingKeys: List<TemporaryExposureKey>): MessageType

    /**
     * Set the validated personal data when a TAN was successfully requested.
     */
    fun setPersonalDataAndTanRequestSuccess(mobileNumber: String)

    /**
     * Set the TAN introduced by the user.
     */
    fun setTan(tan: String)

    /**
     * Set the latest agreement of the user about data reporting.
     */
    fun setUserAgreement(agreement: Boolean)

    /**
     * Navigate back from the TAN entry screen.
     */
    fun goBackFromTanEntryScreen()

    /**
     * Navigate back from the reporting agreement screen.
     */
    fun goBackFromReportingAgreementScreen()

    /**
     * Observe the state of the reporting.
     */
    fun observeReportingState(): Observable<ReportingState>

    /**
     * Observe the personal data.
     */
    fun observePersonalData(): Observable<PersonalData>

    /**
     * Observe the TAN related data.
     */
    fun observeTanData(): Observable<TanData>

    /**
     * Observe the data related to user agreement.
     */
    fun observeAgreementData(): Observable<AgreementData>

    /**
     * Observe the messageType that will reported in this flow.
     * @throws [InvalidConfigurationException]
     */
    fun observeMessageType(): Observable<MessageType>
}

class ReportingRepositoryImpl(
    private val appDispatchers: AppDispatchers,
    private val apiInteractor: ApiInteractor,
    private val quarantineRepository: QuarantineRepository,
    private val contextInteractor: ContextInteractor,
    private val infectionMessengerRepository: InfectionMessengerRepository,
    private val configurationRepository: ConfigurationRepository,
    private val databaseCleanupManager: DatabaseCleanupManager
) : Scope(SCOPE_NAME),
    ReportingRepository,
    CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = appDispatchers.Default

    private val personalDataSubject = NonNullableBehaviorSubject(
        PersonalData()
    )

    private val tanDataSubject = NonNullableBehaviorSubject(
        TanData()
    )

    private val agreementDataSubject = NonNullableBehaviorSubject(AgreementData())
    private val messageTypeSubject = BehaviorSubject.create<MessageType>()

    private var tanUuid: String? = null

    private var dateWithMissingExposureKeys: ZonedDateTime? = null

    override fun setMessageType(messageType: MessageType) {
        messageTypeSubject.onNext(messageType)
    }

    override fun setDateWithMissingExposureKeys(dateWithMissingExposureKeys: ZonedDateTime?) {
        this.dateWithMissingExposureKeys = dateWithMissingExposureKeys
    }

    override suspend fun requestTan(mobileNumber: String) {
        tanUuid = apiInteractor.requestTan(mobileNumber).uuid
    }

    override suspend fun uploadReportInformation(temporaryTracingKeys: List<TemporaryExposureKey>): MessageType {
        return when (messageTypeSubject.value) {
            MessageType.Revoke.Suspicion -> uploadRevokeSuspicionInfo(temporaryTracingKeys)
            MessageType.Revoke.Sickness -> uploadRevokeSicknessInfo(temporaryTracingKeys)
            else -> uploadInfectionInfo(temporaryTracingKeys)
        }
    }

    private suspend fun uploadInfectionInfo(temporaryExposureKeysFromSDK: List<TemporaryExposureKey>): MessageType.InfectionLevel {
        return withContext(coroutineContext) {
            val infectionLevel = messageTypeSubject.value as? MessageType.InfectionLevel
                ?: throw InvalidConfigurationException.InfectionLevelNotSet
            val configuration = configurationRepository.getConfiguration()
                ?: throw InvalidConfigurationException.ConfigurationNotPresent
            val uploadKeysDays = configuration.uploadKeysDays
                ?: throw InvalidConfigurationException.NullNumberOfDaysToUpload
            var thresholdTime = ZonedDateTime.now()
                .minusDays(uploadKeysDays.toLong())
                .startOfTheUtcDay()
                .toRollingStartIntervalNumber()

            val dateWithMissingExposureKeys = dateWithMissingExposureKeys

            if (dateWithMissingExposureKeys != null) {
                // Report only the exposure keys that have not been uploaded in the day of the previous submission.
                val temporaryExposureKeyWrappers = temporaryExposureKeysFromSDK
                    .filter {
                        it.rollingStartIntervalNumber >= dateWithMissingExposureKeys.startOfTheUtcDay().toRollingStartIntervalNumber() &&
                            it.rollingStartIntervalNumber <= dateWithMissingExposureKeys.endOfTheUtcDay().toRollingStartIntervalNumber()
                    }.groupBy { it.rollingStartIntervalNumber }
                    .map { (rollingStartIntervalNumber, _) ->
                        TemporaryExposureKeysWrapper(
                            rollingStartIntervalNumber,
                            UUID.randomUUID(),
                            infectionLevel
                        )
                    }

                uploadData(
                    infectionLevel.warningType,
                    temporaryExposureKeyWrappers.asTemporaryExposureKeys(
                        temporaryExposureKeysFromSDK
                    )
                )

                infectionMessengerRepository.storeSentTemporaryExposureKeys(
                    temporaryExposureKeyWrappers
                )

                quarantineRepository.markMissingExposureKeysAsUploaded()
            } else {
                // The regular flow of reporting the exposure keys.
                val infectionMessages = if (infectionLevel == MessageType.InfectionLevel.Red) {
                    prepareListOfTemporaryExposureKeysForRedReporting(temporaryExposureKeysFromSDK, thresholdTime)
                } else {
                    prepareListOfTemporaryExposureKeysForYellowReporting(temporaryExposureKeysFromSDK, thresholdTime)
                }

                val infectionMessagesAsTemporaryExposureKeys =
                    infectionMessages.asTemporaryExposureKeys(temporaryExposureKeysFromSDK)

                uploadData(infectionLevel.warningType, infectionMessagesAsTemporaryExposureKeys)

                infectionMessengerRepository.storeSentTemporaryExposureKeys(infectionMessages)

                when (infectionLevel) {
                    MessageType.InfectionLevel.Red -> {
                        quarantineRepository.reportMedicalConfirmation()
                        quarantineRepository.revokePositiveSelfDiagnose(backup = true)
                        quarantineRepository.revokeSelfMonitoring()
                    }
                    MessageType.InfectionLevel.Yellow -> {
                        quarantineRepository.reportPositiveSelfDiagnose()
                        quarantineRepository.revokeSelfMonitoring()
                    }
                }
            }

            infectionLevel
        }
    }

    private suspend fun prepareListOfTemporaryExposureKeysForRedReporting(
        temporaryExposureKeysFromSDK: List<TemporaryExposureKey>,
        thresholdTime: Int
    ): List<TemporaryExposureKeysWrapper> {
        val temporaryExposureKeysList = mutableListOf<TemporaryExposureKeysWrapper>()

        val sentYellowTemporaryExposureKeys =
            infectionMessengerRepository.getSentTemporaryExposureKeysByMessageType(
                MessageType.InfectionLevel.Yellow
            )

        // In case a yellow reporting happened in the past we report the keys reported yellow and
        // the newer keys from Exposure SDK.
        if (sentYellowTemporaryExposureKeys.isNotEmpty()) {
            temporaryExposureKeysList.addAll(
                sentYellowTemporaryExposureKeys
                    .map { message ->
                        TemporaryExposureKeysWrapper(
                            message.rollingStartIntervalNumber,
                            message.password,
                            MessageType.InfectionLevel.Red
                        )
                    }
            )

            val latestYellowExposureKeys =
                sentYellowTemporaryExposureKeys.maxBy { it.rollingStartIntervalNumber }?.rollingStartIntervalNumber ?: thresholdTime

            temporaryExposureKeysList.addAll(
                temporaryExposureKeysFromSDK
                    .filter { it.rollingStartIntervalNumber > latestYellowExposureKeys }
                    .groupBy { it.rollingStartIntervalNumber }
                    .map { (rollingStartIntervalNumber, _) ->
                        TemporaryExposureKeysWrapper(
                            rollingStartIntervalNumber,
                            UUID.randomUUID(),
                            MessageType.InfectionLevel.Red
                        )
                    }
            )
        } else {
            // Either is a completely new reporting or the reporting takes place after a revokation.
            temporaryExposureKeysList.addAll(
                prepareListWithGreenTemporaryExposureKeysFromDbAndNewKeysFromTheSDK(temporaryExposureKeysFromSDK, thresholdTime,
                    MessageType.InfectionLevel.Red)
            )
        }

        return temporaryExposureKeysList
    }

    private suspend fun prepareListOfTemporaryExposureKeysForYellowReporting(
        temporaryExposureKeysFromSDK: List<TemporaryExposureKey>,
        thresholdTime: Int
    ): List<TemporaryExposureKeysWrapper> {
        return prepareListWithGreenTemporaryExposureKeysFromDbAndNewKeysFromTheSDK(
            temporaryExposureKeysFromSDK,
            thresholdTime,
            MessageType.InfectionLevel.Yellow
        )
    }

    /**
     * Search in database for green (revoked) exposure keys that are newer than the reporting threshold [thresholdTime].
     * In case such keys are present we use them and for the rest of the period (> latest green key found)
     * we report the keys from the Exposure SDK.
     */
    private suspend fun prepareListWithGreenTemporaryExposureKeysFromDbAndNewKeysFromTheSDK(
        temporaryExposureKeysFromSDK: List<TemporaryExposureKey>,
        thresholdTime: Int,
        infectionLevel: MessageType
    ): List<TemporaryExposureKeysWrapper> {
        val temporaryExposureKeysList = mutableListOf<TemporaryExposureKeysWrapper>()

        val sentGreenTemporaryExposureKeys =
            infectionMessengerRepository.getSentTemporaryExposureKeysByMessageType(
                MessageType.GeneralRevoke
            )

        val latestGreenRollingStartIntervalNumber =
            sentGreenTemporaryExposureKeys.maxBy { it.rollingStartIntervalNumber }?.rollingStartIntervalNumber ?: Int.MIN_VALUE

        if (latestGreenRollingStartIntervalNumber <= thresholdTime) {
            // The latest revoked key it's older than the reporting threshold,
            // we will get all the keys from the Exposure SDK.
            temporaryExposureKeysList.addAll(
                temporaryExposureKeysFromSDK
                    .filter { it.rollingStartIntervalNumber > thresholdTime }
                    .groupBy { it.rollingStartIntervalNumber }
                    .map { (rollingStartIntervalNumber, _) ->
                        TemporaryExposureKeysWrapper(
                            rollingStartIntervalNumber,
                            UUID.randomUUID(),
                            infectionLevel
                        )
                    }
            )
        } else if (latestGreenRollingStartIntervalNumber > thresholdTime) {
            // There are revoked keys that are valid for the current reporting window (> thresholdTime)
            // We report these revoked keys + newer keys provided by the Exposure SDK.
            temporaryExposureKeysList.addAll(
                sentGreenTemporaryExposureKeys.filter {
                    it.rollingStartIntervalNumber > thresholdTime
                }.map { temporaryExposureKey ->
                    TemporaryExposureKeysWrapper(
                        temporaryExposureKey.rollingStartIntervalNumber,
                        temporaryExposureKey.password,
                        infectionLevel
                    )
                }
            )

            temporaryExposureKeysList.addAll(
                temporaryExposureKeysFromSDK
                    .filter { it.rollingStartIntervalNumber > latestGreenRollingStartIntervalNumber }
                    .groupBy { it.rollingStartIntervalNumber }
                    .map { (rollingStartIntervalNumber, _) ->
                        TemporaryExposureKeysWrapper(
                            rollingStartIntervalNumber,
                            UUID.randomUUID(),
                            infectionLevel
                        )
                    }
            )
        }

        return temporaryExposureKeysList
    }

    private suspend fun uploadData(
        warningType: WarningType,
        temporaryExposureKeys: List<Pair<List<TemporaryExposureKey>, UUID>>
    ) {
        apiInteractor.uploadInfectionData(
            temporaryExposureKeys.asApiEntity(),
            contextInteractor.packageName,
            warningType,
            ApiVerificationPayload(
                tanUuid.safeMap(defaultValue = EMPTY_STRING),
                tanDataSubject.value.tan
            )
        )
    }

    private suspend fun uploadRevokeSuspicionInfo(temporaryExposureKeysFromSDK: List<TemporaryExposureKey>): MessageType.Revoke.Suspicion {
        return withContext(coroutineContext) {
            val yellowTemporaryExposureKeys =
                infectionMessengerRepository.getSentTemporaryExposureKeysByMessageType(MessageType.InfectionLevel.Yellow)
            val listOfYellowTemporaryExposureKeysFromSDK = yellowTemporaryExposureKeys
                .map { temporaryExposureKey ->
                    TemporaryExposureKeysWrapper(
                        temporaryExposureKey.rollingStartIntervalNumber,
                        temporaryExposureKey.password,
                        temporaryExposureKey.messageType
                    )
                }.asTemporaryExposureKeys(temporaryExposureKeysFromSDK)

            uploadData(MessageType.Revoke.Suspicion.warningType, listOfYellowTemporaryExposureKeysFromSDK)

            quarantineRepository.revokePositiveSelfDiagnose(backup = false)
            // Mark the yellow exposure keys as green in database.
            val greenTemporaryExposureKeys = yellowTemporaryExposureKeys.map { temporaryExposureKey ->
                TemporaryExposureKeysWrapper(
                    temporaryExposureKey.rollingStartIntervalNumber,
                    temporaryExposureKey.password,
                    MessageType.GeneralRevoke
                )
            }
            infectionMessengerRepository.storeSentTemporaryExposureKeys(greenTemporaryExposureKeys)
            quarantineRepository.markMissingExposureKeysAsNotUploaded()

            MessageType.Revoke.Suspicion
        }
    }

    private suspend fun uploadRevokeSicknessInfo(temporaryExposureKeysFromSDK: List<TemporaryExposureKey>): MessageType.Revoke.Sickness {
        return withContext(coroutineContext) {

            val updateStatus = when {
                quarantineRepository.hasSelfDiagnoseBackup -> MessageType.InfectionLevel.Yellow
                else -> MessageType.GeneralRevoke
            }

            val infectionMessages =
                infectionMessengerRepository.getSentTemporaryExposureKeysByMessageType(MessageType.InfectionLevel.Red)
                    .map { message ->
                        TemporaryExposureKeysWrapper(
                            message.rollingStartIntervalNumber,
                            message.password,
                            updateStatus
                        )
                    }

            val infectionMessagesAsTemporaryExposureKeys =
                infectionMessages.asTemporaryExposureKeys(temporaryExposureKeysFromSDK)

            uploadData(updateStatus.warningType, infectionMessagesAsTemporaryExposureKeys)

            infectionMessengerRepository.storeSentTemporaryExposureKeys(infectionMessages)

            quarantineRepository.revokeMedicalConfirmation()

            when (updateStatus) {
                is MessageType.InfectionLevel.Yellow -> {
                    quarantineRepository.reportPositiveSelfDiagnoseFromBackup()
                }
                is MessageType.GeneralRevoke -> {
                    quarantineRepository.revokePositiveSelfDiagnose(backup = false)
                    quarantineRepository.markMissingExposureKeysAsNotUploaded()
                }
            }

            MessageType.Revoke.Sickness
        }
    }

    private fun List<TemporaryExposureKeysWrapper>.asTemporaryExposureKeys(
        listOfKeysFromSDK: List<TemporaryExposureKey>
    ): List<Pair<List<TemporaryExposureKey>, UUID>> {
        return this.mapNotNull { temporaryExposureKeysWrapper ->
            val temporaryExposureKeysFromSdk =
                listOfKeysFromSDK.filter {
                    it.rollingStartIntervalNumber == temporaryExposureKeysWrapper.rollingStartIntervalNumber
                }
            if (temporaryExposureKeysFromSdk.isNotEmpty()) {
                temporaryExposureKeysFromSdk to temporaryExposureKeysWrapper.password
            } else {
                null
            }
        }
    }

    override fun setPersonalDataAndTanRequestSuccess(mobileNumber: String) {
        personalDataSubject.onNext(
            PersonalData(
                mobileNumber,
                true
            )
        )
    }

    override fun setTan(tan: String) {
        tanDataSubject.onNext(TanData(tan, tanIsFilled = true))
    }

    override fun setUserAgreement(agreement: Boolean) {
        agreementDataSubject.onNext(AgreementData(agreement))
    }

    override fun observeReportingState(): Observable<ReportingState> {
        return Observables.combineLatest(
            personalDataSubject,
            tanDataSubject,
            agreementDataSubject
        ).map { (personalData, tanData, _) ->
            when {
                personalData.tanSuccessfullyRequested.not() -> {
                    return@map ReportingState.PersonalDataEntry
                }
                tanData.tanIsFilled.not() -> {
                    return@map ReportingState.TanEntry
                }
                else -> {
                    return@map ReportingState.ReportingAgreement
                }
            }
        }
    }

    override fun goBackFromTanEntryScreen() {
        personalDataSubject.onNext(personalDataSubject.value.copy(tanSuccessfullyRequested = false))
        tanDataSubject.onNext(tanDataSubject.value.copy(tan = EMPTY_STRING))
    }

    override fun goBackFromReportingAgreementScreen() {
        tanDataSubject.onNext(tanDataSubject.value.copy(tanIsFilled = false))
        agreementDataSubject.onNext(agreementDataSubject.value.copy(userHasAgreed = false))
    }

    override fun observePersonalData(): Observable<PersonalData> {
        return personalDataSubject
    }

    override fun observeTanData(): Observable<TanData> {
        return tanDataSubject
    }

    override fun observeAgreementData(): Observable<AgreementData> {
        return agreementDataSubject
    }

    override fun observeMessageType(): Observable<MessageType> {
        return messageTypeSubject
    }
}

data class AgreementData(val userHasAgreed: Boolean = false)

data class TanData(val tan: String = EMPTY_STRING, val tanIsFilled: Boolean = false)

data class PersonalData(
    val mobileNumber: String = EMPTY_STRING,
    val tanSuccessfullyRequested: Boolean = false
)

/**
 * Automaton definition of the report sending process.
 */
sealed class ReportingState {

    /**
     * User has to enter his personal data.
     */
    object PersonalDataEntry : ReportingState()

    /**
     * User has to enter the TAN received via SMS.
     */
    object TanEntry : ReportingState()

    /**
     * User has to agree that his data will be reported to authorities.
     */
    object ReportingAgreement : ReportingState()
}

/**
 * Exceptions caused by invalid data in configuration.
 */
sealed class InvalidConfigurationException(override val message: String) : Exception(message) {

    /**
     * The infection level is not set for the current reporting.
     */
    object InfectionLevelNotSet : InvalidConfigurationException("messageType is null")

    /**
     * The number of days of temporary exposure keys to upload is null.
     */
    object NullNumberOfDaysToUpload :
        InvalidConfigurationException("The number of days of temporary exposure keys to be uploaded is not provided.")

    /**
     * No configuration present, not even the bundled config.
     */
    object ConfigurationNotPresent : InvalidConfigurationException("No configuration present, not even the bundled config")
}
