package at.roteskreuz.stopcorona.screens.sun_downer

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.FullScreenPortraitBaseActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.color
import at.roteskreuz.stopcorona.skeleton.core.utils.dip
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import at.roteskreuz.stopcorona.skeleton.core.utils.onViewReady
import at.roteskreuz.stopcorona.utils.darkTextInStatusBar
import at.roteskreuz.stopcorona.utils.lightTextInStatusBar
import at.roteskreuz.stopcorona.utils.startDefaultBrowser
import at.roteskreuz.stopcorona.utils.view.CirclePagerIndicatorDecoration
import com.airbnb.epoxy.EpoxyVisibilityTracker
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_sun_downer.btnNext
import kotlinx.android.synthetic.main.fragment_sun_downer.contentRecyclerView
import org.koin.androidx.viewmodel.ext.android.viewModel


/**
 * Screen to show sun downer.
 */
class SunDownerFragment : BaseFragment(R.layout.fragment_sun_downer) {

    private val viewModel: SunDownerViewModel by viewModel()

    private val controller: SunDownerController by lazy {
        SunDownerController(
            context = requireContext(),
            onEnterPage = { pageNumber ->
                viewModel.currentPage = pageNumber
            },
            onClickNewsletterButton = { onClickNewsletterButton() }
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

        disposables += viewModel.observeLastPage()
            .observeOnMainThread()
            .subscribe { isLastPage ->
                btnNext.text = if (isLastPage) {
                    getString(R.string.sunDowner_third_page_button)
                } else {
                    getString(R.string.onboarding_next_button)
                }
            }

        btnNext.setOnClickListener {
            if (viewModel.isLastPage()) {
                viewModel.markSunDownerShown()
                activity?.finish()
            } else {
                onViewReady {
                    contentRecyclerView.smoothScrollToPosition(viewModel.getNextPage())
                }
            }
        }

        controller.requestModelBuild()
    }

    override fun overrideOnBackPressed(): Boolean {
        return if (viewModel.isFirstPage()) {
            return true
        } else {
            onViewReady {
                contentRecyclerView.smoothScrollToPosition(viewModel.getPreviousPage())
            }
            true
        }
    }

    private fun onClickNewsletterButton() {
        startDefaultBrowser(getString(R.string.sunDowner_newsletter_link))
    }
}

fun Activity.startSunDownerFragment() {
    startFragmentActivity<FullScreenPortraitBaseActivity>(
        fragmentName = SunDownerFragment::class.java.name
    )
}