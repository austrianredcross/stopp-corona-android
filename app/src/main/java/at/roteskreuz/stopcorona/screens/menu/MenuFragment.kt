package at.roteskreuz.stopcorona.screens.menu

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.screens.dashboard.DashboardViewModel
import at.roteskreuz.stopcorona.screens.diary.startDiaryFragment
import at.roteskreuz.stopcorona.screens.onboarding.startOnboardingFragment
import at.roteskreuz.stopcorona.screens.questionnaire.report.startReportSuspicionFragment
import at.roteskreuz.stopcorona.screens.questionnaire.startQuestionnaireFragment
import at.roteskreuz.stopcorona.screens.reporting.startReportingActivity
import at.roteskreuz.stopcorona.screens.savedIDs.startInfoDeleteExposureKeysFragment
import at.roteskreuz.stopcorona.screens.webView.startWebView
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.dipif
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import at.roteskreuz.stopcorona.utils.shareApp
import at.roteskreuz.stopcorona.utils.startDefaultBrowser
import at.roteskreuz.stopcorona.utils.view.AccurateScrollListener
import at.roteskreuz.stopcorona.utils.view.LinearLayoutManagerAccurateOffset
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_dashboard.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.TimeUnit

/**
 * Screen with menu content.
 */
class MenuFragment : BaseFragment(R.layout.menu_fragment) {

    override val isToolbarVisible: Boolean = true

    override fun getTitle(): String? {
        return "" // blank
    }

    private val versionClickSubject = PublishSubject.create<Unit>()

    /**
     * Use the DashboardViewModel to not have ownHealthState observation logic twice
     */
    private val viewModel: DashboardViewModel by viewModel()

    private val controller: MenuController by lazy {
        MenuController(
            context = requireContext(),
            onOnboardingClick = {
                activity?.startOnboardingFragment(skipRouter = true)
            },
            onExternalLinkClick = { url ->
                startDefaultBrowser(url)
            },
            onSavedIdsClick = {
                startInfoDeleteExposureKeysFragment()
            },
            onOpenSourceLicenceClick = {
                OssLicensesMenuActivity.setActivityTitle(getString(R.string.start_menu_item_2_1_open_source_licenses))
                startActivity(Intent(activity, OssLicensesMenuActivity::class.java))
            },
            onPrivacyDataClick = {
                startWebView(R.string.start_menu_item_2_2_data_privacy, "privacy-and-terms-of-use")
            },
            onImprintClick = {
                startWebView(R.string.start_menu_item_2_3_imprint, "imprint")
            },
            onVersionClick = {
                versionClickSubject.onNext(Unit)
            },
            onCheckSymptomsClick = {
                startQuestionnaireFragment()
            },
            onReportSuspectedClick = {
                startReportSuspicionFragment()
            },
            onReportOfficialSicknessClick = {
                startReportingActivity(MessageType.InfectionLevel.Red)
            },
            onShareAppClick = {
                shareApp()
            },
            onRevokeSicknessClick = {
                startReportingActivity(MessageType.Revoke.Sickness)
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
        toolbar?.setNavigationIcon(R.drawable.ic_clear)
        toolbar?.setNavigationContentDescription(R.string.start_menu_close)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(contentRecyclerView) {
            setController(controller)
            layoutManager = LinearLayoutManagerAccurateOffset(requireContext(), accurateScrollListener)
            addOnScrollListener(accurateScrollListener)
        }

        // Easter egg listener when 4 times quickly clicked on version
        disposables += versionClickSubject
            .buffer(1000, TimeUnit.MILLISECONDS)
            .filter { it.size == 4 }
            .observeOnMainThread()
            .subscribe {
                Toast.makeText(requireContext(), "¯\\_(ツ)_/¯", Toast.LENGTH_SHORT).show()
            }

        disposables += viewModel.observeOwnHealthStatus()
            .observeOnMainThread()
            .subscribe { healthStatusData ->
                controller.setData(
                    healthStatusData,
                    viewModel.currentExposureNotificationPhase,
                    viewModel.dateOfFirstMedicalConfirmation
                )
            }
    }

    override fun onDestroyView() {
        contentRecyclerView.removeOnScrollListener(accurateScrollListener)
        super.onDestroyView()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                activity?.finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

fun Fragment.startMenuFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = MenuFragment::class.java.name
    )
}
