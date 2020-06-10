package at.roteskreuz.stopcorona.screens.debug.exposure_notification_infection_messages

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.model.entities.infection.info.WarningType
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.screens.reporting.reportStatus.ResolutionType
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataState
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.debug_contact_tracing_fragment.*
import kotlinx.android.synthetic.main.debug_contact_tracing_fragment.exposureNotificationsErrorMessage
import kotlinx.android.synthetic.main.debug_contact_tracing_fragment.exposureNotificationsMasterSwitch
import kotlinx.android.synthetic.main.debug_contact_tracing_fragment.exposureNotificationsSettingsButton
import kotlinx.android.synthetic.main.debug_contact_tracing_fragment.googlePlayServicesVersionTextView
import kotlinx.android.synthetic.main.debug_contact_tracing_tracking_keys_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class DebugExposureNotificationsTrackingKeysFragment : BaseFragment(R.layout.debug_contact_tracing_tracking_keys_fragment) {

    companion object {
        private const val REQUEST_CODE_REGISTER_WITH_FRAMEWORK = Constants.Request.REQUEST_REPORTING_STATUS_FRAGMENT + 1
    }

    private var listenerActive: Boolean = false
    private val viewModel: DebugExposureNotificationsTrackingKeysViewModel by viewModel()

    override val isToolbarVisible: Boolean
        get() = true

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        exposureNotificationsSettingsButton.setOnClickListener { viewModel.jumpToSystemSettings() }

        exposureNotificationsTracingKeysDownloadIndexButton.setOnClickListener { viewModel.downloadTracingKeysIndex() }


        googlePlayServicesVersionTextView.text = viewModel.googlePlayServicesVersion()

        disposables += viewModel.observeEnabledState()
            .observeOnMainThread()
            .subscribe {
                listenerActive = false
                exposureNotificationsMasterSwitch.isChecked = it
                listenerActive = true
            }

        disposables += viewModel.observeResultionErrorReasons()
            .observeOnMainThread()
            .subscribe {
                exposureNotificationsErrorMessage.text = it
            }


        disposables += viewModel.observeResolutionError()
            .observeOnMainThread()
            .subscribe { state ->
                when (state) {
                    is State.Loading -> {
                        //TODO think about what to do here
                    }
                    is DataState.Loaded -> {
                        when (state.data){
                            is ResolutionType.RegisterWithFramework -> {
                                state.data.status.startResolutionForResult(activity, REQUEST_CODE_REGISTER_WITH_FRAMEWORK)
                            }
                        }
                    }
                }
            }

        exposureNotificationsMasterSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (listenerActive.not()) {
                return@setOnCheckedChangeListener
            }
            if (isChecked) {
                activity?.let { viewModel.startExposureNotifications(it) }
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
        return "Exposure Tracing Tracking Keys"
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

fun Activity.startDebugExposureNotificationsTracingKeysFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = DebugExposureNotificationsTrackingKeysFragment::class.java.name
    )
}