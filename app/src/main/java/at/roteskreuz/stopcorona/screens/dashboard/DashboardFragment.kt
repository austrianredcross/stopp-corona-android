package at.roteskreuz.stopcorona.screens.dashboard

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.screens.dashboard.dialog.AutomaticHandshakeExplanationDialog
import at.roteskreuz.stopcorona.screens.dashboard.dialog.GooglePlayServicesNotAvailableDialog
import at.roteskreuz.stopcorona.screens.dashboard.dialog.MicrophoneExplanationDialog
import at.roteskreuz.stopcorona.screens.handshake.startHandshakeFragment
import at.roteskreuz.stopcorona.screens.history.startContactHistoryFragment
import at.roteskreuz.stopcorona.screens.infection_info.startInfectionInfoFragment
import at.roteskreuz.stopcorona.screens.menu.startMenuFragment
import at.roteskreuz.stopcorona.screens.questionnaire.guideline.startQuestionnaireGuidelineFragment
import at.roteskreuz.stopcorona.screens.questionnaire.selfmonitoring.startQuestionnaireSelfMonitoringWithSubmissionDataFragment
import at.roteskreuz.stopcorona.screens.questionnaire.startQuestionnaireFragment
import at.roteskreuz.stopcorona.screens.reporting.reportStatus.guideline.startCertificateReportGuidelinesFragment
import at.roteskreuz.stopcorona.screens.reporting.startReportingActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.PermissionChecker
import at.roteskreuz.stopcorona.skeleton.core.utils.dipif
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import at.roteskreuz.stopcorona.utils.enableBluetoothForResult
import at.roteskreuz.stopcorona.utils.isBatteryOptimizationIgnored
import at.roteskreuz.stopcorona.utils.shareApp
import at.roteskreuz.stopcorona.utils.startBatteryOptimisationSettingsForResult
import at.roteskreuz.stopcorona.utils.view.AccurateScrollListener
import at.roteskreuz.stopcorona.utils.view.LinearLayoutManagerAccurateOffset
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_dashboard.*
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Sample dashboard.
 */
class DashboardFragment : BaseFragment(R.layout.fragment_dashboard), PermissionChecker {

    companion object {
        private const val REQUEST_MICROPHONE_DIALOG = Constants.Request.REQUEST_DASHBOARD + 1
        private const val REQUEST_BATTERY_OPTIMISATION_ENABLE_DIALOG = Constants.Request.REQUEST_DASHBOARD + 2
        private const val REQUEST_ENABLE_BLUETOOTH_DIALOG = Constants.Request.REQUEST_DASHBOARD + 3
    }

    override val requiredPermissions: List<String>
        get() = listOf(Manifest.permission.ACCESS_COARSE_LOCATION)

    override val askForPermissionOnViewCreated: Boolean
        get() = false

    override val isToolbarVisible: Boolean = true

    override fun getTitle(): String? {
        return "" // blank
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val viewModel: DashboardViewModel by viewModel()

    private val controller: DashboardController by lazy {
        DashboardController(
            context = requireContext(),
            onManualHandshakeClick = {
                checkPlayServicesAvailabilityAndStartHandshakeFragment()
            },
            onAutomaticHandshakeInformationClick = {
                AutomaticHandshakeExplanationDialog().show()
            },
            onSavedEncountersClick = {
                startContactHistoryFragment()
            },
            onFeelingClick = {
                startQuestionnaireFragment()
            },
            onReportClick = {
                startReportingActivity(MessageType.InfectionLevel.Red)
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
            onRevokeSuspicionClick = {
                startReportingActivity(MessageType.Revoke.Suspicion)
            },
            onPresentMedicalReportClick = {
                startReportingActivity(MessageType.InfectionLevel.Red)
            },
            onCheckSymptomsAgainClick = {
                startQuestionnaireFragment()
            },
            onSomeoneHasRecoveredCloseClick = viewModel::someoneHasRecoveredSeen,
            onQuarantineEndCloseClick = viewModel::quarantineEndSeen,
            onAutomaticHandshakeEnabled = ::checkDependenciesAndStartAutomaticHandshake,
            onShareAppClick = {
                shareApp()
            },
            onRevokeSicknessClick = {
                startReportingActivity(MessageType.Revoke.Sickness)
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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(contentRecyclerView) {
            setController(controller)
            layoutManager = LinearLayoutManagerAccurateOffset(requireContext(), accurateScrollListener)
            addOnScrollListener(accurateScrollListener)
        }

        disposables += viewModel.observeSavedEncounters()
            .observeOnMainThread()
            .subscribe { savedEncounters ->
                controller.savedEncounters = savedEncounters
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

        disposables += viewModel.observeAutomaticHandshake()
            .observeOnMainThread()
            .subscribe { enabled ->
                controller.automaticHandshakeEnabled = enabled
            }

        /**
         * If the user starts the app for the first time the service will be started automatically
         */
        if (viewModel.wasServiceEnabledAutomaticallyOnFirstStart.not()) {
            viewModel.wasServiceEnabledAutomaticallyOnFirstStart = true
            checkDependenciesAndStartAutomaticHandshake(true)
        }

        controller.requestModelBuild()
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

    private fun checkPlayServicesAvailabilityAndStartHandshakeFragment() {
        when (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext())) {
            ConnectionResult.SUCCESS -> {
                if (viewModel.showMicrophoneExplanationDialog) {
                    // will call startHandshakeFragment() if OK
                    MicrophoneExplanationDialog().showForResult(REQUEST_MICROPHONE_DIALOG)
                } else {
                    startHandshakeFragment()
                }
            }
            else -> {
                GooglePlayServicesNotAvailableDialog().show()
            }
        }
    }

    private fun checkDependenciesAndStartAutomaticHandshake(isEnabled: Boolean) {
        when {
            isEnabled && checkAllPermissionsGranted(requireContext()).not() -> {
                checkPermissions()
            }
            isEnabled && bluetoothAdapter != null && bluetoothAdapter.isEnabled.not() -> {
                enableBluetoothForResult(REQUEST_ENABLE_BLUETOOTH_DIALOG)
            }
            isEnabled && requireContext().isBatteryOptimizationIgnored().not() && viewModel.batteryOptimizationDialogShown.not() -> {
                viewModel.batteryOptimizationDialogShown = true
                requireActivity().startBatteryOptimisationSettingsForResult(REQUEST_BATTERY_OPTIMISATION_ENABLE_DIALOG)
            }
            else -> {
                viewModel.onAutomaticHandshakeEnabled(isEnabled)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_MICROPHONE_DIALOG -> {
                if (resultCode == Activity.RESULT_OK) {
                    startHandshakeFragment()
                }
            }
            REQUEST_BATTERY_OPTIMISATION_ENABLE_DIALOG -> {
                checkDependenciesAndStartAutomaticHandshake(true)
            }
            REQUEST_ENABLE_BLUETOOTH_DIALOG -> {
                checkDependenciesAndStartAutomaticHandshake(bluetoothAdapter?.isEnabled == true)
            }
        }
    }

    override fun onPermissionGranted(permission: String) {
        checkDependenciesAndStartAutomaticHandshake(true)
    }
}