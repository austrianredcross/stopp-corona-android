package at.roteskreuz.stopcorona.screens.dashboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.exceptions.handleBaseCoronaErrors
import at.roteskreuz.stopcorona.model.managers.ExposureNotificationPhase.FrameworkError
import at.roteskreuz.stopcorona.model.managers.ExposureNotificationPhase.PrerequisitesError
import at.roteskreuz.stopcorona.screens.dashboard.changelog.showChangelogBottomSheetFragment
import at.roteskreuz.stopcorona.screens.dashboard.privacy_update.showPrivacyUpdateFragment
import at.roteskreuz.stopcorona.screens.infection_info.startInfectionInfoFragment
import at.roteskreuz.stopcorona.screens.mandatory_update.showMandatoryUpdateFragment
import at.roteskreuz.stopcorona.screens.menu.startMenuFragment
import at.roteskreuz.stopcorona.screens.questionnaire.guideline.startQuestionnaireGuidelineFragment
import at.roteskreuz.stopcorona.screens.questionnaire.report.startReportSuspicionFragment
import at.roteskreuz.stopcorona.screens.questionnaire.selfmonitoring.startQuestionnaireSelfMonitoringWithSubmissionDataFragment
import at.roteskreuz.stopcorona.screens.questionnaire.startQuestionnaireFragment
import at.roteskreuz.stopcorona.screens.reporting.reportStatus.guideline.startCertificateReportGuidelinesFragment
import at.roteskreuz.stopcorona.screens.reporting.startReportingActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.dipif
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import at.roteskreuz.stopcorona.utils.shareApp
import at.roteskreuz.stopcorona.utils.startBatteryOptimisationSettingsForResult
import at.roteskreuz.stopcorona.utils.startDialogToEnableBluetooth
import at.roteskreuz.stopcorona.utils.startGooglePlayStore
import at.roteskreuz.stopcorona.utils.view.AccurateScrollListener
import at.roteskreuz.stopcorona.utils.view.LinearLayoutManagerAccurateOffset
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_dashboard.*
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Sample dashboard.
 */
class DashboardFragment : BaseFragment(R.layout.fragment_dashboard) {

    companion object {

        private const val REQUEST_CODE_EXPOSURE_NOTIFICATION_RESOLUTION_REQUIRED = Constants.Request.REQUEST_DASHBOARD + 1
        private const val REQUEST_CODE_GOOGLE_PLAY_SERVICES_RESOLVE_ACTION = Constants.Request.REQUEST_DASHBOARD + 2
        private const val REQUEST_BATTERY_OPTIMISATION_ENABLE_DIALOG = Constants.Request.REQUEST_DASHBOARD + 3
    }

    override val isToolbarVisible: Boolean = true

    override fun getTitle(): String? {
        return "" // blank
    }

    private val viewModel: DashboardViewModel by viewModel()

    private val controller: DashboardController by lazy {
        DashboardController(
            context = requireContext(),
            onAutomaticHandshakeInformationClick = {
                startHandshakeExplanationFragment()
            },
            onStartQuestionnaireClick = { disabled ->
                if (disabled) {
                    Snackbar.make(requireView(), R.string.main_reporting_disable_btn, Snackbar.LENGTH_LONG).show()
                } else {
                    startQuestionnaireFragment()
                }
            },
            onReportSuspicionClick = { disabled ->
                if (disabled) {
                    Snackbar.make(requireView(), R.string.main_reporting_disable_btn, Snackbar.LENGTH_LONG).show()
                } else {
                    startReportSuspicionFragment()
                }
            },
            onReportClick = { disabled ->
                if (disabled) {
                    Snackbar.make(requireView(), R.string.main_reporting_disable_btn, Snackbar.LENGTH_LONG).show()
                } else {
                    startReportingActivity(MessageType.InfectionLevel.Red)
                }
            },
            onHealthStatusClick = { healthStatusData ->
                when (healthStatusData) {
                    is HealthStatusData.SicknessCertificate -> {
                        startCertificateReportGuidelinesFragment()
                    }
                    HealthStatusData.SelfTestingSymptomsMonitoring -> {
                        startQuestionnaireSelfMonitoringWithSubmissionDataFragment()
                    }
                    is HealthStatusData.SelfTestingSuspicionOfSickness -> {
                        startQuestionnaireGuidelineFragment()
                    }
                    is HealthStatusData.ContactsSicknessInfo -> {
                        startInfectionInfoFragment()
                    }
                }
            },
            onRevokeSuspicionClick = { disabled ->
                if (disabled) {
                    Snackbar.make(requireView(), R.string.main_reporting_disable_btn, Snackbar.LENGTH_LONG).show()
                } else {
                    startReportingActivity(MessageType.Revoke.Suspicion)
                }
            },
            onPresentMedicalReportClick = { disabled ->
                if (disabled) {
                    Snackbar.make(requireView(), R.string.main_reporting_disable_btn, Snackbar.LENGTH_LONG).show()
                } else {
                    startReportingActivity(MessageType.InfectionLevel.Red)
                }
            },
            onCheckSymptomsAgainClick = { disabled ->
                if (disabled) {
                    Snackbar.make(requireView(), R.string.main_reporting_disable_btn, Snackbar.LENGTH_LONG).show()
                } else {
                    startQuestionnaireFragment()
                }
            },
            onSomeoneHasRecoveredCloseClick = viewModel::someoneHasRecoveredSeen,
            onQuarantineEndCloseClick = viewModel::quarantineEndSeen,
            onAutomaticHandshakeEnabled = viewModel::userWantsToRegisterAppForExposureNotifications::set,
            onExposureNotificationErrorActionClick = { exposureNotificationPhase ->
                when (exposureNotificationPhase) {
                    is PrerequisitesError.UnavailableGooglePlayServices -> {
                        exposureNotificationPhase.googlePlayAvailability.getErrorDialog(
                            requireActivity(),
                            exposureNotificationPhase.googlePlayServicesStatusCode,
                            REQUEST_CODE_GOOGLE_PLAY_SERVICES_RESOLVE_ACTION
                        ).show()
                    }
                    is PrerequisitesError.InvalidVersionOfGooglePlayServices -> {
                        startGooglePlayStore(Constants.ExposureNotification.GOOGLE_PLAY_SERVICES_PACKAGE_NAME)
                    }
                    is PrerequisitesError.BatteryOptimizationsNotIgnored -> {
                        startBatteryOptimisationSettingsForResult(REQUEST_BATTERY_OPTIMISATION_ENABLE_DIALOG)
                    }
                    is FrameworkError.NotCritical.BluetoothNotEnabled -> {
                        startDialogToEnableBluetooth()
                    }
                    is FrameworkError -> {
                        exposureNotificationPhase.refresh()
                    }
                }
            },
            onShareAppClick = {
                shareApp()
            },
            onRevokeSicknessClick = { disabled ->
                if (disabled) {
                    Snackbar.make(
                        requireView(),
                        R.string.main_reporting_disable_btn,
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    startReportingActivity(MessageType.Revoke.Sickness)
                }
            },
            onUploadMissingExposureKeysClick = { disabled, uploadMissingExposureKeys ->
                if (disabled) {
                    Snackbar.make(
                        requireView(),
                        R.string.main_reporting_disable_btn,
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    startReportingActivity(
                        messageType = uploadMissingExposureKeys.messageType,
                        dateWithMissingExposureKeys = uploadMissingExposureKeys.missingExposureKeyDate,
                        displayUploadMissingExposureKeysExplanation = true
                    )
                }
            }
        )
    }

    private val accurateScrollListener by lazy {
        AccurateScrollListener(
            onScroll = { scrolledDistance ->
                transparentAppBar.elevation = if (scrolledDistance > 0) {
                    requireContext().dipif(4)
                } else {
                    0f
                }
            }
        )
    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        toolbar?.setNavigationIcon(R.drawable.ic_drawer)
        toolbar?.setNavigationContentDescription(R.string.start_menu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(contentRecyclerView) {
            setController(controller)
            layoutManager = LinearLayoutManagerAccurateOffset(requireContext(), accurateScrollListener)
            addOnScrollListener(accurateScrollListener)
        }

        disposables += viewModel.observeDateOfFirstMedicalConfirmation()
            .observeOnMainThread()
            .subscribe { dateOfFirstMedicalConfirmation ->
                controller.dateOfFirstMedicalConfirmation = dateOfFirstMedicalConfirmation.orElse(null)
            }

        disposables += viewModel.observeOwnHealthStatus()
            .observeOnMainThread()
            .subscribe {
                controller.ownHealthStatus = it
            }

        disposables += viewModel.observeContactsHealthStatus()
            .observeOnMainThread()
            .subscribe {
                controller.contactsHealthStatus = it
            }

        disposables += viewModel.observeShowQuarantineEnd()
            .observeOnMainThread()
            .subscribe {
                controller.showQuarantineEnd = it
            }

        disposables += viewModel.observeSomeoneHasRecoveredStatus()
            .observeOnMainThread()
            .subscribe {
                controller.someoneHasRecoveredHealthStatus = it
            }

        disposables += viewModel.observeExposureNotificationPhase()
            .observeOnMainThread()
            .subscribe { phase ->
                controller.exposureNotificationPhase = phase
                when (phase) {
                    is FrameworkError.Critical.ResolutionRequired -> {
                        phase.exception.status.startResolutionForResult(
                            requireActivity(),
                            REQUEST_CODE_EXPOSURE_NOTIFICATION_RESOLUTION_REQUIRED
                        )
                    }
                    is FrameworkError.Critical.Unknown -> handleBaseCoronaErrors(phase.exception)
                }
            }

        disposables += viewModel.observeIfUploadOfMissingExposureKeysIsNeeded()
            .observeOnMainThread()
            .subscribe {
                val uploadMissingExposureKeys = it.orElse(null)
                if (uploadMissingExposureKeys?.shouldUploadNow == true) {
                    controller.uploadMissingExposureKeys = uploadMissingExposureKeys
                } else {
                    controller.uploadMissingExposureKeys = null
                }
            }

        disposables += viewModel.observeCurrentChangelogState()
            .observeOnMainThread()
            .subscribe { changelogState ->
                if (changelogState is ChangelogState.CurrentChangelogSeen) {
                    /**
                     * If the user starts the app for the first time the exposure notification framework will be started automatically.
                     */
                    if (viewModel.wasExposureFrameworkAutomaticallyEnabledOnFirstStart.not()) {
                        viewModel.wasExposureFrameworkAutomaticallyEnabledOnFirstStart = true
                        viewModel.userWantsToRegisterAppForExposureNotifications = true
                    }
                }
            }

        disposables += viewModel.observeLastContactDate()
            .observeOnMainThread()
            .subscribe { lastContactDates ->
                controller.dateOfLastContact = when {
                    lastContactDates.lastRedContactDate.isPresent && lastContactDates.lastYellowContactDate.isPresent -> {
                        if (lastContactDates.lastRedContactDate.get().isAfter(lastContactDates.lastYellowContactDate.get())){
                            lastContactDates.lastRedContactDate.get()
                        } else {
                            lastContactDates.lastYellowContactDate.get()
                        }
                    }
                    lastContactDates.lastRedContactDate.isPresent -> lastContactDates.lastRedContactDate.get()
                    lastContactDates.lastYellowContactDate.isPresent -> lastContactDates.lastYellowContactDate.get()
                    else -> null
                }
            }

        disposables += viewModel.observeLastKeyRequestDate()
            .observeOnMainThread()
            .subscribe { lastKeyRequestDate ->
                controller.dateOfLastKeyRequest = lastKeyRequestDate.orElse(null)

                val keyRequestCountLastWeek = viewModel.getKeyRequestCountLastWeek()
                controller.keyRequestCountLastWeek = keyRequestCountLastWeek
            }
        

        if (viewModel.shouldDisplayWhatsNew) {
            showChangelogBottomSheetFragment()
        }

        if (!viewModel.hasAcceptedPrivacyUpdate) {
            showPrivacyUpdateFragment()
        }

        disposables += viewModel.observeDisplayMandatoryUpdate()
            .observeOnMainThread()
            .subscribe { displayMandatoryUpdate ->
                if (displayMandatoryUpdate) {
                    showMandatoryUpdateFragment()
                }
            }

        controller.requestModelBuild()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPrerequisitesErrorStatement(ignoreErrors = true)
    }

    override fun onDestroyView() {
        contentRecyclerView.removeOnScrollListener(accurateScrollListener)
        super.onDestroyView()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                startMenuFragment()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_EXPOSURE_NOTIFICATION_RESOLUTION_REQUIRED -> {
                if (resultCode == Activity.RESULT_OK) {
                    viewModel.onExposureNotificationRegistrationResolutionResultOk()
                } else {
                    viewModel.onExposureNotificationRegistrationResolutionResultNotOk()
                }
            }
            REQUEST_CODE_GOOGLE_PLAY_SERVICES_RESOLVE_ACTION -> {
                if (resultCode == Activity.RESULT_OK) {
                    viewModel.refreshPrerequisitesErrorStatement()
                }
            }
            REQUEST_BATTERY_OPTIMISATION_ENABLE_DIALOG -> {
                viewModel.refreshPrerequisitesErrorStatement()
            }
        }
    }
}