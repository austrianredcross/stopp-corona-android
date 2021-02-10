package at.roteskreuz.stopcorona.screens.reporting.reportStatus.guideline

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.repositories.QuarantineStatus
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.screens.reporting.personalData.ReportingPersonalDataFragment
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.dip
import at.roteskreuz.stopcorona.skeleton.core.utils.dipif
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import at.roteskreuz.stopcorona.skeleton.core.utils.visible
import at.roteskreuz.stopcorona.utils.*
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.certificate_report_guidelines_fragment.*
import kotlinx.android.synthetic.main.certificate_report_guidelines_fragment.transparentAppBar
import kotlinx.android.synthetic.main.certificate_report_guidelines_fragment.txtDescription1
import kotlinx.android.synthetic.main.certificate_report_guidelines_fragment.txtDescription4Phone
import kotlinx.android.synthetic.main.certificate_report_guidelines_fragment.txtTitle
import kotlinx.android.synthetic.main.guide_info_help.*
import kotlinx.android.synthetic.main.guide_info_help.txtConsultingFirstPhone
import kotlinx.android.synthetic.main.guide_info_help.txtConsultingSecondPhone
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Screen displaying guideline of certificate reporting.
 */
class CertificateReportGuidelinesFragment : BaseFragment(R.layout.certificate_report_guidelines_fragment) {

    private val viewModel: CertificateReportGuidelinesViewModel by viewModel()

    override val isToolbarVisible: Boolean = true

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtTitle.contentDescription = getString(R.string.sickness_certificate_quarantine_guidelines_headline) + getString(R.string.accessibility_heading_1)

        scrollViewContainer.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            transparentAppBar.elevation = if (scrollY > requireContext().dip(ReportingPersonalDataFragment.SCROLLED_DISTANCE_THRESHOLD)) {
                requireContext().dipif(4)
            } else {
                0f
            }
        }

        txtDescription4Phone.text  = SpannableStringBuilder().apply {
            context?.let {context->
                append(context.getClickableBoldSpan(R.string.sickness_certificate_quarantine_guidelines_steps_fourth_phone,
                    colored = true,
                    underline = false,
                    insertLeadingSpace = false,
                    onClick = {
                        context.startCallWithPhoneNumber(context.getString(R.string.sickness_certificate_quarantine_guidelines_steps_fourth_phone))
                    }))
            }
        }

        txtConsultingFirstPhone.startPhoneCallOnClick()
        txtConsultingSecondPhone.startPhoneCallOnClick()
        txtConsultingThirdPhone.startPhoneCallOnClick()
        txtUrgentNumber1.startPhoneCallOnClick()
        txtUrgentNumber2.startPhoneCallOnClick()
    }

    override fun getTitle(): String? {
        return getString(R.string.sickness_certificate_quarantine_guidelines_title)
    }
}

fun Fragment.startCertificateReportGuidelinesFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = CertificateReportGuidelinesFragment::class.java.name
    )
}

