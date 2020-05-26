package at.roteskreuz.stopcorona.screens.debug.exposure_notifications

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import kotlinx.android.synthetic.main.debug_contact_tracing_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class DebugExposureNotificationsFragment : BaseFragment(R.layout.debug_contact_tracing_fragment) {

    private val viewModel: DebugExposureNotificationsViewModel by viewModel()

    override val isToolbarVisible: Boolean
        get() = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        exposureNotificationsSettingsButton.setOnClickListener { viewModel.jumpToSystemSettings() }

        viewModel.checkEnabledState()

        googlePlayServicesVersionTextView.text = viewModel.googlePlayServicesVersion()

        exposureNotificationsMasterSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked){
                viewModel.startExposureNotifications()
            } else {
                viewModel.stopExposureNotifications()
            }
        }
    }

    override fun getTitle(): String? {
        return "Exposure Tracing"
    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
    }
}

fun Activity.startDebugExposureNotificationsFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = DebugExposureNotificationsFragment::class.java.name
    )
}