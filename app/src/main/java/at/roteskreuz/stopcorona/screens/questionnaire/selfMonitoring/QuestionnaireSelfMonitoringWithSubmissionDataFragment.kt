package at.roteskreuz.stopcorona.screens.questionnaire.selfmonitoring

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.screens.questionnaire.startQuestionnaireFragment
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.dip
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import at.roteskreuz.stopcorona.skeleton.core.utils.visible
import at.roteskreuz.stopcorona.utils.backgroundColor
import at.roteskreuz.stopcorona.utils.formatDayAndMonthAndYearAndTime
import at.roteskreuz.stopcorona.utils.tint
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_questionnaire_hint.btnActionButton
import kotlinx.android.synthetic.main.fragment_questionnaire_hint.txtHeadline1
import kotlinx.android.synthetic.main.fragment_questionnaire_self_monitoring.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.threeten.bp.ZonedDateTime

/**
 * Screen to display the information about symptoms monitoring, it's opened
 * from the health status card in dashboard.
 */
class QuestionnaireSelfMonitoringWithSubmissionDataFragment : BaseFragment(R.layout.fragment_questionnaire_self_monitoring){

    override val isToolbarVisible: Boolean = true

    private val viewModel: QuestionnaireSelfMonitoringViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtHeadline1.visible = false
        txtStepsHeadline.visible = true
        txtFormFilledDate.visible = true

        txtHeadline2.text = getString(R.string.questionnaire_examine_observe_sub_headline_1)
        txtDescription1.text = getString(R.string.questionnaire_examine_observe_recommendation_1)
        txtDescription2.text = getString(R.string.questionnaire_examine_observe_recommendation_2)
        txtDescription3.text = getString(R.string.questionnaire_examine_observe_recommendation_3)
        txtDescription.text = getString(R.string.questionnaire_examine_observe_headline_2)
        txtSubDescription.text = getString(R.string.questionnaire_examine_observe_description)
        txtStepsHeadline.text = getString(R.string.questionnaire_observe_symptoms_next_steps)
        btnActionButton.text = getString(R.string.questionnaire_observe_symptoms_button)
        stepsContainer.backgroundColor(R.color.questionnaire_self_monitoring_container)
        imgCircle1.tint(R.color.questionnaire_self_monitoring_circle_tint)
        imgCircle2.tint(R.color.questionnaire_self_monitoring_circle_tint)
        imgCircle3.tint(R.color.questionnaire_self_monitoring_circle_tint)
        stepsContainer.updateLayoutParams<ConstraintLayout.LayoutParams> {
            topMargin = requireContext().dip(32)
        }
        txtDescription.updateLayoutParams<ConstraintLayout.LayoutParams> {
            topMargin = requireContext().dip(32)
        }

        btnActionButton.setOnClickListener {
            startQuestionnaireFragment()
            activity?.finish()
        }

        disposables += viewModel.observeDateOfLastSelfMonitoringInstruction()
            .observeOnMainThread()
            .subscribe { dateOfLastSelfMonitoringInstruction ->
                if (dateOfLastSelfMonitoringInstruction.isPresent){
                    txtFormFilledDate.text =
                        getString(R.string.questionnaire_observe_symptoms_form_filled_date, dateOfLastSelfMonitoringInstruction.get().formatDayAndMonthAndYearAndTime(requireContext()))
                }
            }
    }

    override fun getTitle(): String? {
        return getString(R.string.questionnaire_examine_observe_title)
    }

}

fun Fragment.startQuestionnaireSelfMonitoringWithSubmissionDataFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = QuestionnaireSelfMonitoringWithSubmissionDataFragment::class.java.name
    )
}