package at.roteskreuz.stopcorona.screens.savedIDs

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import kotlinx.android.synthetic.main.info_delete_exposure_keys_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

/**
 * Screen to inform the user about how he can delete his exposure keys.
 */
class InfoDeleteExposureKeysFragment : BaseFragment(R.layout.info_delete_exposure_keys_fragment) {

    override val isToolbarVisible: Boolean = true

    private val viewModel: InfoDeleteExposureKeysViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnDisplayExposureNotificationsSettings.setOnClickListener {
            displayExposureNotificationsSettings()
        }
    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
        toolbar?.setNavigationContentDescription(R.string.general_back)
    }

    override fun getTitle(): String? {
        return getString(R.string.info_delete_exposure_keys_title)
    }

    private fun displayExposureNotificationsSettings() {
        val notificationsSettingsIntent = viewModel.getExposureNotificationsSettingsIntent()
        if (notificationsSettingsIntent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(notificationsSettingsIntent)
        } else {
            Toast.makeText(
                requireContext(),
                R.string.info_delete_exposure_keys_settings_cannot_be_opened,
                Toast.LENGTH_SHORT
            ).show()
            Timber.e(SilentError("Exposure notifications settings intent could not be resolved."))
        }
    }

}

fun Fragment.startInfoDeleteExposureKeysFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = InfoDeleteExposureKeysFragment::class.java.name
    )
}