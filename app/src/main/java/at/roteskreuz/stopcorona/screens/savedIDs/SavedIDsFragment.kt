package at.roteskreuz.stopcorona.screens.savedIDs

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import kotlinx.android.synthetic.main.saved_ids_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Screen to inform the user about how he can delete his exposure keys.
 */
class SavedIDsFragment : BaseFragment(R.layout.saved_ids_fragment) {

    override val isToolbarVisible: Boolean = true

    private val viewModel: SavedIDsViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnDisplayAppSettings.setOnClickListener {
            displayExposureNotificationsSettings()
        }
    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
    }

    override fun getTitle(): String? {
        return getString(R.string.saved_IDs_title)
    }

    private fun displayExposureNotificationsSettings() {
        val notificationsSettingsIntent = viewModel.getExposureNotificationsSettingsIntent()
        if (notificationsSettingsIntent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(notificationsSettingsIntent)
        } else {
            Toast.makeText(
                requireContext(),
                R.string.saved_IDs_notification_exposure_settings_cannot_be_opened,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

}

fun Fragment.startSavedIDsFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = SavedIDsFragment::class.java.name
    )
}