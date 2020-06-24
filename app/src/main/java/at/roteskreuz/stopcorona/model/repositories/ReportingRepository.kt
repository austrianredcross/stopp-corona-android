package at.roteskreuz.stopcorona.model.repositories

import at.roteskreuz.stopcorona.constants.Constants.Misc.EMPTY_STRING
import at.roteskreuz.stopcorona.model.api.ApiInteractor
import at.roteskreuz.stopcorona.model.entities.infection.info.ApiVerificationPayload
import at.roteskreuz.stopcorona.model.entities.infection.info.WarningType
import at.roteskreuz.stopcorona.model.entities.infection.info.asApiEntity
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
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
    private val diagnosisKeysRepository: DiagnosisKeysRepository,
    private val configurationRepository: ConfigurationRepository
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

    override suspend fun uploadReportInformation(teks: List<TemporaryExposureKey>): MessageType {
        return when (messageTypeSubject.value) {
            MessageType.Revoke.Suspicion -> uploadRevokeSuspicionInfo(teks)
            MessageType.Revoke.Sickness -> uploadRevokeSicknessInfo(teks)
            else -> uploadInfectionInfo(teks)
        }
    }

    private suspend fun uploadInfectionInfo(teks: List<TemporaryExposureKey>): MessageType.InfectionLevel {
        return withContext(coroutineContext) {
            val infectionLevel = messageTypeSubject.value as? MessageType.InfectionLevel
                ?: throw InvalidConfigurationException.InfectionLevelNotSet
            val configuration = configurationRepository.getConfiguration()
                ?: throw InvalidConfigurationException.ConfigurationNotPresent
            val uploadKeysDays = configuration.uploadKeysDays
                ?: throw InvalidConfigurationException.NullNumberOfDaysToUpload
            val thresholdTimeFromConfiguration = ZonedDateTime.now()
                .minusDays(uploadKeysDays.toLong())
                .startOfTheUtcDay()
                .toRollingStartIntervalNumber()

            val dateWithMissingExposureKeys = dateWithMissingExposureKeys

            if (dateWithMissingExposureKeys != null) {
                // Report only the exposure keys that have not been uploaded in the day of the previous submission.
                val keysToUpload = teks.filter {
                    it.rollingStartIntervalNumber >= dateWithMissingExposureKeys.startOfTheUtcDay().toRollingStartIntervalNumber() &&
                        it.rollingStartIntervalNumber <= dateWithMissingExposureKeys.endOfTheUtcDay().toRollingStartIntervalNumber()
                }

                uploadTeksWithMessageType(keysToUpload, infectionLevel)

                quarantineRepository.markMissingExposureKeysAsUploaded()
            } else {
                // The regular flow of reporting the exposure keys.
                val thresholdTime = if (infectionLevel == MessageType.InfectionLevel.Red) {
                    diagnosisKeysRepository.getSentTeksByMessageType(
                        MessageType.InfectionLevel.Yellow
                    ).minBy { tekMetadata ->
                        tekMetadata.rollingStartIntervalNumber
                    }?.rollingStartIntervalNumber ?: thresholdTimeFromConfiguration
                } else {
                    thresholdTimeFromConfiguration
                }

                val teksToUpload = teks.filter {
                    it.rollingStartIntervalNumber >= thresholdTime
                }
                uploadTeksWithMessageType(teksToUpload, infectionLevel)

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

    /**
     * Upload keys and store the password and new infection level to the database.
     *
     * Passwords are taken from the DB if available or random ones created if the key is published for the first time.
     */
    private suspend fun uploadTeksWithMessageType(
        teks: List<TemporaryExposureKey>,
        messageType: MessageType
    ) {
        val passwordsByRollingStartInterval = diagnosisKeysRepository
            .getSentTemporaryExposureKeys()
            .associateBy(
                keySelector = { it.rollingStartIntervalNumber },
                valueTransform = { it.password }
            )

        val tekMetadata = teks.map { tek ->
            val passwordForKey = passwordsByRollingStartInterval[tek.rollingStartIntervalNumber] ?: UUID.randomUUID()

            TekMetadata(
                tek.rollingStartIntervalNumber,
                passwordForKey,
                messageType
            )
        }

        val tekPasswordPairs =
            teks.pairWithPassword(tekMetadata)

        uploadData(messageType.warningType, tekPasswordPairs)

        diagnosisKeysRepository.storeSentTemporaryExposureKeys(tekMetadata)
    }

    private suspend fun uploadData(
        warningType: WarningType,
        tekPasswordPairs: List<Pair<TemporaryExposureKey, UUID>>
    ) {
        apiInteractor.uploadInfectionData(
            tekPasswordPairs.asApiEntity(),
            contextInteractor.packageName,
            warningType,
            ApiVerificationPayload(
                tanUuid.safeMap(defaultValue = EMPTY_STRING),
                tanDataSubject.value.tan
            )
        )
    }

    private suspend fun uploadRevokeSuspicionInfo(teks: List<TemporaryExposureKey>): MessageType.Revoke.Suspicion {
        return withContext(coroutineContext) {
            val rollingStartIntervalsToRevoke = diagnosisKeysRepository
                .getSentTeksByMessageType(MessageType.InfectionLevel.Yellow)
                .map {
                    it.rollingStartIntervalNumber
                }.toSet()

            val teksToRevoke = teks.filter { it.rollingStartIntervalNumber in rollingStartIntervalsToRevoke }
            uploadTeksWithMessageType(teksToRevoke, MessageType.GeneralRevoke)

            quarantineRepository.revokePositiveSelfDiagnose(backup = false)
            quarantineRepository.markMissingExposureKeysAsNotUploaded()
            MessageType.Revoke.Suspicion
        }
    }

    private suspend fun uploadRevokeSicknessInfo(teks: List<TemporaryExposureKey>): MessageType.Revoke.Sickness {
        return withContext(coroutineContext) {

            val updateStatus = when {
                quarantineRepository.hasSelfDiagnoseBackup -> MessageType.InfectionLevel.Yellow
                else -> MessageType.GeneralRevoke
            }

            val rollingStartIntervalsToRevoke = diagnosisKeysRepository
                .getSentTeksByMessageType(MessageType.InfectionLevel.Red)
                .map {
                    it.rollingStartIntervalNumber
                }.toSet()

            val teksToRevoke = teks.filter { it.rollingStartIntervalNumber in rollingStartIntervalsToRevoke }
            uploadTeksWithMessageType(teksToRevoke, updateStatus)

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

    private fun List<TemporaryExposureKey>.pairWithPassword(
        tekMetadata: List<TekMetadata>
    ): List<Pair<TemporaryExposureKey, UUID>> {
        val tekMetaDataByRollingStartIntervalNumber = tekMetadata.associateBy { it.rollingStartIntervalNumber }

        return mapNotNull { tek ->
            val matchingMetadata = tekMetaDataByRollingStartIntervalNumber[tek.rollingStartIntervalNumber]
            matchingMetadata?.let { tekMetadata ->
                tek to tekMetadata.password
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
