package at.roteskreuz.stopcorona.screens.dashboard

import android.content.Context
import android.text.SpannableString
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.model.entities.statistics.CovidStatistics
import at.roteskreuz.stopcorona.model.managers.ExposureNotificationPhase
import at.roteskreuz.stopcorona.model.managers.ExposureNotificationPhase.*
import at.roteskreuz.stopcorona.model.managers.ExposureNotificationPhase.FrameworkError.Critical
import at.roteskreuz.stopcorona.model.managers.ExposureNotificationPhase.FrameworkError.Critical.*
import at.roteskreuz.stopcorona.model.managers.ExposureNotificationPhase.FrameworkError.NotCritical
import at.roteskreuz.stopcorona.model.managers.ExposureNotificationPhase.PrerequisitesError.*
import at.roteskreuz.stopcorona.model.managers.ExposureNotificationPhase.PrerequisitesError.UnavailableGooglePlayServices.*
import at.roteskreuz.stopcorona.model.repositories.UploadMissingExposureKeys
import at.roteskreuz.stopcorona.screens.base.epoxy.*
import at.roteskreuz.stopcorona.screens.base.epoxy.buttons.ButtonType1Model_
import at.roteskreuz.stopcorona.screens.base.epoxy.buttons.ButtonType2Model_
import at.roteskreuz.stopcorona.screens.dashboard.epoxy.*
import at.roteskreuz.stopcorona.screens.statistics.StatisticIncidenceItem
import at.roteskreuz.stopcorona.screens.statistics.expoxy.StatisticAustriaViewModel_
import at.roteskreuz.stopcorona.screens.statistics.expoxy.StatisticsIncidenceItemModel_
import at.roteskreuz.stopcorona.skeleton.core.utils.adapterProperty
import at.roteskreuz.stopcorona.skeleton.core.utils.addTo
import at.roteskreuz.stopcorona.utils.*
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyModel
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime

/**
 * Contents of the dashboard.
 */
class DashboardController(
    private val context: Context,
    private val onAutomaticHandshakeInformationClick: () -> Unit,
    private val onStartQuestionnaireClick: (disabled: Boolean) -> Unit,
    private val onReportSuspicionClick: (disabled: Boolean) -> Unit,
    private val onReportClick: (disabled: Boolean) -> Unit,
    private val onHealthStatusClick: (data: HealthStatusData) -> Unit,
    private val onRevokeSuspicionClick: (disabled: Boolean) -> Unit,
    private val onPresentMedicalReportClick: (disabled: Boolean) -> Unit,
    private val onCheckSymptomsAgainClick: (disabled: Boolean) -> Unit,
    private val onSomeoneHasRecoveredCloseClick: () -> Unit,
    private val onQuarantineEndCloseClick: () -> Unit,
    private val onAutomaticHandshakeEnabled: (isEnabled: Boolean) -> Unit,
    private val onExposureNotificationErrorActionClick: (ExposureNotificationPhase) -> Unit,
    private val onRevokeSicknessClick: (disabled: Boolean) -> Unit,
    private val onReportHealthySicknessClick: () -> Unit,
    private val onUploadMissingExposureKeysClick: (disabled: Boolean, uploadMissingExposureKeys: UploadMissingExposureKeys) -> Unit,
    private val onShareAppClick: () -> Unit,
    private val onDiaryClick: () -> Unit,
    private val onStatisticsClick: () -> Unit,
    private val onStatisticsToggleClick: () -> Unit,
    private val onLegendClick: () -> Unit,
    private val onAdditionalInformationClick: () -> Unit
) : EpoxyController() {

    var ownHealthStatus: HealthStatusData by adapterProperty(HealthStatusData.NoHealthStatus)
    var contactsHealthStatus: HealthStatusData by adapterProperty(HealthStatusData.NoHealthStatus)
    var showQuarantineEnd: Boolean by adapterProperty(false)
    var someoneHasRecoveredHealthStatus: HealthStatusData by adapterProperty(HealthStatusData.NoHealthStatus)
    var exposureNotificationPhase: ExposureNotificationPhase? by adapterProperty(null as ExposureNotificationPhase?)
    var dateOfFirstMedicalConfirmation: ZonedDateTime? by adapterProperty(null as ZonedDateTime?)
    var uploadMissingExposureKeys: UploadMissingExposureKeys? by adapterProperty(null as UploadMissingExposureKeys?)
    var dateOfLastContact: Instant? by adapterProperty(null as Instant?)
    var dateOfLastKeyRequest: ZonedDateTime? by adapterProperty(null as ZonedDateTime?)
    var keyRequestCountLastWeek: Int? by adapterProperty(null as Int?)
    var statisticsCurrentDate: String? by adapterProperty(null as String?)
    var statisticsCompareDate: String? by adapterProperty(null as String?)
    var isStatisticsExpanded: Boolean by adapterProperty(true)
    var statisticIncidenceItems: MutableList<StatisticIncidenceItem> by adapterProperty(mutableListOf())
    var statistics: CovidStatistics? by adapterProperty(null as CovidStatistics?)

    override fun buildModels() {
        emptySpace(modelCountBuiltSoFar, 16)

        /**
         * Build all cards for own and contact health status as well as status updates
         */
        if (ownHealthStatus != HealthStatusData.NoHealthStatus ||
            contactsHealthStatus != HealthStatusData.NoHealthStatus ||
            showQuarantineEnd ||
            someoneHasRecoveredHealthStatus != HealthStatusData.NoHealthStatus
        ) {

            /**
             * Build card for own health status if available
             */
            if (ownHealthStatus != HealthStatusData.NoHealthStatus) {
                buildOwnHealthStatus()
            }

            /**
             * Add single space if own AND contact health state are available
             */
            if (ownHealthStatus != HealthStatusData.NoHealthStatus &&
                contactsHealthStatus != HealthStatusData.NoHealthStatus
            ) {
                emptySpace(modelCountBuiltSoFar, 16)
            }

            /**
             * Build card for contacts health status if available
             */
            if (contactsHealthStatus != HealthStatusData.NoHealthStatus) {
                if (contactsHealthStatus is HealthStatusData.ContactsSicknessInfo) {
                    val healthStatusData =
                        contactsHealthStatus as HealthStatusData.ContactsSicknessInfo
                    if (healthStatusData.warningType.redContactsDetected) {
                        buildContactHealthStatus(
                            redContactsDetected = true,
                            yellowContactsDetected = false
                        )
                        if (healthStatusData.warningType.yellowContactsDetected) {
                            emptySpace(modelCountBuiltSoFar, 16)
                        }
                    }
                    if (healthStatusData.warningType.yellowContactsDetected) {
                        buildContactHealthStatus(
                            redContactsDetected = false,
                            yellowContactsDetected = true
                        )
                    }
                }
            }

            /**
             * Add single space if own OR contact health state are available AND someone has recovered
             */
            if ((ownHealthStatus != HealthStatusData.NoHealthStatus ||
                        contactsHealthStatus != HealthStatusData.NoHealthStatus) &&
                someoneHasRecoveredHealthStatus == HealthStatusData.SomeoneHasRecovered
            ) {
                emptySpace(modelCountBuiltSoFar, 16)
            }

            /**
             * Build card for someone has recovered if available
             */
            if (someoneHasRecoveredHealthStatus == HealthStatusData.SomeoneHasRecovered) {
                buildSomeoneHasRecoveredStatus()
            }

            /**
             * Add single space if own or contact health state are available or someone has recovered AND the quarantine should end
             */
            if ((ownHealthStatus != HealthStatusData.NoHealthStatus ||
                        contactsHealthStatus != HealthStatusData.NoHealthStatus ||
                        someoneHasRecoveredHealthStatus == HealthStatusData.SomeoneHasRecovered) &&
                showQuarantineEnd
            ) {
                emptySpace(modelCountBuiltSoFar, 16)
            }

            /**
             * Build card for quarantine end if available
             */
            if (showQuarantineEnd) {
                buildQuarantineEndStatus()
            }

            /**
             * Add single space if a single card is shown
             */
            if (ownHealthStatus != HealthStatusData.NoHealthStatus ||
                contactsHealthStatus != HealthStatusData.NoHealthStatus ||
                someoneHasRecoveredHealthStatus == HealthStatusData.SomeoneHasRecovered ||
                showQuarantineEnd
            ) {
                emptySpace(modelCountBuiltSoFar, 32)
            }
        }

        automaticHandshakeSwitch(onAutomaticHandshakeEnabled) {
            id("automatic_handshake_switch")
            phase(exposureNotificationPhase)
        }

        emptySpace(modelCountBuiltSoFar, 16)

        exposureNotificationPhase?.let { phase ->
            when (phase) {
                is PrerequisitesError -> {
                    buildPrerequisitesErrorCard(phase)
                }
                is FrameworkError -> {
                    buildExposureFrameworkErrorCard(phase)
                }
            }
        }

        emptySpace(modelCountBuiltSoFar, 16)

        // Prioritize by own and contact health state
        val healthStatus = when {
            ownHealthStatus is HealthStatusData.SicknessCertificate -> ownHealthStatus
            contactsHealthStatus is HealthStatusData.ContactsSicknessInfo && (contactsHealthStatus as HealthStatusData.ContactsSicknessInfo).warningType.redContactsDetected -> contactsHealthStatus
            ownHealthStatus is HealthStatusData.SelfTestingSuspicionOfSickness -> ownHealthStatus
            contactsHealthStatus is HealthStatusData.ContactsSicknessInfo && (contactsHealthStatus as HealthStatusData.ContactsSicknessInfo).warningType.yellowContactsDetected -> contactsHealthStatus
            else -> ownHealthStatus
        }

        // needed to have two caches of epoxy models because of lottie
        if (exposureNotificationPhase is FrameworkRunning) {
            handshakeImage {
                id("handshake_image_active")
                active(true)
                data(healthStatus)
            }
        } else {
            handshakeImage {
                id("handshake_image_inactive")
                active(false)
            }
        }

        emptySpace(modelCountBuiltSoFar, 40)

        if (exposureNotificationPhase is FrameworkRunning) {
            riskHeadline {
                id("risk_headline_active")
                active(true)
                data(healthStatus)
            }
        } else {
            riskHeadline {
                id("risk_headline_inactive")
                active(false)
            }
        }

        emptySpace(modelCountBuiltSoFar, 40)

        separator {
            id(modelCountBuiltSoFar)
            color(R.color.gray_4)
        }

        emptySpace(modelCountBuiltSoFar, 20)

        dateOfLastContact?.let { dateOfLastContact ->
            val days = dateOfLastContact.daysTo(Instant.now())

            appUpdate {
                id("app_update_last_contact")
                imageRes(R.drawable.ic_calendar)
                status(
                    context.getString(
                        R.string.main_automatic_handshake_last_contact_days,
                        days.toString()
                    )
                )
            }
        }

        keyRequestCountLastWeek?.let { keyRequestCountLastWeek ->
            val status = if (ownHealthStatus is HealthStatusData.NoHealthStatus) {
                context.getString(
                    R.string.main_automatic_handshake_contact_checks,
                    keyRequestCountLastWeek.toString()
                )
            } else {
                context.getString(R.string.main_automatic_handshake_self_reported_info)
            }

            appUpdate {
                id("app_update_count")
                imageRes(R.drawable.ic_check)
                status(status)
            }
        }

        dateOfLastKeyRequest?.let { dateOfLastKeyRequest ->
            val lastUpdate = if (dateOfLastKeyRequest.areOnTheSameDay(ZonedDateTime.now())) {
                context.getString(R.string.general_today) + ", " + dateOfLastKeyRequest.format("HH:mm")
            } else {
                dateOfLastKeyRequest.format("d. MMM, HH:mm")
            }

            appUpdate {
                id("app_update_last_update")
                imageRes(R.drawable.ic_update)
                status(context.getString(R.string.main_automatic_handshake_last_update, lastUpdate))
            }
        }

        separator {
            id(modelCountBuiltSoFar)
            color(R.color.gray_4)
        }

        emptySpace(modelCountBuiltSoFar, 20)

        additionalInformation(onAutomaticHandshakeInformationClick) {
            id("handshake_additional_information")
            title(context.string(R.string.main_automatic_handshake_information_hint))
        }

        emptySpace(modelCountBuiltSoFar, 20)

        separator {
            id(modelCountBuiltSoFar)
            color(R.color.dashboard_separator)
        }

        emptySpace {
            id(modelCountBuiltSoFar)
            height(24)
            backgroundColor(R.color.background_gray)
        }

        buildStatisticsCard()

        emptySpace {
            id(modelCountBuiltSoFar)
            height(24)
            backgroundColor(R.color.background_gray)
        }

        separator {
            id(modelCountBuiltSoFar)
            color(R.color.dashboard_separator)
        }

        buildDiaryCard()

        separator {
            id(modelCountBuiltSoFar)
            color(R.color.dashboard_separator)
        }

        if ((ownHealthStatus is HealthStatusData.SelfTestingSuspicionOfSickness).not()
            && (ownHealthStatus is HealthStatusData.SicknessCertificate).not()
        ) {
            emptySpace {
                id(modelCountBuiltSoFar)
                height(24)
                backgroundColor(R.color.background_gray)
            }

            verticalBackgroundModelGroup(
                listOf(
                    EmptySpaceModel_()
                        .id(modelCountBuiltSoFar)
                        .height(32),
                    DescriptionBlockModel_()
                        .id("feel")
                        .title(context.string(R.string.main_body_feeling_title))
                        .description(context.string(R.string.main_body_feeling_description)),
                    EmptySpaceModel_()
                        .id(modelCountBuiltSoFar)
                        .height(16),
                    ButtonType2Model_ { onStartQuestionnaireClick(false) }
                        .id("feel_button")
                        .text(context.string(R.string.main_button_feel_today_button))
                        .enabled(exposureNotificationPhase.isReportingEnabled())
                        .onDisabledClick { onStartQuestionnaireClick(true) },
                    EmptySpaceModel_()
                        .id(modelCountBuiltSoFar)
                        .height(16),
                    ButtonType2Model_ { onReportSuspicionClick(false) }
                        .id("feel_button")
                        .text(context.string(R.string.main_button_suspicion_button))
                        .enabled(exposureNotificationPhase.isReportingEnabled())
                        .onDisabledClick { onReportSuspicionClick(true) },
                    EmptySpaceModel_()
                        .id(modelCountBuiltSoFar)
                        .height(40)
                )
            ) {
                backgroundColor(R.color.primary)
            }
        }

        if ((ownHealthStatus is HealthStatusData.SicknessCertificate).not()) {

            emptySpace {
                id(modelCountBuiltSoFar)
                height(24)
                backgroundColor(R.color.background_gray)
            }
            separator {
                id(modelCountBuiltSoFar)
                color(R.color.dashboard_separator)
            }

            emptySpace(modelCountBuiltSoFar, 24)

            verticalBackgroundModelGroup(
                listOf(
                    EmptySpaceModel_()
                        .id(modelCountBuiltSoFar)
                        .height(32),
                    DescriptionBlockModel_()
                        .id("report")
                        .title(context.string(R.string.main_body_report_title))
                        .description(context.string(R.string.main_body_report_description)),
                    EmptySpaceModel_()
                        .id(modelCountBuiltSoFar)
                        .height(16),
                    ButtonType2Model_ { onReportClick(false) }
                        .id("report_button")
                        .text(context.string(R.string.main_body_report_button))
                        .enabled(exposureNotificationPhase.isReportingEnabled())
                        .onDisabledClick { onReportClick(true) },
                    EmptySpaceModel_()
                        .id(modelCountBuiltSoFar)
                        .height(40)
                )
            ) {
                backgroundColor(R.color.primary)
            }
        } else {
            emptySpace(modelCountBuiltSoFar, 40)
        }

        buildShareAppCard()
    }

    /**
     * Display an error card if some of exposure prerequisites checks failed.
     */
    private fun buildPrerequisitesErrorCard(phase: ExposureNotificationPhase) {
        when (phase) {
            is UnavailableGooglePlayServices -> {
                exposureNotificationError({ onExposureNotificationErrorActionClick(phase) }) {
                    id("unavailable_google_play_services")
                    title(context.string(R.string.main_exposure_error_google_play_unavailable_title))
                    fun addTryToResolveButtonIfPossible() {
                        if (phase.googlePlayAvailability.isUserResolvableError(phase.googlePlayServicesStatusCode)) {
                            action(context.string(R.string.main_exposure_error_google_play_unavailable_action))
                        }
                    }
                    when (phase) {
                        is ServiceMissing -> {
                            description(context.string(R.string.main_exposure_error_google_play_unavailable_missing_message))
                            addTryToResolveButtonIfPossible()
                        }
                        is ServiceUpdating -> {
                            description(context.string(R.string.main_exposure_error_google_play_unavailable_updating_message))
                            action(context.string(R.string.main_exposure_error_google_play_unavailable_updating_action))
                        }
                        is ServiceVersionUpdateRequired -> {
                            description(context.string(R.string.main_exposure_error_google_play_unavailable_update_required_message))
                            action(context.string(R.string.main_exposure_error_google_play_unavailable_update_required_action))
                        }
                        is ServiceDisabled -> {
                            description(context.string(R.string.main_exposure_error_google_play_unavailable_disabled_message))
                            addTryToResolveButtonIfPossible()
                        }
                        is ServiceInvalid -> {
                            description(context.string(R.string.main_exposure_error_google_play_unavailable_invalid_message))
                            addTryToResolveButtonIfPossible()
                        }
                    }
                }

                emptySpace(modelCountBuiltSoFar, 16)
            }
            is InvalidVersionOfGooglePlayServices -> {
                exposureNotificationError({ onExposureNotificationErrorActionClick(phase) }) {
                    id("invalid_google_play_services_version")
                    title(context.string(R.string.main_exposure_error_google_play_wrong_version_title))
                    description(context.string(R.string.main_exposure_error_google_play_wrong_version_message))
                    action(context.string(R.string.main_exposure_error_google_play_wrong_version_action_btn))
                }

                emptySpace(modelCountBuiltSoFar, 16)
            }
            is BluetoothNotSupported -> {
                exposureNotificationError({ onExposureNotificationErrorActionClick(phase) }) {
                    id("bluetooth_not_supported")
                    title(context.string(R.string.main_exposure_error_bluetooth_not_supported_title))
                    description(context.string(R.string.main_exposure_error_bluetooth_not_supported_title))
                }

                emptySpace(modelCountBuiltSoFar, 16)
            }
            is BatteryOptimizationsNotIgnored -> {
                exposureNotificationError({ onExposureNotificationErrorActionClick(phase) }) {
                    id("battery_optimizations_not_ignored")
                    title(context.string(R.string.main_exposure_error_battery_optimizations_not_ignored_title))
                    description(context.string(R.string.main_exposure_error_battery_optimizations_not_ignored_message))
                    action(context.string(R.string.main_exposure_error_battery_optimizations_not_ignored_action_btn))
                }

                emptySpace(modelCountBuiltSoFar, 16)
            }
        }
    }

    /**
     * Display an error card if exposure framework has an error.
     */
    private fun buildExposureFrameworkErrorCard(phase: ExposureNotificationPhase) {
        fun exposureNotificationError(description: String) {
            exposureNotificationError({ onExposureNotificationErrorActionClick(phase) }) {
                id("exposure_notification_framework_error")
                title(context.string(R.string.main_exposure_error_title))
                description(description)
                action(context.string(R.string.main_exposure_error_action))
            }

            emptySpace(modelCountBuiltSoFar, 16)
        }
        when (phase) {
            is Critical -> {
                when (phase) {
                    is SignInRequired -> {
                        exposureNotificationError(context.string(R.string.main_exposure_error_sign_in_message))
                    }
                    is InvalidAccount -> {
                        exposureNotificationError(context.string(R.string.main_exposure_error_invalid_account_message))
                    }
                    is ResolutionRequired -> {
                        // ignored, there is displayed a dialog
                    }
                    is ResolutionDeclined -> {
                        exposureNotificationError(context.string(R.string.main_exposure_error_declined_message))
                    }
                    is NetworkError,
                    is Interrupted,
                    is Timeout,
                    is Canceled -> {
                        exposureNotificationError(context.string(R.string.main_exposure_error_network_error_message))
                    }
                    is InternalError,
                    is Error,
                    is Unknown -> {
                        exposureNotificationError(context.string(R.string.main_exposure_error_internal_message))
                    }
                    is DeveloperError,
                    is ApiNotConnected -> {
                        exposureNotificationError(context.string(R.string.main_exposure_error_developer_message))
                    }
                }
            }
            is NotCritical.BluetoothNotEnabled -> {
                exposureNotificationError({ onExposureNotificationErrorActionClick(phase) }) {
                    id("exposure_notification_framework_error")
                    title(context.string(R.string.main_exposure_error_title))
                    description(context.string(R.string.main_exposure_error_bluetooth_off_message))
                    action(context.string(R.string.main_exposure_error_bluetooth_off_action))
                }

                emptySpace(modelCountBuiltSoFar, 16)
            }
        }
    }

    /**
     * Build card for own health status
     */
    private fun buildOwnHealthStatus() {
        val modelList = arrayListOf<EpoxyModel<out Any>>()

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(32)
            .addTo(modelList)

        HealthStatusModel_(onHealthStatusClick)
            .id("own_health_status")
            .data(ownHealthStatus)
            .addTo(modelList)

        if (ownHealthStatus is HealthStatusData.SelfTestingSuspicionOfSickness) {
            EmptySpaceModel_()
                .id(modelCountBuiltSoFar)
                .height(16)
                .addTo(modelList)

            ButtonType2Model_ { onRevokeSuspicionClick(false) }
                .id("own_health_status_present_revoke_suspicion")
                .text(context.string(R.string.self_testing_suspicion_button_revoke))
                .enabled(exposureNotificationPhase.isReportingEnabled())
                .onDisabledClick { onRevokeSuspicionClick(true) }
                .addTo(modelList)

            EmptySpaceModel_()
                .id(modelCountBuiltSoFar)
                .height(16)
                .addTo(modelList)

            ButtonType2Model_ { onPresentMedicalReportClick(false) }
                .id("own_health_status_present_medical_report_button")
                .text(context.string(R.string.self_testing_suspicion_secondary_button))
                .enabled(exposureNotificationPhase.isReportingEnabled())
                .onDisabledClick { onPresentMedicalReportClick(true) }
                .addTo(modelList)
        }

        if (ownHealthStatus is HealthStatusData.SelfTestingSymptomsMonitoring) {
            EmptySpaceModel_()
                .id(modelCountBuiltSoFar)
                .height(16)
                .addTo(modelList)

            ButtonType2Model_ { onCheckSymptomsAgainClick(false) }
                .id("own_health_status_check_symptoms_button")
                .text(context.string(R.string.self_testing_symptoms_secondary_button))
                .enabled(exposureNotificationPhase.isReportingEnabled())
                .onDisabledClick { onCheckSymptomsAgainClick(true) }
                .addTo(modelList)
        }

        val isRedRevokingEnabled = dateOfFirstMedicalConfirmation
            ?.isAfter(
                ZonedDateTime.now()
                    .minus(Constants.Behavior.MEDICAL_CONFIRMATION_REVOKING_POSSIBLE_DURATION)
                    .startOfTheDay()
            )
            ?: true

        if (ownHealthStatus is HealthStatusData.SicknessCertificate && isRedRevokingEnabled) {
            EmptySpaceModel_()
                .id(modelCountBuiltSoFar)
                .height(16)
                .addTo(modelList)

            ButtonType2Model_ { onRevokeSicknessClick(false) }
                .id("own_health_status_revoke_sickness")
                .text(context.string(R.string.sickness_certificate_attest_revoke))
                .enabled(exposureNotificationPhase.isReportingEnabled())
                .onDisabledClick { onRevokeSicknessClick(true) }
                .addTo(modelList)
        }

        if (ownHealthStatus is HealthStatusData.SicknessCertificate && !isRedRevokingEnabled) {
            EmptySpaceModel_()
                .id(modelCountBuiltSoFar)
                .height(16)
                .addTo(modelList)

            ButtonType2Model_ { onReportHealthySicknessClick() }
                .id("own_health_status_report_healthy")
                .text(context.string(R.string.sickness_certificate_attest_report_healthy))
                .addTo(modelList)
        }

        val healthStatusDataMatches =
            ownHealthStatus is HealthStatusData.SelfTestingSuspicionOfSickness ||
                    ownHealthStatus is HealthStatusData.SicknessCertificate

        val uploadMissingExposureKeys = uploadMissingExposureKeys
        if (healthStatusDataMatches && uploadMissingExposureKeys != null) {
            EmptySpaceModel_()
                .id(modelCountBuiltSoFar)
                .height(16)
                .addTo(modelList)

            CopyTextModel_()
                .id("own_health_status_upload_missing_exposure_keys_explanation")
                .text(SpannableString(context.string(R.string.self_testing_symptoms_warning_info_update)))
                .addTo(modelList)

            EmptySpaceModel_()
                .id(modelCountBuiltSoFar)
                .height(16)
                .addTo(modelList)

            ButtonType2Model_ {
                onUploadMissingExposureKeysClick(
                    false,
                    uploadMissingExposureKeys
                )
            }
                .id("own_health_status_upload_missing_exposure_keys_button")
                .text(context.string(R.string.self_testing_symptoms_warning_button_update))
                .enabled(exposureNotificationPhase.isReportingEnabled())
                .onDisabledClick {
                    onUploadMissingExposureKeysClick(
                        true,
                        uploadMissingExposureKeys
                    )
                }
                .addTo(modelList)
        }

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(32)
            .addTo(modelList)

        verticalBackgroundModelGroup(modelList) {
            id("vertical_model_group_own_health_status")
            backgroundColor(R.color.background_gray)
        }
    }

    /**
     * Build card for contacts health status
     */
    private fun buildContactHealthStatus(
        redContactsDetected: Boolean,
        yellowContactsDetected: Boolean
    ) {
        val modelList = arrayListOf<EpoxyModel<out Any>>()

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(32)
            .addTo(modelList)

        HealthStatusModel_(onHealthStatusClick)
            .id("contacts_health_status")
            .data(contactsHealthStatus)
            .ownHealthStatus(ownHealthStatus)
            .redContactsDetected(redContactsDetected)
            .yellowContactsDetected(yellowContactsDetected)
            .addTo(modelList)

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(32)
            .addTo(modelList)

        verticalBackgroundModelGroup(modelList) {
            id("vertical_model_group_contact_health_status")
            backgroundColor(R.color.background_gray)
        }
    }

    /**
     * Build card for someone has recovered
     */
    private fun buildSomeoneHasRecoveredStatus() {
        val modelList = arrayListOf<EpoxyModel<out Any>>()

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(32)
            .addTo(modelList)

        StatusUpdateModel_(onSomeoneHasRecoveredCloseClick)
            .id("someone_has_recovered_health_status")
            .title(context.string(R.string.main_status_update_headline))
            .description(context.string(R.string.main_status_update_contact_sickness_not_confirmed_message))
            .cardStatus(CardUpdateStatus.ContactUpdate)
            .addTo(modelList)

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(32)
            .addTo(modelList)

        verticalBackgroundModelGroup(modelList) {
            id("vertical_model_group_someone_has_recovered")
            backgroundColor(R.color.background_gray)
        }
    }

    /**
     * Build card for quarantine end
     */
    private fun buildQuarantineEndStatus() {
        val modelList = arrayListOf<EpoxyModel<out Any>>()

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(32)
            .addTo(modelList)

        StatusUpdateModel_(onQuarantineEndCloseClick)
            .id("quarantine_ended")
            .title(context.string(R.string.local_notification_quarantine_end_headline))
            .description(context.string(R.string.local_notification_quarantine_end_message))
            .cardStatus(CardUpdateStatus.EndOfQuarantine)
            .addTo(modelList)

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(32)
            .addTo(modelList)

        verticalBackgroundModelGroup(modelList) {
            id("vertical_model_group_end_of_quarantine")
            backgroundColor(R.color.background_gray)
        }
    }

    private fun buildDiaryCard() {
        val modelList = arrayListOf<EpoxyModel<out Any>>()

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(32)
            .addTo(modelList)

        TitleModel_()
            .id("diary_title")
            .title(context.string(R.string.diary_main_title))
            .addTo(modelList)

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(24)
            .addTo(modelList)

        ImageModel_()
            .id("diary_img")
            .imageRes(R.drawable.ic_diary)
            .addTo(modelList)

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(24)
            .addTo(modelList)

        CopyTextModel_()
            .id("diary_description")
            .text(SpannableString(context.string(R.string.diary_main_description)))
            .addTo(modelList)

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(16)
            .addTo(modelList)

        AdditionalInformationModel_(onAdditionalInformationClick)
            .id("diary_additional_information")
            .title(context.string(R.string.diary_main_link))
            .textColor(R.color.blue)
            .addTo(modelList)

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(16)
            .addTo(modelList)

        ButtonType1Model_(onDiaryClick)
            .id("diary_edit_button")
            .text(context.string(R.string.diary_main_button))
            .addTo(modelList)

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(32)
            .addTo(modelList)

        verticalBackgroundModelGroup(modelList) {
            id("vertical_model_group_diary")
            backgroundColor(R.color.background)
        }

    }

    private fun buildStatisticsCard() {
        val modelList = arrayListOf<EpoxyModel<out Any>>()

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(32)
            .addTo(modelList)

        TitleModel_()
            .id("statistics_title")
            .title(context.getString(R.string.main_covid_statistics_title))
            .addTo(modelList)

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(10)
            .addTo(modelList)

        SmallDescriptionModel_()
            .id("statistics_description")
            .description(statisticsCurrentDate?.formatDayAndMonth(context)?.let { currentDate ->
                statisticsCompareDate?.formatDayAndMonth(context)?.let { compareDate ->
                    context.string(
                        R.string.main_covid_statistics_comparison,
                        currentDate, compareDate
                    )
                }
            })
            .addTo(modelList)

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(24)
            .addTo(modelList)

        StatisticAustriaViewModel_()
            .id("dashboard_statistics_austria_view")
            .statistics(statistics)
            .addTo(modelList)

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(24)
            .addTo(modelList)

        StatisticsExpandedCollapsedButtonModel_(onStatisticsToggleClick)
            .id("statistics_expanded_collapsed_button")
            .expanded(isStatisticsExpanded)
            .addTo(modelList)

        val modelIncidenceItemList = arrayListOf<EpoxyModel<out Any>>()
        statisticIncidenceItems.forEachIndexed { index, statisticIncidenceItem ->
            StatisticsIncidenceItemModel_()
                .id("statistic_incidence_item_$index")
                .data(statisticIncidenceItem)
                .addTo(modelIncidenceItemList)
        }

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(14)
            .addTo(modelIncidenceItemList)

        AdditionalInformationModel_(onLegendClick)
            .id("statistic_legend")
            .title(context.string(R.string.main_covid_statistics_legend))
            .textColor(R.color.blue)
            .addTo(modelIncidenceItemList)

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(24)
            .addTo(modelIncidenceItemList)

        if (isStatisticsExpanded) {
            VerticalBackgroundModelGroup_(modelIncidenceItemList)
                .id("vertical_model_group_statistics_incidence")
                .backgroundColor(R.color.white)
                .background(R.drawable.statistics_expanded_background_wrapper)
                .addTo(modelList)
        }

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(24)
            .addTo(modelList)

        ButtonType2Model_(onStatisticsClick)
            .id("statistics_overview_button")
            .text(context.getString(R.string.main_covid_statistics_show_button))
            .addTo(modelList)

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(32)
            .addTo(modelList)

        verticalBackgroundModelGroup(modelList) {
            id("vertical_model_group_statistics")
            backgroundColor(R.color.background)
        }

    }

    /**
     * Build card for sharing the app
     */
    private fun buildShareAppCard() {
        val modelList = arrayListOf<EpoxyModel<out Any>>()

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(32)
            .addTo(modelList)

        DashboardShareAppModel_(onShareAppClick)
            .id("share_app")
            .addTo(modelList)

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(32)
            .addTo(modelList)

        verticalBackgroundModelGroup(modelList) {
            id("vertical_model_group_share_app")
            backgroundColor(R.color.background_gray)
        }
    }
}

sealed class CardUpdateStatus {

    object ContactUpdate : CardUpdateStatus()
    object EndOfQuarantine : CardUpdateStatus()
}

private fun ExposureNotificationPhase?.isReportingEnabled(): Boolean {
    return this is FrameworkRunning || this is NotCritical
}