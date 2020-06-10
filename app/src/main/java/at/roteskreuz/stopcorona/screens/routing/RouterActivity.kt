package at.roteskreuz.stopcorona.screens.routing

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import at.roteskreuz.stopcorona.screens.dashboard.startDashboardActivity
import at.roteskreuz.stopcorona.screens.onboarding.startOnboardingFragment
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.argument
import kotlinx.coroutines.*
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
        private const val SCREEN_DISPLAY_DURATION = 1_000L // ms
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

    private var activityScope: CoroutineScope? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fun routeInternal() {
            when (val routerAction = viewModel.route(deepLinkUri = intent.data)) {
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
            activityScope = CoroutineScope(SupervisorJob() + appDispatchers.Main)

            // Postpone routing until all databases are populated
            activityScope?.launch {
                delay(SCREEN_DISPLAY_DURATION) // splashscreen is displayed

                routeInternal()

                finishAffinity()
            }
        }
    }

    override fun onStop() {
        activityScope?.cancel()
        super.onStop()
    }
}

fun Activity.startRouterActivity(skipSplashscreenDelay: Boolean = true) {
    startActivity(Intent(this, RouterActivity::class.java).apply {
        putExtras(RouterActivity.args(skipSplashscreenDelay))
        flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
    })
}