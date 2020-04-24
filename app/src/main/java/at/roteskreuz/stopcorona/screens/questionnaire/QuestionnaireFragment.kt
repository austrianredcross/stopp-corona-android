package at.roteskreuz.stopcorona.screens.questionnaire

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.entities.configuration.Decision
import at.roteskreuz.stopcorona.model.exceptions.handleBaseCoronaErrors
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.screens.questionnaire.compliance.startQuestionnaireComplianceFragment
import at.roteskreuz.stopcorona.screens.questionnaire.hint.startQuestionnaireHintFragment
import at.roteskreuz.stopcorona.screens.questionnaire.selfmonitoring.startQuestionnaireSelfMonitoringFragment
import at.roteskreuz.stopcorona.screens.questionnaire.suspicion.startQuestionnaireSuspicionFragment
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataState
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.getFragmentActivityIntent
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.color
import at.roteskreuz.stopcorona.skeleton.core.utils.dip
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import at.roteskreuz.stopcorona.skeleton.core.utils.onViewReady
import at.roteskreuz.stopcorona.utils.darkTextInStatusBar
import at.roteskreuz.stopcorona.utils.view.CirclePagerIndicatorDecoration
import at.roteskreuz.stopcorona.utils.view.LinearLayoutManagerWithScrollOption
import at.roteskreuz.stopcorona.utils.view.safeRun
import com.airbnb.epoxy.EpoxyVisibilityTracker
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_questionnaire.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class QuestionnaireFragment : BaseFragment(R.layout.fragment_questionnaire) {

    private val viewModel: QuestionnaireViewModel by viewModel()

    private val controller: QuestionnaireController by lazy {
        QuestionnaireController(
            context = requireContext(),
            onEnterPage = { pageNumber ->
                viewModel.currentPage = pageNumber
            },
            onAnswerSelected = viewModel::setDecision
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (viewModel.complianceAccepted.not()) {
            startQuestionnaireComplianceFragment()
            activity?.finish()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.darkTextInStatusBar()

        with(contentRecyclerView) {
            layoutManager = LinearLayoutManagerWithScrollOption(requireContext())
            setController(controller)
            addItemDecoration(pagerIndicator)
            EpoxyVisibilityTracker().attach(this)
        }

        disposables += viewModel.observeQuestionnaireDataState()
            .observeOnMainThread()
            .subscribe { state ->
                hideProgressDialog()
                when (state) {
                    State.Loading -> {
                        showProgressDialog(R.string.general_loading)
                    }
                    is DataState.Loaded -> {
                        controller.apiConfiguration = state.data
                    }
                    is State.Error -> {
                        handleBaseCoronaErrors(state.error)
                    }
                }
            }

        btnNext.setOnClickListener {
            viewModel.executeDecision()
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

        disposables += viewModel.observeButtonState()
            .observeOnMainThread()
            .subscribe { buttonEnabled ->
                btnNext.isEnabled = buttonEnabled
            }

        disposables += viewModel.observeDecision()
            .observeOnMainThread()
            .subscribe { decision ->
                when (decision) {
                    Decision.NEXT -> contentRecyclerView.forceSmoothScrollToPosition(viewModel.getNextPage())
                    Decision.HINT -> startQuestionnaireHintFragment()
                    Decision.SUSPICION -> startQuestionnaireSuspicionFragment()
                    Decision.SELFMONITORING -> startQuestionnaireSelfMonitoringFragment()
                }
            }
    }

    override fun overrideOnBackPressed(): Boolean {
        return if (viewModel.isFirstPage()) {
            super.overrideOnBackPressed()
        } else {
            onViewReady {
                contentRecyclerView.forceSmoothScrollToPosition(viewModel.getPreviousPage())
            }
            true
        }
    }
}

fun Activity.startQuestionnaireFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = QuestionnaireFragment::class.java.name
    )
}

fun Fragment.startQuestionnaireFragment() {
    activity?.startQuestionnaireFragment()
}

fun Context.getQuestionnaireIntent(): Intent {
    return getFragmentActivityIntent<CoronaPortraitBaseActivity>(
        this,
        fragmentName = QuestionnaireFragment::class.java.name
    )
}

fun RecyclerView.forceSmoothScrollToPosition(position: Int) {
    (layoutManager as? LinearLayoutManagerWithScrollOption).safeRun("LayoutManager does not support scroll options") { manager ->
        manager.scrollable = true
        smoothScrollToPosition(position)

        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == SCROLL_STATE_IDLE) {
                    manager.scrollable = false
                    removeOnScrollListener(this)
                }
            }
        })
    }
}
