package at.roteskreuz.stopcorona.screens.debug.exposure_notifications

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
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
import at.roteskreuz.stopcorona.utils.isGmsAvailable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.debug_contact_tracing_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class DebugExposureNotificationsFragment : BaseFragment(R.layout.debug_contact_tracing_fragment) {

    companion object {
        private const val REQUEST_CODE_REGISTER_WITH_FRAMEWORK = Constants.Request.REQUEST_REPORTING_STATUS_FRAGMENT + 1
        private const val REQUEST_CODE_REQUEST_EXPOSURE_KEYS = Constants.Request.REQUEST_REPORTING_STATUS_FRAGMENT + 2
    }

    private var listenerActive: Boolean = false
    private val viewModel: DebugExposureNotificationsViewModel by viewModel()

    override val isToolbarVisible: Boolean
        get() = true

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        exposureNotificationsSettingsButton.setOnClickListener { viewModel.jumpToSystemSettings() }
        exposureNotificationsSettingsButton.isVisible = requireContext().isGmsAvailable()

        val uploadKeylistener = View.OnClickListener {button ->
            val tan = exposureNotificationsTanEditText.text.toString()
            if (tan.isBlank()) {
                activity?.let { Toast.makeText(activity, "please add TAN", Toast.LENGTH_SHORT) }
                exposureNotificationsTanEditText.error = "please provide TAN"
                return@OnClickListener
            } else {
                exposureNotificationsTanEditText.error = null
            }
            val warningType = when (button) {
                exposureNotificationsUploadTemporaryExposureKeysGreenButton -> WarningType.GREEN
                exposureNotificationsUploadTemporaryExposureKeysRedButton -> WarningType.RED
                exposureNotificationsUploadTemporaryExposureKeysYellowButton -> WarningType.YELLOW
                else -> throw IllegalArgumentException()
            }

            viewModel.uploadKeys(warningType, tan)
        }
        exposureNotificationsUploadTemporaryExposureKeysGreenButton.setOnClickListener(uploadKeylistener)
        exposureNotificationsUploadTemporaryExposureKeysRedButton.setOnClickListener(uploadKeylistener)
        exposureNotificationsUploadTemporaryExposureKeysYellowButton.setOnClickListener(uploadKeylistener)

        exposureNotificationsTanButton.setOnClickListener { viewModel.requestTan(exposureNotificationsPhoneNumberEditText.text.toString()) }

        servicesVersionTextView.text = viewModel.getServicesVersion(requireContext())

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

        val uploadButtons = listOf(exposureNotificationsUploadTemporaryExposureKeysGreenButton,
            exposureNotificationsUploadTemporaryExposureKeysRedButton,
            exposureNotificationsUploadTemporaryExposureKeysYellowButton
        )
        disposables+= viewModel.observeLastTemporaryExposureKeys()
            .observeOnMainThread()
            .subscribe { keys ->
                uploadButtons.onEach { it.text = "${keys.size} keys ready to be uploaded with key as password" }
            }

        disposables += viewModel.observeResolutionError()
            .observeOnMainThread()
            .subscribe { state ->
                when (state) {
                    is State.Loading -> {

                    }
                    is DataState.Loaded -> {
                        when (state.data){
                            is ResolutionType.GetExposureKeys -> {
                                state.data.status.startResolutionForResult(activity, REQUEST_CODE_REQUEST_EXPOSURE_KEYS)
                            }
                        }
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
        toolbar?.setNavigationContentDescription(R.string.general_back)
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
            REQUEST_CODE_REQUEST_EXPOSURE_KEYS -> {
                if (resultCode == Activity.RESULT_OK) {
                    viewModel.resolutionForExposureKeyHistorySucceded()
                }
                else {
                    viewModel.resolutionForExposureKeyHistoryFailed(resultCode)
                }
            }
            else -> {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }
}

fun Activity.startDebugExposureNotificationsFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = DebugExposureNotificationsFragment::class.java.name
    )
}