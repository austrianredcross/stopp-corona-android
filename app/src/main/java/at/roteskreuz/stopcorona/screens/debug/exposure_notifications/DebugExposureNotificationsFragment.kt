package at.roteskreuz.stopcorona.screens.debug.exposure_notifications

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.entities.infection.info.WarningType
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataState
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.debug_contact_tracing_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class DebugExposureNotificationsFragment : BaseFragment(R.layout.debug_contact_tracing_fragment) {

    private var listenerActive: Boolean = false
    private val viewModel: DebugExposureNotificationsViewModel by viewModel()

    override val isToolbarVisible: Boolean
        get() = true

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        exposureNotificationsSettingsButton.setOnClickListener { viewModel.jumpToSystemSettings() }

        exposureNotificationsUploadTemporaryExposureKeysGreenButton.setOnClickListener { viewModel.uploadKeys(WarningType.REVOKE) }
        exposureNotificationsUploadTemporaryExposureKeysRedButton.setOnClickListener { viewModel.uploadKeys(WarningType.RED) }
        exposureNotificationsUploadTemporaryExposureKeysYellowButton.setOnClickListener { viewModel.uploadKeys(WarningType.YELLOW) }

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

        var uploadButtons = listOf(exposureNotificationsUploadTemporaryExposureKeysGreenButton,
            exposureNotificationsUploadTemporaryExposureKeysRedButton,
            exposureNotificationsUploadTemporaryExposureKeysYellowButton
        )
        disposables+= viewModel.observeLastTemporaryExposureKeys()
            .observeOnMainThread()
            .subscribe{keys ->
                uploadButtons.onEach { it.text = "${keys.size} keys ready to be uploaded" }

            }

        disposables += viewModel.observeResolutionError()
            .observeOnMainThread()
            .subscribe { state ->
                when (state) {
                    is State.Loading -> {
                        //TODO think about what to do here
                    }
                    is DataState.Loaded -> {
                        state.data.first.startResolutionForResult(
                            activity, state.data.second.requestCode()

                        );
                    }
                }
            }

        exposureNotificationsGetTemporaryExposureKeyHistoryButton.setOnClickListener{
            viewModel.getTemporaryExposureKeyHistory()
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
        return "Exposure Tracing"
    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            DebugExposureNotificationsViewModel.DebugAction.REGISTER_WITH_FRAMEWORK.requestCode() -> {
                if (resultCode == Activity.RESULT_OK) {
                    activity?.let { viewModel.resolutionForRegistrationSucceeded(it) }
                }
                else {
                    viewModel.resolutionForRegistrationFailed(resultCode)
                }
            }
            DebugExposureNotificationsViewModel.DebugAction.REQUEST_EXPOSURE_KEYS.requestCode() -> {
                if (resultCode == Activity.RESULT_OK) {
                    viewModel.resolutionForExposureKeyHistorySucceded()
                }
                else {
                    viewModel.resolutionForExposureKeyHistoryFailed(resultCode)
                }
            }
        }
    }
}

fun Activity.startDebugExposureNotificationsFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = DebugExposureNotificationsFragment::class.java.name
    )
}