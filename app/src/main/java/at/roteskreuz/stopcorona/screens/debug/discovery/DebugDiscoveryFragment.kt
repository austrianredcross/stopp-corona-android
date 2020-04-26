package at.roteskreuz.stopcorona.screens.debug.discovery

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.repositories.CryptoRepository
import at.roteskreuz.stopcorona.model.repositories.DiscoveryResult
import at.roteskreuz.stopcorona.model.repositories.P2PKitState
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.PermissionChecker
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.debug_discovery_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.threeten.bp.ZonedDateTime
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        disposables += viewModel.observeP2PKitState()
            .observeOnMainThread()
            .filter { it.isPresent }
            .subscribe { p2pKitState ->
                val timestamp = ZonedDateTime.now().format(timestampFormatter)
                when (p2pKitState.get()) {
                    is P2PKitState.Enabled -> txtResult.append("$timestamp - Enabled (${cryptoRepository.publicKeyPrefix})\n")
                    is P2PKitState.Disabled.Disabled -> txtResult.append("$timestamp - Disabled\n")
                    is P2PKitState.Disabled.Error -> txtResult.append("$timestamp - Error\n")
                    is P2PKitState.Disabled.Exception -> txtResult.append("$timestamp - Exception\n")
                }
            }

        disposables += viewModel.observeDiscoveryResult()
            .observeOnMainThread()
            .subscribe { discoveryResult ->
                val timestamp = ZonedDateTime.now().format(timestampFormatter)
                when (discoveryResult) {
                    is DiscoveryResult.PeerDiscovered -> {
                        txtResult.append("$timestamp - Discovered: ${cryptoRepository.getPublicKeyPrefix(
                            discoveryResult.discoveryInfo)}, Strength: ${discoveryResult.proximityStrength}\n")
                    }
                    is DiscoveryResult.ProximityStrengthChanged -> {
                        txtResult.append("$timestamp - Proximity: ${cryptoRepository.getPublicKeyPrefix(
                            discoveryResult.discoveryInfo)}, Strength: ${discoveryResult.proximityStrength}\n")
                    }
                    is DiscoveryResult.PeerLost -> {
                        txtResult.append("$timestamp - Lost: ${cryptoRepository.getPublicKeyPrefix(discoveryResult.discoveryInfo)}\n")
                    }
                    is DiscoveryResult.PeerUpdated -> {
                        txtResult.append("$timestamp - Updated: ${cryptoRepository.getPublicKeyPrefix(
                            discoveryResult.discoveryInfo)}, Strength: ${discoveryResult.proximityStrength}\n")
                    }
                    is DiscoveryResult.StateChanged -> {
                        txtResult.append("$timestamp - StateChanged: ${discoveryResult.state}\n")
                    }
                    else -> txtResult.append("$timestamp - Something else: $discoveryResult\n")
                }
            }
    }
}

fun Activity.startDebugDiscoveryFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = DummyDiscoveryFragment::class.java.name
    )
}
