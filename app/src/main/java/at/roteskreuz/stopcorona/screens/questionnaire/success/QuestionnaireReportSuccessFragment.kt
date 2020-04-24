package at.roteskreuz.stopcorona.screens.questionnaire.success

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.screens.dashboard.goBackToDashboardActivity
import at.roteskreuz.stopcorona.screens.questionnaire.guideline.startQuestionnaireGuidelineFragment
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.utils.getClickableBoldSpan
import at.roteskreuz.stopcorona.utils.startCallWithPhoneNumber
import at.roteskreuz.stopcorona.utils.startPhoneCallOnClick
import kotlinx.android.synthetic.main.certificate_report_success_fragment.btnBackToDashboard
import kotlinx.android.synthetic.main.certificate_report_success_fragment.btnQuarantineGuideline
import kotlinx.android.synthetic.main.questionnaire_report_success_fragment.*
import kotlinx.android.synthetic.main.questionnaire_report_success_fragment.txtDescription3

/**
 * Screen displaying success of questionnaire results reporting.
 */
class QuestionnaireReportSuccessFragment : BaseFragment(R.layout.questionnaire_report_success_fragment) {

    override val isToolbarVisible: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnQuarantineGuideline.setOnClickListener {
            startQuestionnaireGuidelineFragment()
        }

        btnBackToDashboard.setOnClickListener {
            activity?.onBackPressed()
        }

        txtDescription3.text = SpannableStringBuilder().apply {
            append(getString(R.string.questionnaire_guideline_contact_info1))
            append(requireContext().getClickableBoldSpan(R.string.questionnaire_guideline_contact_phone,
                colored = true,
                underline = false,
                onClick = {
                    requireContext().startCallWithPhoneNumber(getString(R.string.questionnaire_guideline_contact_phone))
                }))
            append(getString(R.string.questionnaire_guideline_contact_info2))
        }
        txtDescription3.movementMethod = LinkMovementMethod()
        txtPhoneContact.text = SpannableStringBuilder().apply {
            append(getString(R.string.questionnaire_guideline_contact_info3))
            append(requireContext().getClickableBoldSpan(R.string.questionnaire_guideline_contact_phone,
                colored = true,
                underline = false,
                onClick = {
                    requireContext().startCallWithPhoneNumber(getString(R.string.questionnaire_guideline_contact_phone))
                }))
        }
        txtPhoneContact.movementMethod = LinkMovementMethod()
        txtConsultingFirstPhone.startPhoneCallOnClick()
        txtConsultingSecondPhone.startPhoneCallOnClick()
    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
    }

    override fun overrideOnBackPressed(): Boolean {
        goBackToDashboardActivity()
        return true
    }

    override fun getTitle(): String? {
        return getString(R.string.sickness_certificate_confirmation_title)
    }
}

fun Fragment.startQuestionnaireReportSuccessFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = QuestionnaireReportSuccessFragment::class.java.name
    )
}
