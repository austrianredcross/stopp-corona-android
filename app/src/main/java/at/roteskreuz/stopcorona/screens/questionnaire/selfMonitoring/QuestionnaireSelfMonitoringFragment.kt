package at.roteskreuz.stopcorona.screens.questionnaire.selfmonitoring

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.screens.routing.startRouterActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.visible
import kotlinx.android.synthetic.main.fragment_questionnaire_self_monitoring.*
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Screen to display information after the user has completed a self testing and
 * the result was to monitor symptoms.
 */
open class QuestionnaireSelfMonitoringFragment : BaseFragment(R.layout.fragment_questionnaire_self_monitoring) {

    override val isToolbarVisible: Boolean = true

    private val viewModel: QuestionnaireSelfMonitoringViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtHeadline1.text = getString(R.string.questionnaire_examine_observe_headline_1)
        txtHeadline2.text = getString(R.string.questionnaire_examine_observe_sub_headline_1)
        txtDescription1.text = getString(R.string.questionnaire_examine_observe_recommendation_1)
        txtDescription2.text = getString(R.string.questionnaire_examine_observe_recommendation_2)
        txtDescription3.text = getString(R.string.questionnaire_examine_observe_recommendation_3)
        txtDescription.text = getString(R.string.questionnaire_examine_observe_headline_2)
        txtSubDescription.text = getString(R.string.questionnaire_examine_observe_description)
        btnActionButton.text = getString(R.string.onboarding_finish_button)
        txtStepsHeadline.visible = false
        txtFormFilledDate.visible = false
        btnActionButton.setOnClickListener {
            activity?.startRouterActivity()
        }

        viewModel.reportSelfMonitoring()
    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
    }

    override fun getTitle(): String? {
        return getString(R.string.questionnaire_examine_observe_title)
    }
}

fun Fragment.startQuestionnaireSelfMonitoringFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = QuestionnaireSelfMonitoringFragment::class.java.name
    )
}
