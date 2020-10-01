package at.roteskreuz.stopcorona.screens.debug.diagnosis_keys

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataState
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.debug_contact_tracing_fragment.exposureNotificationsErrorMessage
import kotlinx.android.synthetic.main.debug_contact_tracing_fragment.exposureNotificationsMasterSwitch
import kotlinx.android.synthetic.main.debug_contact_tracing_fragment.exposureNotificationsSettingsButton
import kotlinx.android.synthetic.main.debug_contact_tracing_fragment.googlePlayServicesVersionTextView
import kotlinx.android.synthetic.main.debug_diagnosis_keys_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class DebugDiagnosisFragment : BaseFragment(R.layout.debug_diagnosis_keys_fragment) {

    companion object {
        private const val REQUEST_CODE_REGISTER_WITH_FRAMEWORK = Constants.Request.REQUEST_REPORTING_STATUS_FRAGMENT + 1
    }

    private var listenerActive: Boolean = false
    private val viewModel: DebugDiagnosisKeysViewModel by viewModel()

    override val isToolbarVisible: Boolean
        get() = true

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        exposureNotificationsSettingsButton.setOnClickListener { viewModel.jumpToSystemSettings() }

        exposureNotificationsTracingKeysDownloadIndexButton.setOnClickListener { viewModel.downloadDiagnosisKeysArchiveIndex() }

        diagnosisKeysGetExposureSummaryButton.setOnClickListener{ viewModel.getExposureSummary() }

        diagnosisKeysGetExposureInformationButton.setOnClickListener { viewModel.getDiagnosisKeysGetExposureInformation() }

        diagnosisKeysBackgroundProcessingButton.setOnClickListener { viewModel.startBackgroundDiagnosisKeysProcessing() }

        googlePlayServicesVersionTextView.text = viewModel.googlePlayServicesVersion()

        disposables += viewModel.observeDiagnosisKeyToken()
            .observeOnMainThread()
            .subscribe{
                diagnosisKeysGetExposureSummaryButton.text = "Get Summary for $it"
                diagnosisKeysGetExposureInformationButton.text = "Get Information for $it"
            }

        disposables += viewModel.observeEnabledState()
            .observeOnMainThread()
            .subscribe {
                listenerActive = false
                exposureNotificationsMasterSwitch.isChecked = it
                listenerActive = true
            }

        disposables += viewModel.observeResolutionErrorReasons()
            .observeOnMainThread()
            .subscribe {
                exposureNotificationsErrorMessage.text = it
            }


        disposables += viewModel.observeResolutionError()
            .observeOnMainThread()
            .subscribe { state ->
                when (state) {
                    is State.Loading -> {

                    }
                    is DataState.Loaded -> {
                        // no resolution handled. Framework must be running already to continue.
                    }
                }
            }

        exposureNotificationsMasterSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (listenerActive.not()) {
                return@setOnCheckedChangeListener
            }
            if (isChecked) {
                activity?.let { viewModel.startExposureNotifications() }
            } else {
                viewModel.stopExposureNotifications()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkEnabledState()
    }

    override fun getTitle(): String? {
        return "Diagnosis Keys Processing"
    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_REGISTER_WITH_FRAMEWORK -> {
                if (resultCode == Activity.RESULT_OK) {
                    activity?.let { viewModel.resolutionForRegistrationSucceeded(it) }
                }
                else {
                    viewModel.resolutionForRegistrationFailed(resultCode)
                }
            }
            else -> {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }
}

fun Activity.startDebugDiagnosisKeysFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = DebugDiagnosisFragment::class.java.name
    )
}