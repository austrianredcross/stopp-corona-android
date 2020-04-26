package at.roteskreuz.stopcorona.screens.questionnaire.compliance

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.screens.questionnaire.startQuestionnaireFragment
import at.roteskreuz.stopcorona.screens.webView.startWebView
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.dipif
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import at.roteskreuz.stopcorona.utils.string
import at.roteskreuz.stopcorona.utils.view.AccurateScrollListener
import at.roteskreuz.stopcorona.utils.view.LinearLayoutManagerAccurateOffset
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_contact_history.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class QuestionnaireComplianceFragment : BaseFragment(R.layout.fragment_questionnaire_compliance) {

    override val isToolbarVisible: Boolean = true

    override fun getTitle(): String? {
        return context?.string(R.string.questionnaire_examine_title)
    }

    private val viewModel: QuestionnaireComplianceViewModel by viewModel()

    private val controller: QuestionnaireComplianceController by lazy {
        QuestionnaireComplianceController(
            context = requireContext(),
            onAgreementCheckboxChange = viewModel::setComplianceAccepted,
            onDataPrivacyClick = { startWebView(R.string.onboarding_headline_data_privacy, "privacy") },
            onContinueClick = {
                viewModel.onComplianceAccepted()
                startQuestionnaireFragment()
                activity?.finish()
            }
        )
    }

    private val accurateScrollListener by lazy {
        AccurateScrollListener(
            onScroll = { scrolledDistance ->
                transparentAppBar.elevation = if (scrolledDistance > 0) {
                    requireContext().dipif(4)
                } else {
                    0f
                }
            }
        )
    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(contentRecyclerView) {
            setController(controller)
            layoutManager = LinearLayoutManagerAccurateOffset(requireContext(), accurateScrollListener)
            addOnScrollListener(accurateScrollListener)
        }

        disposables += viewModel.observeComplianceAccepted()
            .observeOnMainThread()
            .subscribe { accepted ->
                controller.continueButtonEnabled = accepted
            }
    }
}

fun Fragment.startQuestionnaireComplianceFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = QuestionnaireComplianceFragment::class.java.name
    )
}
