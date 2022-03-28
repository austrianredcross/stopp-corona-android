package at.roteskreuz.stopcorona.screens.routing

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import at.roteskreuz.stopcorona.model.repositories.other.OfflineSyncer
import at.roteskreuz.stopcorona.screens.dashboard.startDashboardActivity
import at.roteskreuz.stopcorona.screens.onboarding.startOnboardingFragment
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.argument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.standalone.KoinComponent
import org.koin.standalone.get

/**
 * Initial activity to route to other activities.
 *
 * Routing strategies:
 * 1) onboarding on first run
 * 2) main activity else.
 */
class RouterActivity : FragmentActivity(), KoinComponent {

    companion object {
        /**
         * Duration of how long the splashscreen must be shown at least.
         */
        const val SCREEN_DISPLAY_DURATION = 1_000L // ms
        private const val ARGUMENT_SKIP_SPLASHSCREEN_DELAY = "skip_splashscreen_delay"

        fun args(skipSplashscreenDelay: Boolean): Bundle {
            return bundleOf(
                ARGUMENT_SKIP_SPLASHSCREEN_DELAY to skipSplashscreenDelay
            )
        }
    }

    private val viewModel: RouterViewModel by viewModel()

    private val skipSplashscreenDelay: Boolean by argument(ARGUMENT_SKIP_SPLASHSCREEN_DELAY, false)

    private val appDispatchers = get<AppDispatchers>()

    private var mainScope: CoroutineScope = CoroutineScope(SupervisorJob() + appDispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fun routeInternal() {
            when (viewModel.route(deepLinkUri = intent.data)) {
                is RouterAction.Onboarding -> {
                    startOnboardingFragment()
                }
                is RouterAction.Dashboard -> {
                    startDashboardActivity()
                }
            }
        }

        if (skipSplashscreenDelay) {
            routeInternal()
            finishAffinity()
        } else {
            // Postpone routing until all databases are populated
            mainScope.launch {
                val offlineSyncer = get<OfflineSyncer>()
                offlineSyncer.awaitDatabasePopulation()

                delay(SCREEN_DISPLAY_DURATION) // splashscreen is displayed

                routeInternal()

                finishAffinity()
            }
        }
    }
}

fun Activity.startRouterActivity(skipSplashscreenDelay: Boolean = true) {
    startActivity(Intent(this, RouterActivity::class.java).apply {
        putExtras(RouterActivity.args(skipSplashscreenDelay))
        flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
    })
}