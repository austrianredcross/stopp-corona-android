package at.roteskreuz.stopcorona.screens.debug.exposure_notifications

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.Constants.Request.EXPOSURE_NOTIFICATION_DEBUG_FRAGMENT
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

    companion object {
        private const val REQUEST_CODE_START_EXPOSURE_NOTIFICATION =
            EXPOSURE_NOTIFICATION_DEBUG_FRAGMENT + 1;
    }

    private var listenerActive: Boolean = false
    private val viewModel: DebugExposureNotificationsViewModel by viewModel()

    override val isToolbarVisible: Boolean
        get() = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        exposureNotificationsSettingsButton.setOnClickListener { viewModel.jumpToSystemSettings() }

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
                        state.data.startResolutionForResult(
                            activity,
                            REQUEST_CODE_START_EXPOSURE_NOTIFICATION
                        );
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
            REQUEST_CODE_START_EXPOSURE_NOTIFICATION -> {
                if (resultCode == Activity.RESULT_OK) {
                    activity?.let { viewModel.resolutionSucceeded(it) }
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