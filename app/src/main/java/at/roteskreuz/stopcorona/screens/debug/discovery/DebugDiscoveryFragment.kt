package at.roteskreuz.stopcorona.screens.debug.discovery

import android.Manifest
import android.app.Activity
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.repositories.CryptoRepository
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.PermissionChecker
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.threeten.bp.format.DateTimeFormatter

class DummyDiscoveryFragment : BaseFragment(R.layout.debug_discovery_fragment), PermissionChecker, KoinComponent {

    private val cryptoRepository: CryptoRepository by inject()

    private val timestampFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    override val requiredPermissions: List<String>
        get() = listOf(Manifest.permission.ACCESS_COARSE_LOCATION)

    override val askForPermissionOnViewCreated: Boolean
        get() = true

    override fun onPermissionGranted(permission: String) {
        // do nothing
    }

    private val viewModel: DebugDiscoveryViewModel by viewModel()

    override val isToolbarVisible: Boolean
        get() = true

    override fun getTitle(): String? {
        return "BT Discovery"
    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
    }
}

fun Activity.startDebugDiscoveryFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = DummyDiscoveryFragment::class.java.name
    )
}
