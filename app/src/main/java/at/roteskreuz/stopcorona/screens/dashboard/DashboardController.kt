package at.roteskreuz.stopcorona.screens.dashboard

import android.content.Context
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.epoxy.EmptySpaceModel_
import at.roteskreuz.stopcorona.screens.base.epoxy.additionalInformation
import at.roteskreuz.stopcorona.screens.base.epoxy.buttons.ButtonType2Model_
import at.roteskreuz.stopcorona.screens.base.epoxy.emptySpace
import at.roteskreuz.stopcorona.screens.base.epoxy.verticalBackgroundModelGroup
import at.roteskreuz.stopcorona.screens.dashboard.ExposureNotificationPhase.*
import at.roteskreuz.stopcorona.screens.dashboard.epoxy.*
import at.roteskreuz.stopcorona.skeleton.core.utils.adapterProperty
import at.roteskreuz.stopcorona.skeleton.core.utils.addTo
import at.roteskreuz.stopcorona.utils.string
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyModel

/**
 * Contents of the dashboard.
 */
class DashboardController(
    private val context: Context,
    private val onAutomaticHandshakeInformationClick: () -> Unit,
    private val onFeelingClick: () -> Unit,
    private val onReportClick: () -> Unit,
    private val onHealthStatusClick: (data: HealthStatusData) -> Unit,
    private val onRevokeSuspicionClick: () -> Unit,
    private val onPresentMedicalReportClick: () -> Unit,
    private val onCheckSymptomsAgainClick: () -> Unit,
    private val onSomeoneHasRecoveredCloseClick: () -> Unit,
    private val onQuarantineEndCloseClick: () -> Unit,
    private val onAutomaticHandshakeEnabled: (isEnabled: Boolean) -> Unit,
    private val refreshAutomaticHandshakeErrors: (ExposureNotificationPhase) -> Unit,
    private val onRevokeSicknessClick: () -> Unit,
    private val onShareAppClick: () -> Unit
) : EpoxyController() {

    var ownHealthStatus: HealthStatusData by adapterProperty(HealthStatusData.NoHealthStatus)
    var contactsHealthStatus: HealthStatusData by adapterProperty(HealthStatusData.NoHealthStatus)
    var showQuarantineEnd: Boolean by adapterProperty(false)
    var someoneHasRecoveredHealthStatus: HealthStatusData by adapterProperty(HealthStatusData.NoHealthStatus)
    var exposureNotificationPhase: ExposureNotificationPhase? by adapterProperty(null as ExposureNotificationPhase?)

    override fun buildModels() {
        emptySpace(modelCountBuiltSoFar, 16)

        /**
         * Build all cards for own and contact health status as well as status updates
         */
        if (ownHealthStatus != HealthStatusData.NoHealthStatus ||
            contactsHealthStatus != HealthStatusData.NoHealthStatus ||
            showQuarantineEnd ||
            someoneHasRecoveredHealthStatus != HealthStatusData.NoHealthStatus) {

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
                contactsHealthStatus != HealthStatusData.NoHealthStatus) {
                emptySpace(modelCountBuiltSoFar, 16)
            }

            /**
             * Build card for contacts health status if available
             */
            if (contactsHealthStatus != HealthStatusData.NoHealthStatus) {
                buildContactHealthStatus()
            }

            /**
             * Add single space if own OR contact health state are available AND someone has recovered
             */
            if ((ownHealthStatus != HealthStatusData.NoHealthStatus ||
                    contactsHealthStatus != HealthStatusData.NoHealthStatus) &&
                someoneHasRecoveredHealthStatus == HealthStatusData.SomeoneHasRecovered) {
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
                showQuarantineEnd) {
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
                showQuarantineEnd) {
                emptySpace(modelCountBuiltSoFar, 32)
            }
        }

        handshakeHeadline {
            id("handshake_title")
            title(context.string(R.string.main_body_contact_title))
        }

        emptySpace(modelCountBuiltSoFar, 16)

        // needed to have two caches of epoxy models because of lottie
        if (exposureNotificationPhase is FrameworkRunning) {
            handshakeImage {
                id("handshake_image_active")
                active(true)
            }
        } else {
            handshakeImage {
                id("handshake_image_inactive")
                active(false)
            }
        }

        emptySpace(modelCountBuiltSoFar, 16)

        automaticHandshakeSwitch(onAutomaticHandshakeEnabled) {
            id("automatic_handshake_switch")
            phase(exposureNotificationPhase)
            // TODO: 03/06/2020 dusanjencik: Do we need to disable it?
//            enabled((ownHealthStatus is HealthStatusData.SicknessCertificate).not())
        }

        emptySpace(modelCountBuiltSoFar, 16)

        if (exposureNotificationPhase != null) {
            when (exposureNotificationPhase) {
                // TODO: 04/06/2020 dusanjencik: Add the correct card and update the content appropriately
                is PrerequisitesError.UnavailableGooglePlayServices -> {
                    statusUpdate({ refreshAutomaticHandshakeErrors(exposureNotificationPhase!!) }) {
                        id("UnavailableGooglePlayServices")
                        title("UnavailableGooglePlayServices")
                        cardStatus(CardUpdateStatus.ContactUpdate)
                    }
                }
                is PrerequisitesError.InvalidVersionOfGooglePlayServices -> {
                    statusUpdate({ refreshAutomaticHandshakeErrors(exposureNotificationPhase!!) }) {
                        id("InvalidVersionOfGooglePlayServices")
                        title("InvalidVersionOfGooglePlayServices")
                        cardStatus(CardUpdateStatus.ContactUpdate)
                    }
                }
                is FrameworkError.Unknown -> {
                    statusUpdate({ refreshAutomaticHandshakeErrors(exposureNotificationPhase!!) }) {
                        id("FrameworkError.Unknown")
                        title("FrameworkError.Unknown")
                        cardStatus(CardUpdateStatus.ContactUpdate)
                    }
                }
                is FrameworkError -> {
                    statusUpdate({ refreshAutomaticHandshakeErrors(exposureNotificationPhase!!) }) {
                        id("FrameworkError." + exposureNotificationPhase!!.javaClass.simpleName)
                        title("FrameworkError." + exposureNotificationPhase!!.javaClass.simpleName)
                        cardStatus(CardUpdateStatus.ContactUpdate)
                    }
                }
            }
        }

        emptySpace(modelCountBuiltSoFar, 16)

        additionalInformation(onAutomaticHandshakeInformationClick) {
            id("handshake_additional_information")
            title(context.string(R.string.main_automatic_handshake_information_hint))
        }

        emptySpace(modelCountBuiltSoFar, 16)

        buildShareAppCard()

        if ((ownHealthStatus is HealthStatusData.SelfTestingSuspicionOfSickness).not()
            && (ownHealthStatus is HealthStatusData.SicknessCertificate).not()
        ) {
            emptySpace(modelCountBuiltSoFar, 16)

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
                    ButtonType2Model_(onFeelingClick)
                        .id("feel_button")
                        .text(context.string(R.string.main_button_feel_today_button)),
                    EmptySpaceModel_()
                        .id(modelCountBuiltSoFar)
                        .height(40)
                )
            ) {
                backgroundColor(R.color.white)
            }
        }

        if ((ownHealthStatus is HealthStatusData.SicknessCertificate).not()) {

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
                    ButtonType2Model_(onReportClick)
                        .id("report_button")
                        .text(context.string(R.string.main_body_report_button)),
                    EmptySpaceModel_()
                        .id(modelCountBuiltSoFar)
                        .height(40)
                )
            ) {
                backgroundColor(R.color.background_gray)
            }
        } else {
            emptySpace(modelCountBuiltSoFar, 40)
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

            ButtonType2Model_(onRevokeSuspicionClick)
                .id("own_health_status_present_revoke_suspicion")
                .text(context.string(R.string.self_testing_suspicion_button_revoke))
                .addTo(modelList)

            EmptySpaceModel_()
                .id(modelCountBuiltSoFar)
                .height(16)
                .addTo(modelList)

            ButtonType2Model_(onPresentMedicalReportClick)
                .id("own_health_status_present_medical_report_button")
                .text(context.string(R.string.self_testing_suspicion_secondary_button))
                .addTo(modelList)
        }

        if (ownHealthStatus is HealthStatusData.SelfTestingSymptomsMonitoring) {
            EmptySpaceModel_()
                .id(modelCountBuiltSoFar)
                .height(16)
                .addTo(modelList)

            ButtonType2Model_(onCheckSymptomsAgainClick)
                .id("own_health_status_check_symptoms_button")
                .text(context.string(R.string.self_testing_symptoms_secondary_button))
                .addTo(modelList)
        }

        if (ownHealthStatus is HealthStatusData.SicknessCertificate) {
            EmptySpaceModel_()
                .id(modelCountBuiltSoFar)
                .height(16)
                .addTo(modelList)

            ButtonType2Model_(onRevokeSicknessClick)
                .id("own_health_status_revoke_sickness")
                .text(context.string(R.string.sickness_certificate_attest_revoke))
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
    private fun buildContactHealthStatus() {
        val modelList = arrayListOf<EpoxyModel<out Any>>()

        EmptySpaceModel_()
            .id(modelCountBuiltSoFar)
            .height(32)
            .addTo(modelList)

        HealthStatusModel_(onHealthStatusClick)
            .id("contacts_health_status")
            .data(contactsHealthStatus)
            .ownHealthStatus(ownHealthStatus)
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
