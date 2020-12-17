package at.roteskreuz.stopcorona.screens.questionnaire.suspicion

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.screens.reporting.startReportingActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import kotlinx.android.synthetic.main.fragment_questionnaire_hint.*

class QuestionnaireSuspicionFragment : BaseFragment(R.layout.fragment_questionnaire_suspicion) {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtHeadline1.contentDescription = getString(R.string.questionnaire_hint_headline_1) + getString(R.string.accessibility_heading_1)
        txtHeadline2.contentDescription = getString(R.string.questionnaire_examine_headling_1) + getString(R.string.accessibility_heading_1)
        txtDescription.contentDescription = getString(R.string.questionnaire_examine_headling_2) + getString(R.string.accessibility_heading_2)

        btnActionButton.setOnClickListener {
            startReportingActivity(MessageType.InfectionLevel.Yellow)
            activity?.finish()
        }
    }
}

fun Activity.startQuestionnaireSuspicionFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = QuestionnaireSuspicionFragment::class.java.name
    )
}

fun Fragment.startQuestionnaireSuspicionFragment() {
    activity?.startQuestionnaireSuspicionFragment()
}
