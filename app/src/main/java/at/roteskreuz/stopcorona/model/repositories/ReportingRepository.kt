package at.roteskreuz.stopcorona.model.repositories

import android.util.Base64
import at.roteskreuz.stopcorona.constants.Constants.Misc.EMPTY_STRING
import at.roteskreuz.stopcorona.model.api.ApiInteractor
import at.roteskreuz.stopcorona.model.db.dao.InfectionMessageDao
import at.roteskreuz.stopcorona.model.db.dao.NearbyRecordDao
import at.roteskreuz.stopcorona.model.entities.infection.info.ApiAddressedInfectionMessage
import at.roteskreuz.stopcorona.model.entities.infection.info.ApiInfectionInfoRequest
import at.roteskreuz.stopcorona.model.entities.infection.info.ApiPersonalData
import at.roteskreuz.stopcorona.model.entities.infection.info.WarningType
import at.roteskreuz.stopcorona.model.entities.infection.message.InfectionMessageContent
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.manager.DatabaseCleanupManager
import at.roteskreuz.stopcorona.model.repositories.ReportingRepository.Companion.SCOPE_NAME
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.scope.Scope
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import org.threeten.bp.ZonedDateTime
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
     * Request a TAN for authentication.
     */
    suspend fun requestTan(mobileNumber: String)

    /**
     * Upload the report information with the upload infection request.
     * @throws InvalidConfigurationException
     *
     * @return Returns the messageType  the user sent to his contacts
     */
    suspend fun uploadReportInformation(): MessageType

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
    private val configurationRepository: ConfigurationRepository,
    private val nearbyRecordDao: NearbyRecordDao,
    private val infectionMessageDao: InfectionMessageDao,
    private val cryptoRepository: CryptoRepository,
    private val infectionMessengerRepository: InfectionMessengerRepository,
    private val quarantineRepository: QuarantineRepository,
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

    override fun setMessageType(messageType: MessageType) {
        messageTypeSubject.onNext(messageType)
    }

    override suspend fun requestTan(mobileNumber: String) {
        apiInteractor.requestTan(mobileNumber)
    }

    override suspend fun uploadReportInformation(): MessageType {
        return when (messageTypeSubject.value) {
            MessageType.Revoke.Suspicion -> uploadRevokeSuspicionInfo()
            MessageType.Revoke.Sickness -> uploadRevokeSicknessInfo()
            else -> uploadInfectionInfo()
        }
    }

    private suspend fun uploadInfectionInfo(): MessageType.InfectionLevel {
        return withContext(coroutineContext) {
            val infectionLevel = messageTypeSubject.value as? MessageType.InfectionLevel ?: throw InvalidConfigurationException.InfectionLevelNotSet

            val configuration = configurationRepository.observeConfiguration().blockingFirst()
            val warnBeforeSymptoms = configuration.warnBeforeSymptoms ?: throw InvalidConfigurationException.NullWarnBeforeSymptoms
            var thresholdTime = ZonedDateTime.now().minusHours(warnBeforeSymptoms.toLong())

            val infectionMessages = mutableListOf<Pair<ByteArray, InfectionMessageContent>>()

            if (infectionLevel == MessageType.InfectionLevel.Red) {
                infectionMessages.addAll(
                    infectionMessageDao.getSentInfectionMessagesByMessageType(MessageType.InfectionLevel.Yellow)
                        .map { message ->
                            message.publicKey to InfectionMessageContent(
                                MessageType.InfectionLevel.Red,
                                message.timeStamp,
                                message.uuid
                            )
                        }
                )

                if (infectionMessages.isNotEmpty()) {
                    infectionMessages.sortByDescending { (_, content) -> content.timeStamp }
                    thresholdTime = infectionMessages.first().second.timeStamp
                }
            } else if (infectionLevel == MessageType.InfectionLevel.Yellow) {
                val resetMessages = infectionMessageDao.getSentInfectionMessagesByMessageType(MessageType.InfectionLevel.Yellow)
                    .map { message ->
                        message.publicKey to InfectionMessageContent(
                            MessageType.Revoke.Suspicion,
                            message.timeStamp,
                            message.uuid
                        )
                    }

                infectionMessengerRepository.storeSentInfectionMessages(resetMessages)
            }

            infectionMessages.addAll(nearbyRecordDao.observeRecordsRecentThan(thresholdTime)
                .blockingFirst()
                .map { nearbyRecord ->
                    nearbyRecord.publicKey to InfectionMessageContent(infectionLevel, nearbyRecord.timestamp)
                }
            )

            apiInteractor.setInfectionInfo(
                ApiInfectionInfoRequest(
                    tanDataSubject.value.tan,
                    encryptInfectionMessages(infectionMessages),
                    personalDataSubject.value.asApiEntity(infectionLevel.warningType)
                )
            )

            infectionMessengerRepository.storeSentInfectionMessages(infectionMessages)

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

            infectionLevel
        }
    }

    private suspend fun uploadRevokeSuspicionInfo(): MessageType.Revoke.Suspicion {
        return withContext(coroutineContext) {
            val infectionMessages = infectionMessageDao.getSentInfectionMessagesByMessageType(MessageType.InfectionLevel.Yellow)
                .map { message ->
                    message.publicKey to InfectionMessageContent(
                        MessageType.Revoke.Suspicion,
                        message.timeStamp,
                        message.uuid
                    )
                }

            apiInteractor.setInfectionInfo(
                ApiInfectionInfoRequest(
                    tanDataSubject.value.tan,
                    encryptInfectionMessages(infectionMessages),
                    personalDataSubject.value.asApiEntity(MessageType.Revoke.Suspicion.warningType)
                )
            )

            quarantineRepository.revokePositiveSelfDiagnose(backup = false)
            databaseCleanupManager.removeSentYellowMessages()

            MessageType.Revoke.Suspicion
        }
    }

    private suspend fun uploadRevokeSicknessInfo(): MessageType.Revoke.Sickness {
        return withContext(coroutineContext) {

            val updateStatus = when {
                quarantineRepository.hasSelfDiagnoseBackup -> MessageType.InfectionLevel.Yellow
                else -> MessageType.Revoke.Suspicion
            }

            val infectionMessages = infectionMessageDao.getSentInfectionMessagesByMessageType(MessageType.InfectionLevel.Red)
                .map { message ->
                    message.publicKey to InfectionMessageContent(
                        updateStatus,
                        message.timeStamp,
                        message.uuid
                    )
                }

            apiInteractor.setInfectionInfo(
                ApiInfectionInfoRequest(
                    tanDataSubject.value.tan,
                    encryptInfectionMessages(infectionMessages),
                    personalDataSubject.value.asApiEntity(updateStatus.warningType)
                )
            )

            infectionMessengerRepository.storeSentInfectionMessages(infectionMessages)

            quarantineRepository.revokeMedicalConfirmation()

            when (updateStatus) {
                is MessageType.InfectionLevel.Yellow -> {
                    quarantineRepository.reportPositiveSelfDiagnoseFromBackup()
                }
                is MessageType.Revoke.Suspicion -> {
                    quarantineRepository.revokePositiveSelfDiagnose(backup = false)
                }
            }

            MessageType.Revoke.Sickness
        }
    }

    private fun encryptInfectionMessages(infectionMessages: List<Pair<ByteArray, InfectionMessageContent>>): List<ApiAddressedInfectionMessage> {
        return infectionMessages.map { (publicKey, infectionMessage) ->
            Pair(
                cryptoRepository.encrypt(infectionMessage.toByteArray(), publicKey),
                cryptoRepository.getPublicKeyPrefix(publicKey)
            )
        }.filter { (encryptedInfectionMessage, addressPrefix) ->
            encryptedInfectionMessage != null
        }.map { (encryptedInfectionMessage, addressPrefix) ->
            val encodedEncryptedInfectionMessage = Base64.encodeToString(encryptedInfectionMessage, Base64.NO_WRAP)
            ApiAddressedInfectionMessage(encodedEncryptedInfectionMessage, addressPrefix)
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
) {

    fun asApiEntity(warningType: WarningType): ApiPersonalData {
        return ApiPersonalData(EMPTY_STRING, EMPTY_STRING, mobileNumber, EMPTY_STRING, EMPTY_STRING, warningType, EMPTY_STRING)
    }
}

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

    object NullWarnBeforeSymptoms : InvalidConfigurationException("warnBeforeSymptoms is null")

    object InfectionLevelNotSet : InvalidConfigurationException("messageType is null")
}
