package at.roteskreuz.stopcorona.screens.reporting.reportStatus.guideline

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.screens.reporting.personalData.ReportingPhoneNumberFragment
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.dip
import at.roteskreuz.stopcorona.skeleton.core.utils.dipif
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import at.roteskreuz.stopcorona.skeleton.core.utils.visible
import at.roteskreuz.stopcorona.utils.formatDayAndMonth
import at.roteskreuz.stopcorona.utils.startPhoneCallOnClick
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.certificate_report_guidelines_fragment.*
import kotlinx.android.synthetic.main.guide_info_epoxy_model.*
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

        scrollViewContainer.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            transparentAppBar.elevation = if (scrollY > requireContext().dip(ReportingPhoneNumberFragment.SCROLLED_DISTANCE_THRESHOLD)) {
                requireContext().dipif(4)
            } else {
                0f
            }
        }

        txtTopDescription.visible = false
        disposables += viewModel.observeDateOfFirstMedicalConfirmation()
            .observeOnMainThread()
            .subscribe { dateOfFirstMedicalConfirmation ->
                if (dateOfFirstMedicalConfirmation.isPresent) {
                    val date = dateOfFirstMedicalConfirmation.get().toLocalDate().formatDayAndMonth(requireContext())
                    txtTopDescription.visible = true
                    txtTopDescription.text = getString(R.string.sickness_certificate_guidelines_top_description, date)
                }
            }

        val description4team = getString(R.string.sickness_certificate_guidelines_fourth_team)
        val description4 = SpannableString(getString(R.string.sickness_certificate_guidelines_fourth, description4team))
        val teamStartingIndex = description4.indexOf(description4team)
        description4.setSpan(StyleSpan(Typeface.BOLD), teamStartingIndex, teamStartingIndex + description4team.length, 0)
        txtDescription4.text = description4

        txtDescription4Phone.startPhoneCallOnClick()
        txtConsultingFirstPhone.startPhoneCallOnClick()
        txtConsultingSecondPhone.startPhoneCallOnClick()
        txtConsultingThirdPhone.startPhoneCallOnClick()
        txtUrgentNumber1.startPhoneCallOnClick()
        txtUrgentNumber2.startPhoneCallOnClick()
    }

    override fun getTitle(): String? {
        return getString(R.string.sickness_certificate_guidelines_title)
    }
}

fun Fragment.startCertificateReportGuidelinesFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = CertificateReportGuidelinesFragment::class.java.name
    )
}

