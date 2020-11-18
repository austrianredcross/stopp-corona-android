package at.roteskreuz.stopcorona.screens.questionnaire.guideline

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.repositories.QuarantineStatus
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.screens.reporting.startReportingActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import at.roteskreuz.stopcorona.skeleton.core.utils.visible
import at.roteskreuz.stopcorona.utils.formatDayAndMonth
import at.roteskreuz.stopcorona.utils.getClickableBoldSpan
import at.roteskreuz.stopcorona.utils.startCallWithPhoneNumber
import at.roteskreuz.stopcorona.utils.startPhoneCallOnClick
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.questionnaire_guideline_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Screen displaying success of questionnaire results reporting.
 */
class QuestionnaireGuidelineFragment : BaseFragment(R.layout.questionnaire_guideline_fragment) {

    private val viewModel: QuestionnaireGuidelineViewModel by viewModel()

    override val isToolbarVisible: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnBackToDashboard.setOnClickListener {
            startReportingActivity(MessageType.InfectionLevel.Yellow)
            activity?.finish()
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
        txtDescription4Phone.startPhoneCallOnClick()
        txtConsultingFirstPhone.startPhoneCallOnClick()
        txtConsultingSecondPhone.startPhoneCallOnClick()
        txtAdvicePhone1Number.startPhoneCallOnClick()
        txtAdvicePhone2Number.startPhoneCallOnClick()
        txtAdvicePhone3Number.startPhoneCallOnClick()

        txtStayInQuarantineUntil.visible = false
        disposables += viewModel.observeQuarantineStatus()
            .observeOnMainThread()
            .subscribe { quarantineStatus ->
                if (quarantineStatus is QuarantineStatus.Jailed.Limited) {
                    val date = quarantineStatus.end.toLocalDate().formatDayAndMonth(requireContext())
                    txtStayInQuarantineUntil.visible = true
                    txtStayInQuarantineUntil.text = getString(R.string.infection_info_quarantine_end, date)
                }
            }
    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
        toolbar?.setNavigationContentDescription(R.string.general_back)
    }

    override fun getTitle(): String? {
        return getString(R.string.questionnaire_guideline_title)
    }
}

fun Fragment.startQuestionnaireGuidelineFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = QuestionnaireGuidelineFragment::class.java.name
    )
}
