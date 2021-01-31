package at.roteskreuz.stopcorona.screens.questionnaire.report

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.repositories.ReportingRepository
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.screens.base.dialog.datepicker.DatePickerFragmentDialog
import at.roteskreuz.stopcorona.screens.reporting.startReportingActivity
import at.roteskreuz.stopcorona.skeleton.core.model.scope.connectToScope
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import at.roteskreuz.stopcorona.utils.format
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_questionnaire_report.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.threeten.bp.ZonedDateTime

/**
 * Screen displaying report suspicion or medical confirmation with date
 */
class QuestionnaireReportFragment : BaseFragment(R.layout.fragment_questionnaire_report) {

    override val isToolbarVisible: Boolean = true

    private val viewModel: QuestionnaireReportViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        connectToScope(ReportingRepository.SCOPE_NAME)
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnNotConfirmContacts.setOnClickListener {
            startReportingActivity(MessageType.InfectionLevel.Yellow)
        }

        btnConfirmContacts.setOnClickListener {
            startReportingActivity(MessageType.InfectionLevel.Red)
        }

        // init date of infection with today
        viewModel.setDateOfInfection(ZonedDateTime.now())

        disposables += viewModel.observeDateOfInfection()
            .observeOnMainThread()
            .subscribe { date ->
                date.dateOfInfection?.let {
                    datePicker.text = if (date.dateOfInfection.dayOfMonth == ZonedDateTime.now().dayOfMonth) {
                        getString(R.string.general_today)
                    } else {
                        date.dateOfInfection.format("EEEE, d. MMM y")
                    }
                }
            }

        datePicker.setOnClickListener{
            val newFragment =
                DatePickerFragmentDialog()
            fragmentManager?.let { newFragment.show(it, DatePickerFragmentDialog::class.java.name) }
        }
    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
        toolbar?.setNavigationContentDescription(R.string.general_back)
    }

    override fun getTitle(): String? {
        return getString(R.string.questionnaire_report_suspicion_title)
    }

    override fun overrideOnBackPressed(): Boolean {
        viewModel.goBack()
        return super.overrideOnBackPressed()
    }
}

fun Activity.startReportSuspicionFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = QuestionnaireReportFragment::class.java.name
    )
}

fun Fragment.startReportSuspicionFragment() {
    activity?.startReportSuspicionFragment()
}

