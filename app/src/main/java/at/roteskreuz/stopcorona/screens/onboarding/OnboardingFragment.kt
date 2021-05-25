package at.roteskreuz.stopcorona.screens.onboarding

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.FullScreenPortraitBaseActivity
import at.roteskreuz.stopcorona.screens.dashboard.startHandshakeExplanationFragment
import at.roteskreuz.stopcorona.screens.routing.startRouterActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.argument
import at.roteskreuz.stopcorona.skeleton.core.utils.color
import at.roteskreuz.stopcorona.skeleton.core.utils.dip
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import at.roteskreuz.stopcorona.skeleton.core.utils.onViewReady
import at.roteskreuz.stopcorona.utils.darkTextInStatusBar
import at.roteskreuz.stopcorona.utils.lightTextInStatusBar
import at.roteskreuz.stopcorona.utils.view.CirclePagerIndicatorDecoration
import com.airbnb.epoxy.EpoxyVisibilityTracker
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_onboarding.*
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Screen that onboards the user into the app functionality.
 */
class OnboardingFragment : BaseFragment(R.layout.fragment_onboarding) {

    companion object {
        private const val ARGUMENT_SKIP_ROUTER = "skip_router"

        fun args(skipRouter: Boolean): Bundle {
            return bundleOf(
                ARGUMENT_SKIP_ROUTER to skipRouter
            )
        }
    }

    private val skipRouter: Boolean by argument(ARGUMENT_SKIP_ROUTER, false)

    private val viewModel: OnboardingViewModel by viewModel()

    private val controller: OnboardingController by lazy {
        OnboardingController(
            context = requireContext(),
            dataPrivacyAccepted = viewModel.dataPrivacyAccepted,
            onEnterLastPage = { pageNumber ->
                viewModel.currentPage = pageNumber
            },
            onDataPrivacyCheckBoxChanged = viewModel::setDataPrivacyChecked,
            onAutomaticHandshakeInformationClick = {
                startHandshakeExplanationFragment()
            }
        )
    }

    private val pagerIndicator: CirclePagerIndicatorDecoration by lazy {
        CirclePagerIndicatorDecoration(
            context = requireContext(),
            colorActive = requireContext().color(R.color.onboarding_indicator_active),
            colorInactive = requireContext().color(R.color.onboarding_indicator_inactive),
            paddingBottom = requireContext().dip(6),
            indicatorItemPadding = requireContext().dip(8),
            indicatorItemDiameter = requireContext().dip(8)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.darkTextInStatusBar()

        when (context?.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {
                activity?.lightTextInStatusBar()
            }
        }

        with(contentRecyclerView) {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            setController(controller)
            addItemDecoration(pagerIndicator)
            EpoxyVisibilityTracker().attach(this)
        }
        controller.requestModelBuild()

        btnNext.setOnClickListener {
            if (viewModel.isLastPage(viewModel.currentPage)) {
                if (viewModel.dataPrivacyChecked || viewModel.dataPrivacyAccepted) {
                    viewModel.onboardingFinished()
                    if (skipRouter.not()) {
                        activity?.startRouterActivity(skipSplashscreenDelay = true)
                    }
                    activity?.finish()
                }
            } else {
                onViewReady {
                    contentRecyclerView.smoothScrollToPosition(viewModel.getNextPage())
                }
            }
        }

        disposables += viewModel.observeButtonEnabledState()
            .observeOnMainThread()
            .subscribe { buttonEnabled ->
                btnNext.isEnabled = buttonEnabled
                btnNext.contentDescription = if (viewModel.isLastPage(viewModel.currentPage) && !btnNext.isEnabled) {
                    getString(R.string.accessibility_self_testing_next_button_disabled_description)
                } else {
                    ""
                }
            }

        disposables += viewModel.observeDataPrivacyPageShown()
            .observeOnMainThread()
            .subscribe { dataPrivacyPageShown ->
                when (dataPrivacyPageShown) {
                    true -> {
                        with(contentRecyclerView) {
                            removeItemDecoration(pagerIndicator)
                        }
                    }
                    false -> {
                        with(contentRecyclerView) {
                            if (itemDecorationCount == 1) {
                                addItemDecoration(pagerIndicator)
                            }
                        }
                    }
                }
            }

        disposables += viewModel.observeLastPage()
            .observeOnMainThread()
            .subscribe { isLastPage ->
                btnNext.text = if (isLastPage) {
                    getString(R.string.onboarding_finish_button)
                } else {
                    getString(R.string.onboarding_next_button)
                }
            }
    }

    override fun overrideOnBackPressed(): Boolean {
        return if (viewModel.isFirstPage()) {
            super.overrideOnBackPressed()
        } else {
            onViewReady {
                contentRecyclerView.smoothScrollToPosition(viewModel.getPreviousPage())
            }
            true
        }
    }
}

fun Activity.startOnboardingFragment(skipRouter: Boolean = false, options: Bundle? = null) {
    startFragmentActivity<FullScreenPortraitBaseActivity>(
        fragmentName = OnboardingFragment::class.java.name,
        fragmentArgs = OnboardingFragment.args(skipRouter),
        options = options
    )
}

private fun RecyclerView.setHeight(height: Int) {
    val params = layoutParams as ConstraintLayout.LayoutParams
    params.matchConstraintMaxHeight = height
    layoutParams = params
}

