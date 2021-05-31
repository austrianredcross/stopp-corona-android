package at.roteskreuz.stopcorona.model.repositories

import android.content.SharedPreferences
import androidx.work.WorkManager
import at.roteskreuz.stopcorona.constants.Constants.Prefs
import at.roteskreuz.stopcorona.model.entities.configuration.DbConfiguration
import at.roteskreuz.stopcorona.model.entities.infection.info.WarningType
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.workers.EndQuarantineNotifierWorker.Companion.cancelEndQuarantineReminder
import at.roteskreuz.stopcorona.model.workers.EndQuarantineNotifierWorker.Companion.enqueueEndQuarantineReminder
import at.roteskreuz.stopcorona.model.workers.SelfRetestNotifierWorker.Companion.cancelSelfRetestingReminder
import at.roteskreuz.stopcorona.model.workers.SelfRetestNotifierWorker.Companion.enqueueSelfRetestingReminder
import at.roteskreuz.stopcorona.skeleton.core.utils.*
import at.roteskreuz.stopcorona.utils.*
import at.roteskreuz.stopcorona.utils.view.safeMap
import com.github.dmstocking.optional.java.util.Optional
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import kotlinx.coroutines.rx2.awaitFirst
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Repository for controlling quarantine status of the user.
 */
interface QuarantineRepository {

    /**
     * Get date of first medical confirmation.
     */
    val dateOfFirstMedicalConfirmation: ZonedDateTime?

    /**
     * Get date of last red contact.
     */
    val dateOfLastRedContact: Instant?

    /**
     * Get date of last yellow contact.
     */
    val dateOfLastYellowContact: Instant?

    /**
     * Indicator if the user was in yellow state before turning red.
     */
    val hasSelfDiagnoseBackup: Boolean

    /**
     * Observe date of the first medical confirmation.
     */
    fun observeDateOfFirstMedicalConfirmation(): Observable<Optional<ZonedDateTime>>

    /**
     * Observe date of the first self diagnose.
     */
    fun observeDateOfFirstSelfDiagnose(): Observable<Optional<ZonedDateTime>>

    /**
     * Observe current quarantine status.
     */
    fun observeQuarantineState(): Observable<QuarantineStatus>

    /**
     * User has reported himself as red case.
     */
    fun reportMedicalConfirmation(timeOfReport: ZonedDateTime?)

    /**
     * User has reported himself as yellow case.
     */
    fun reportPositiveSelfDiagnose(timeOfReport: ZonedDateTime?)

    /**
     * User has revoked his official sickness status and goes back to yellow case.
     */
    fun reportPositiveSelfDiagnoseFromBackup()

    /**
     * User revoked the state of his red case.
     */
    fun revokeMedicalConfirmation()

    /**
     * User revoked the state of his yellow case.
     *
     * Use [backup] to store the self diagnose timestamps in a backup.
     */
    fun revokePositiveSelfDiagnose(backup: Boolean)

    /**
     * Some contact has reported [warningType].
     */
    fun receivedWarning(warningType: WarningType, timeOfContact: Instant = Instant.now())

    /**
     * User completed the questionnaire and has to monitor his symptoms
     */
    fun reportSelfMonitoring(timeOfReport: ZonedDateTime = ZonedDateTime.now())

    /**
     * User revoked the state of symptom monitoring
     */
    fun revokeSelfMonitoring()

    /**
     * Show information about quarantine end.
     */
    fun setShowQuarantineEnd()

    /**
     * Hide information about quarantine end.
     */
    fun quarantineEndSeen()

    /**
     * Observe if quarantine end information can be shown.
     */
    fun observeShowQuarantineEnd(): Observable<Boolean>

    /**
     * Resets the last red contact date.
     */
    fun revokeLastRedContactDate()

    /**
     * Resets the last yellow contact date.
     */
    fun revokeLastYellowContactDate()

    /**
     * Return the current Warning type based on the last Yellow and Red contact date times.
     */
    suspend fun getCurrentWarningType(): WarningType

    /**
     * Observe if the user needs to upload the exposure keys from the day of the submission.
     * The user can upload the missing exposure keys starting with the next day after
     * a successful submission.
     */
    fun observeIfUploadOfMissingExposureKeysIsNeeded(): Observable<Optional<UploadMissingExposureKeys>>

    /**
     * Mark the upload of the missing exposure keys as done.
     */
    fun markMissingExposureKeysAsUploaded()

    /**
     * Mark the upload of the missing exposure keys as not done.
     */
    fun markMissingExposureKeysAsNotUploaded()

    /**
     * Observe a combined warning type, this warning type provides information about
     * the red and yellow contacts.
     */
    fun observeCombinedWarningType(): Observable<CombinedWarningType>

    /**
     * Observe date of last red contact
     */
    fun observeLastRedContactDate(): Observable<Optional<Instant>>

    /**
     * Observe date of last yellow contact
     */
    fun observeLastYellowContactDate(): Observable<Optional<Instant>>

    /**
     * Observe date of last self monitoring instruction
     */
    fun observeDateOfLastSelfMonitoringInstruction(): Observable<Optional<ZonedDateTime>>
}

class QuarantineRepositoryImpl(
    preferences: SharedPreferences,
    configurationRepository: ConfigurationRepository,
    private val workManager: WorkManager
) : QuarantineRepository {

    companion object {
        private const val PREF_DATE_OF_FIRST_MEDICAL_CONFIRMATION = Prefs.QUARANTINE_REPOSITORY_PREFIX + "date_of_first_medical_confirmation"
        private const val PREF_DATE_OF_FIRST_SELF_DIAGNOSE = Prefs.QUARANTINE_REPOSITORY_PREFIX + "date_of_first_self_diagnose"
        private const val PREF_DATE_OF_FIRST_SELF_DIAGNOSE_BACKUP = Prefs.QUARANTINE_REPOSITORY_PREFIX + "date_of_first_self_diagnose_backup"
        private const val PREF_DATE_OF_LAST_SELF_DIAGNOSE = Prefs.QUARANTINE_REPOSITORY_PREFIX + "date_of_last_self_diagnose"
        private const val PREF_DATE_OF_LAST_SELF_DIAGNOSE_BACKUP = Prefs.QUARANTINE_REPOSITORY_PREFIX + "date_of_last_self_diagnose_backup"
        private const val PREF_DATE_OF_LAST_RED_CONTACT = Prefs.QUARANTINE_REPOSITORY_PREFIX + "date_of_last_red_contact"
        private const val PREF_DATE_OF_LAST_YELLOW_CONTACT = Prefs.QUARANTINE_REPOSITORY_PREFIX + "date_of_last_yellow_contact"
        private const val PREF_DATE_OF_LAST_SELF_MONITORING = Prefs.QUARANTINE_REPOSITORY_PREFIX + "date_opf_last_self_monitoring"
        private const val PREF_SHOW_QUARANTINE_END = Prefs.QUARANTINE_REPOSITORY_PREFIX + "show_quarantine_end"
        private const val PREF_MISSING_KEYS_UPLOADED = Prefs.QUARANTINE_REPOSITORY_PREFIX + "missing_keys_uploaded"
    }

    override var dateOfFirstMedicalConfirmation: ZonedDateTime? by preferences.nullableZonedDateTimeSharedPreferencesProperty(
        PREF_DATE_OF_FIRST_MEDICAL_CONFIRMATION)

    private val dateOfFirstMedicalConfirmationObservable =
        preferences.observeNullableZonedDateTime(PREF_DATE_OF_FIRST_MEDICAL_CONFIRMATION).shareReplayLast()

    private var dateOfFirstSelfDiagnose: ZonedDateTime? by preferences.nullableZonedDateTimeSharedPreferencesProperty(
        PREF_DATE_OF_FIRST_SELF_DIAGNOSE)

    private var dateOfFirstSelfDiagnoseObservable = preferences.observeNullableZonedDateTime(PREF_DATE_OF_FIRST_SELF_DIAGNOSE).shareReplayLast()

    private var dateOfFirstSelfDiagnoseBackup: ZonedDateTime?
        by preferences.nullableZonedDateTimeSharedPreferencesProperty(PREF_DATE_OF_FIRST_SELF_DIAGNOSE_BACKUP)

    private var dateOfLastSelfDiagnose: ZonedDateTime?
        by preferences.nullableZonedDateTimeSharedPreferencesProperty(PREF_DATE_OF_LAST_SELF_DIAGNOSE)

    private val dateOfLastSelfDiagnoseObservable = preferences.observeNullableZonedDateTime(PREF_DATE_OF_LAST_SELF_DIAGNOSE).shareReplayLast()

    private var dateOfLastSelfDiagnoseBackup: ZonedDateTime?
        by preferences.nullableZonedDateTimeSharedPreferencesProperty(PREF_DATE_OF_LAST_SELF_DIAGNOSE_BACKUP)

    override var dateOfLastRedContact: Instant?
        by preferences.nullableInstantSharedPreferencesProperty(
            PREF_DATE_OF_LAST_RED_CONTACT
        )

    private val dateOfLastRedContactObservable = preferences.observeNullableInstant(PREF_DATE_OF_LAST_RED_CONTACT).shareReplayLast()

    override var dateOfLastYellowContact: Instant?
        by preferences.nullableInstantSharedPreferencesProperty(
            PREF_DATE_OF_LAST_YELLOW_CONTACT
        )

    private val dateOfLastYellowContactObservable = preferences.observeNullableInstant(PREF_DATE_OF_LAST_YELLOW_CONTACT).shareReplayLast()

    private var dateOfLastSelfMonitoringInstruction: ZonedDateTime?
        by preferences.nullableZonedDateTimeSharedPreferencesProperty(
            PREF_DATE_OF_LAST_SELF_MONITORING
        )

    private val dateOfLastSelfMonitoringInstructionObservable =
        preferences.observeNullableZonedDateTime(PREF_DATE_OF_LAST_SELF_MONITORING).shareReplayLast()

    private var showQuarantineEnd: Boolean
        by preferences.booleanSharedPreferencesProperty(PREF_SHOW_QUARANTINE_END, false)

    private val showQuarantineEndObservable = preferences.observeBoolean(PREF_SHOW_QUARANTINE_END, false).shareReplayLast()

    private var missingKeysUploaded: Boolean
        by preferences.booleanSharedPreferencesProperty(PREF_MISSING_KEYS_UPLOADED, false)

    private val missingKeysUploadedObservable = preferences.observeBoolean(PREF_MISSING_KEYS_UPLOADED, false).shareReplayLast()

    override val hasSelfDiagnoseBackup: Boolean
        get() = dateOfFirstSelfDiagnoseBackup != null && dateOfLastSelfDiagnoseBackup != null

    /**
     * Emit at the next time the quarantineState needs updating.
     */
    private val quarantineStateNeedsTimeBasedUpdateEventSubject = TimerEventSubject().apply { startTicker() }

    private val quarantineStateObservable = Observables.combineLatest(
        configurationRepository.observeConfiguration(),
        quarantineStateNeedsTimeBasedUpdateEventSubject,
        dateOfFirstMedicalConfirmationObservable,
        dateOfLastSelfDiagnoseObservable,
        dateOfLastRedContactObservable,
        dateOfLastYellowContactObservable,
        dateOfLastSelfMonitoringInstructionObservable
    ) { configuration,
        _,
        medicalConfirmationFirstDateTime,
        selfDiagnoseLastDateTime,
        redContactLastDateTime,
        yellowContactLastDateTime,
        selfMonitoringLastDateTime ->
        QuarantinePrerequisitesHolder(
            configuration,
            medicalConfirmationFirstDateTime.orElse(null),
            selfDiagnoseLastDateTime.orElse(null),
            redContactLastDateTime.orElse(null),
            yellowContactLastDateTime.orElse(null),
            selfMonitoringLastDateTime.orElse(null)
        )
    }.subscribeOnComputation()
        .debounce(50, TimeUnit.MILLISECONDS) // some of the shared prefs can be changed together
        .map { prerequisites ->
            prerequisites.toQuarantineStatus()
        }
        // don't notify again if nothing has changed
        .distinctUntilChanged()
        .shareReplayLast()

    init {
        var oldState: QuarantineStatus? = null

        // Ignore dispoasable. We are a singleton.
        @Suppress("UNUSED_VARIABLE")
        val ignoredDisposable = quarantineStateObservable.subscribe { newState ->
            if ((oldState is QuarantineStatus.Jailed.Limited || oldState is QuarantineStatus.Jailed.Forever) && newState is QuarantineStatus.Free) {
                setShowQuarantineEnd()
            }
            if (newState is QuarantineStatus.Jailed.Limited) {
                val nextUpdateNeeded = listOfNotNull(
                    newState.byRedWarning,
                    newState.byYellowWarning,
                    newState.bySelfYellowDiagnosis
                ).min()
                // Schedule recalculation of quarantine state
                quarantineStateNeedsTimeBasedUpdateEventSubject.setNextEvent(nextUpdateNeeded)
            }

            oldState = newState

            updateNotifications(newState)

        }
    }

    /**
     * Logic of making quarantine status by prerequisites.
     */
    private fun QuarantinePrerequisitesHolder.toQuarantineStatus(): QuarantineStatus {
        /**
         * Own health state is Red.
         * Quarantine never ends.
         */
        if (medicalConfirmationFirstDateTime != null) {
            return QuarantineStatus.Jailed.Forever
        }

        val redWarningQuarantinedHours = configuration.redWarningQuarantine?.toLong()
            .safeMap("redWarningQuarantine is null", defaultValue = TimeUnit.DAYS.toHours(14L))
        val yellowWarningQuarantinedHours = configuration.yellowWarningQuarantine?.toLong()
            .safeMap("yellowWarningQuarantine is null", defaultValue = TimeUnit.DAYS.toHours(7L))
        val selfDiagnoseQuarantinedHours = configuration.selfDiagnosedQuarantine?.toLong()
            .safeMap("selfDiagnosedQuarantine is null", defaultValue = TimeUnit.DAYS.toHours(7L))

        val now = ZonedDateTime.now()

        // Quarantine end in contact time + 14 days
        val redWarningQuarantineUntil = redContactLastDateTime?.let {
            ZonedDateTime.ofInstant(it, ZoneId.systemDefault()).plusHours(redWarningQuarantinedHours).afterOrNull(now)
        }
        // Quarantine end in contact time + 7 days
        val yellowWarningQuarantineUntil = yellowContactLastDateTime?.let {
            ZonedDateTime.ofInstant(it, ZoneId.systemDefault()).plusHours(yellowWarningQuarantinedHours).afterOrNull(now)
        }
        // Quarantine end in diagnose time + 7 days
        val selfYellowDiagnoseQuarantineUntil = selfDiagnoseLastDateTime?.plusHours(selfDiagnoseQuarantinedHours)?.afterOrNull(now)

        val quarantinedUntil = listOfNotNull(redWarningQuarantineUntil, yellowWarningQuarantineUntil, selfYellowDiagnoseQuarantineUntil)
            .max()

        /**
         * Quarantine end date computed as maximum of possible ends.
         */
        return if (quarantinedUntil != null) {
            QuarantineStatus.Jailed.Limited(
                end = quarantinedUntil,
                bySelfYellowDiagnosis = selfYellowDiagnoseQuarantineUntil,
                byRedWarning = redWarningQuarantineUntil,
                byYellowWarning = yellowWarningQuarantineUntil,
                selfMonitoring = selfMonitoringLastDateTime != null
            )
        } else {
            QuarantineStatus.Free(selfMonitoringLastDateTime != null)
        }
    }

    /**
     * Schedule, reschedule or cancel notifications by [quarantineStatus].
     * Notifications:
     * - each 6 hours for self retesting
     * - at the end of the quarantine
     */
    private fun updateNotifications(quarantineStatus: QuarantineStatus) {
        when (quarantineStatus) {
            is QuarantineStatus.Jailed.Limited -> {
                enqueueSelfRetestingReminder(workManager)
                enqueueEndQuarantineReminder(workManager, quarantineStatus.end)
            }
            QuarantineStatus.Jailed.Forever -> {
                enqueueSelfRetestingReminder(workManager)
                cancelEndQuarantineReminder(workManager)
            }
            is QuarantineStatus.Free -> {
                if (quarantineStatus.selfMonitoring) {
                    enqueueSelfRetestingReminder(workManager)
                } else {
                    cancelSelfRetestingReminder(workManager)
                }
                cancelEndQuarantineReminder(workManager)
            }
        }
    }

    override fun observeDateOfFirstMedicalConfirmation(): Observable<Optional<ZonedDateTime>> {
        return dateOfFirstMedicalConfirmationObservable
    }

    override fun observeDateOfFirstSelfDiagnose(): Observable<Optional<ZonedDateTime>> {
        return dateOfFirstSelfDiagnoseObservable
    }

    override fun observeQuarantineState(): Observable<QuarantineStatus> {
        return quarantineStateObservable
    }

    override fun reportMedicalConfirmation(timeOfReport: ZonedDateTime?) {
        timeOfReport?.let {
            dateOfFirstMedicalConfirmation = timeOfReport
        } ?: run {
            dateOfFirstMedicalConfirmation = ZonedDateTime.now()
        }
    }

    override fun reportPositiveSelfDiagnose(timeOfReport: ZonedDateTime?) {
        timeOfReport?.let {
            if (dateOfFirstSelfDiagnose == null) {
                dateOfFirstSelfDiagnose = timeOfReport
            }
            dateOfLastSelfDiagnose = timeOfReport
        } ?: run {
            if (dateOfFirstSelfDiagnose == null) {
                dateOfFirstSelfDiagnose = ZonedDateTime.now()
            }
            dateOfLastSelfDiagnose = ZonedDateTime.now()
        }
    }

    override fun reportPositiveSelfDiagnoseFromBackup() {
        dateOfFirstSelfDiagnose = dateOfFirstSelfDiagnoseBackup
        dateOfLastSelfDiagnose = dateOfLastSelfDiagnoseBackup
        dateOfFirstSelfDiagnoseBackup = null
        dateOfLastSelfDiagnoseBackup = null
    }

    override fun revokeMedicalConfirmation() {
        dateOfFirstMedicalConfirmation = null
    }

    override fun revokePositiveSelfDiagnose(backup: Boolean) {
        if (backup) {
            dateOfFirstSelfDiagnoseBackup = dateOfFirstSelfDiagnose
            dateOfLastSelfDiagnoseBackup = dateOfLastSelfDiagnose
        }

        dateOfFirstSelfDiagnose = null
        dateOfLastSelfDiagnose = null
    }

    override fun reportSelfMonitoring(timeOfReport: ZonedDateTime) {
        dateOfLastSelfMonitoringInstruction = timeOfReport
    }

    override fun revokeSelfMonitoring() {
        dateOfLastSelfMonitoringInstruction = null
    }

    override fun receivedWarning(warningType: WarningType, timeOfContact: Instant) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (warningType) {
            WarningType.YELLOW -> {
                if (dateOfLastYellowContact.areOnTheSameUtcDay(timeOfContact).not()) {
                    dateOfLastYellowContact = timeOfContact
                } else {
                    Timber.d("no update of the yellow quarantine neccesary, we stay at " +
                        "$dateOfLastYellowContact. Update to $timeOfContact can be discarded")
                }
            }
            WarningType.RED -> {
                if (dateOfLastRedContact.areOnTheSameUtcDay(timeOfContact).not()) {
                    dateOfLastRedContact = timeOfContact
                } else {
                    Timber.d("no update of the red quarantine neccesary, we stay at " +
                        "$dateOfLastRedContact. Update to $timeOfContact can be discarded")
                }
            }
        }
    }

    override suspend fun getCurrentWarningType(): WarningType {
        val quarantineStatus = quarantineStateObservable.awaitFirst()
        if (quarantineStatus is QuarantineStatus.Jailed.Limited) {
            if (quarantineStatus.byRedWarning != null && (quarantineStatus.byYellowWarning == null || quarantineStatus.byRedWarning > quarantineStatus.byYellowWarning)) {
                return WarningType.RED
            } else if (quarantineStatus.byYellowWarning != null) {
                return WarningType.YELLOW
            }
        }
        return WarningType.GREEN
    }

    override fun setShowQuarantineEnd() {
        showQuarantineEnd = true
    }

    override fun observeCombinedWarningType(): Observable<CombinedWarningType> {
        return quarantineStateObservable
            .map {
                if (it is QuarantineStatus.Jailed.Limited) {
                    CombinedWarningType(
                        yellowContactsDetected = it.byYellowWarning != null,
                        redContactsDetected = it.byRedWarning != null
                    )
                } else {
                    CombinedWarningType(
                        yellowContactsDetected = false,
                        redContactsDetected = false
                    )
                }
            }
    }

    override fun observeLastRedContactDate(): Observable<Optional<Instant>> {
        return dateOfLastRedContactObservable
    }

    override fun observeLastYellowContactDate(): Observable<Optional<Instant>> {
        return dateOfLastYellowContactObservable
    }

    override fun observeDateOfLastSelfMonitoringInstruction(): Observable<Optional<ZonedDateTime>> {
        return dateOfLastSelfMonitoringInstructionObservable
    }

    override fun quarantineEndSeen() {
        showQuarantineEnd = false
    }

    override fun observeShowQuarantineEnd(): Observable<Boolean> {
        return showQuarantineEndObservable
    }

    override fun revokeLastRedContactDate() {
        dateOfLastRedContact = null
    }

    override fun revokeLastYellowContactDate() {
        dateOfLastYellowContact = null
    }

    override fun observeIfUploadOfMissingExposureKeysIsNeeded(): Observable<Optional<UploadMissingExposureKeys>> {
        return Observables.combineLatest(
            dateOfFirstMedicalConfirmationObservable,
            dateOfLastSelfDiagnoseObservable,
            missingKeysUploadedObservable
        ).debounce(50, TimeUnit.MILLISECONDS) // some of the shared prefs can be changed together
            .map { (dateOfFirstMedicalConfirmation, dateOfLastSelfDiagnose, areMissingKeysUploaded) ->
                if (dateOfFirstMedicalConfirmation.isPresent &&
                    areMissingKeysUploaded.not()
                ) {
                    Optional.of(UploadMissingExposureKeys(
                        dateOfFirstMedicalConfirmation.get(),
                        MessageType.InfectionLevel.Red
                    ))
                } else if (dateOfLastSelfDiagnose.isPresent &&
                    areMissingKeysUploaded.not()
                ) {
                    Optional.of(UploadMissingExposureKeys(
                        dateOfLastSelfDiagnose.get(),
                        MessageType.InfectionLevel.Yellow
                    ))
                } else {
                    Optional.empty<UploadMissingExposureKeys>()
                }
            }
    }

    override fun markMissingExposureKeysAsUploaded() {
        missingKeysUploaded = true
    }

    override fun markMissingExposureKeysAsNotUploaded() {
        missingKeysUploaded = false
    }
}

private data class QuarantinePrerequisitesHolder(
    val configuration: DbConfiguration,
    val medicalConfirmationFirstDateTime: ZonedDateTime?,
    val selfDiagnoseLastDateTime: ZonedDateTime?,
    val redContactLastDateTime: Instant?,
    val yellowContactLastDateTime: Instant?,
    val selfMonitoringLastDateTime: ZonedDateTime?
)

/**
 * Status about quarantine of the user.
 */
sealed class QuarantineStatus {

    /**
     * User must be in the quarantine.
     */
    sealed class Jailed : QuarantineStatus() {

        /**
         * Quarantine ends at [end] time.
         * After this time user's state is [Free].
         */
        data class Limited(val end: ZonedDateTime, val bySelfYellowDiagnosis: ZonedDateTime?, val byRedWarning: ZonedDateTime?,
            val byYellowWarning: ZonedDateTime?, val selfMonitoring: Boolean = false) :
            Jailed() {

            /**
             * Number of days displayed to the user until he is off quarantine (we add one to account for the offset on the last day)
             */
            fun daysUntilEnd(): Long {
                return LocalDate.now().daysTo(end.toLocalDate()) + 1
            }
        }

        /**
         * Quarantine never ends.
         */
        object Forever : Jailed()
    }

    /**
     * User doesn't have to be in the quarantine.
     */
    data class Free(val selfMonitoring: Boolean = false) : QuarantineStatus()
}

/**
 * Indicates the date for which the exposure keys have not been uploaded and
 * the associated diagnostic.
 */
data class UploadMissingExposureKeys(val missingExposureKeyDate: ZonedDateTime, val messageType: MessageType) {

    val uploadAfter = missingExposureKeyDate.endOfTheUtcDay()

    val shouldUploadNow: Boolean
        get() = ZonedDateTime.now().isAfter(uploadAfter)
}

/**
 * Describes a warning type that provides information about the red and yellow contacts.
 */
data class CombinedWarningType(val yellowContactsDetected: Boolean, val redContactsDetected: Boolean)