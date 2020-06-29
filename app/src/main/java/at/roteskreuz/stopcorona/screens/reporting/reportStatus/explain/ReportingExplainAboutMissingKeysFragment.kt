package at.roteskreuz.stopcorona.screens.reporting.reportStatus.explain

import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.reporting.reportStatus.ReportingStatusController
import at.roteskreuz.stopcorona.screens.reporting.reportStatus.ReportingStatusViewModel
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReportingExplainAboutMissingKeysFragment : BaseFragment(R.layout.fragment_reporting_explanation_missing_keys) {

    companion object {
        const val CURRENT_SCREEN = 0
    }

    private val viewModel: ReportingStatusViewModel by viewModel()

    override val isToolbarVisible: Boolean = true

    override fun getTitle(): String? {
        return "" // blank, is depending on messageType
    }

    private val controller: ReportingStatusController by lazy {
        ReportingStatusController(
            context = requireContext(),
            onAgreementCheckboxChange = viewModel::setUserAgreement,
            onSendReportClick = viewModel::uploadData
        )
    }
}