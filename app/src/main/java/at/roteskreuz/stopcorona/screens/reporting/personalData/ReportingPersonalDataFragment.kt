package at.roteskreuz.stopcorona.screens.reporting.personalData

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.api.SicknessCertificateUploadException
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.exceptions.handleBaseCoronaErrors
import at.roteskreuz.stopcorona.model.repositories.ReportingRepository
import at.roteskreuz.stopcorona.screens.base.dialog.GeneralErrorDialog
import at.roteskreuz.stopcorona.screens.base.dialog.datepicker.DatePickerFragmentDialog
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.model.scope.connectToScope
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.dip
import at.roteskreuz.stopcorona.skeleton.core.utils.dipif
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import at.roteskreuz.stopcorona.skeleton.core.utils.onViewReady
import at.roteskreuz.stopcorona.utils.*
import at.roteskreuz.stopcorona.utils.view.applyText
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_questionnaire_report.*
import kotlinx.android.synthetic.main.fragment_reporting_personal_data.*
import kotlinx.android.synthetic.main.fragment_reporting_personal_data.datePicker
import kotlinx.android.synthetic.main.fragment_reporting_personal_data.scrollViewContainer
import kotlinx.android.synthetic.main.fragment_reporting_personal_data.transparentAppBar
import kotlinx.android.synthetic.main.fragment_reporting_personal_data.txtDescription
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.threeten.bp.ZonedDateTime

/**
 * Screen for entering personal data, part of the flow for reporting a medical certificate or
 * the result of a self-testing to the authorities.
 */
class ReportingPersonalDataFragment : BaseFragment(R.layout.fragment_reporting_personal_data) {

    companion object {
        const val CURRENT_SCREEN = 1
        const val TOTAL_NUMBER_OF_SCREENS = 3
        const val SCROLLED_DISTANCE_THRESHOLD = 2 // dp
    }

    private val viewModel: ReportingPersonalDataViewModel by viewModel()

    override val isToolbarVisible: Boolean = true

    override fun getTitle(): String? {
        return "" // blank, is depending on messageType
    }

    private lateinit var textWatcherMobileNumber: TextWatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        connectToScope(ReportingRepository.SCOPE_NAME)
        super.onCreate(savedInstanceState)
    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
        toolbar?.setNavigationContentDescription(R.string.general_back)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtTitle.contentDescription = getString(R.string.certificate_personal_data_title) + getString(R.string.accessibility_heading_1)
        txtProgress.text = getString(R.string.certificate_personal_progress_label, CURRENT_SCREEN, TOTAL_NUMBER_OF_SCREENS)



        datePicker.setOnClickListener{
            val newFragment =
                DatePickerFragmentDialog()
            fragmentManager?.let { newFragment.show(it, DatePickerFragmentDialog::class.java.name) }
        }

        disposables += viewModel.observeMessageType()
            .observeOnMainThread()
            .subscribe { messageType ->
                when (messageType) {
                    is MessageType.Revoke.Sickness -> {
                        setTitle(R.string.revoke_sickness_title)
                        txtTitle.text = getString(R.string.revoke_sickness_headline)
                        txtDescription.text = getString(R.string.revoke_sickness_personal_data_description)
                    }
                    else -> {
                        setTitle(R.string.certificate_personal_data_title)
                        txtTitle.text = getString(R.string.certificate_personal_data_title)
                        txtDescription.text = if (viewModel.dateOfInfection != null){
                            val reportContactsDate = viewModel.dateOfInfection!!.minusDays(2)
                            val days = reportContactsDate.daysTo(ZonedDateTime.now())
                            getString(R.string.certificate_personal_data_description, days.toString())
                        } else {
                            getString(R.string.certificate_personal_data_description, "2")
                        }
                        if (messageType is MessageType.InfectionLevel.Red) {
                            dateContainer.visibility = VISIBLE

                            disposables += viewModel.observeDateOfInfection()
                                .observeOnMainThread()
                                .subscribe { date ->
                                    date.dateOfInfection?.let {
                                        datePicker.text = if (date.dateOfInfection.dayOfMonth == ZonedDateTime.now().dayOfMonth) {
                                            getString(R.string.general_today)
                                        } else {
                                            date.dateOfInfection.format("EEEE, d. MMM y")
                                        }
                                        val reportContactsDate = date.dateOfInfection.minusDays(2)
                                        val days = reportContactsDate.daysTo(ZonedDateTime.now())
                                        txtDescription.text = getString(R.string.certificate_personal_data_description, days.toString())
                                    } ?: run {
                                        // init date of infection with today
                                        viewModel.setDateOfInfection(ZonedDateTime.now())
                                    }
                                }
                        } else {
                            dateContainer.visibility = GONE
                        }
                    }
                }
            }

        disposables += viewModel.observePersonalData()
            .observeOnMainThread()
            .subscribe {
                textInputEditTextMobileNumber.applyText(it.mobileNumber)
            }

        disposables += viewModel.observeValidationResult()
            .observeOnMainThread()
            .subscribe { validationResult ->
                if (validationResult.isValid().not()) {
                    handleValidationResult(validationResult)
                }
            }

        disposables += viewModel.observeTanRequestState()
            .observeOnMainThread()
            .subscribe { state ->
                hideProgressDialog()
                when (state) {
                    is State.Loading -> {
                        showProgressDialog(R.string.general_loading)
                    }
                    is State.Error -> {
                        when (state.error) {
                            is SicknessCertificateUploadException.PhoneNumberInvalidException, is SicknessCertificateUploadException -> {
                                displayInlineErrorForInvalidNumber()
                            }
                            else -> handleBaseCoronaErrors(state.error)
                        }
                    }
                }
            }

        btnNext.setOnClickListener {
            viewModel.validate()
        }

        scrollViewContainer.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            transparentAppBar.elevation = if (scrollY > requireContext().dip(SCROLLED_DISTANCE_THRESHOLD)) {
                requireContext().dipif(4)
            } else {
                0f
            }
        }

        textInputEditTextMobileNumber.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnNext.performClick()
                true
            } else false
        }

        textWatcherMobileNumber = listenForTextChanges(
            textInputLayoutMobileNumber,
            textInputEditTextMobileNumber,
            viewModel::setMobileNumber
        )
    }

    private fun handleValidationResult(validationResult: ValidationResult) {
        updateUIBasedOnValidationResult(validationResult)
        scrollToError(validationResult)
    }

    private fun displayInlineErrorForInvalidNumber() {
        textInputLayoutMobileNumber.error = requireContext().getString(R.string.certificate_personal_data_input_error)
        scrollToField(textInputLayoutMobileNumber)
        GeneralErrorDialog(R.string.certificate_personal_data_invalid_mobile_number_error,
            R.string.certificate_personal_data_invalid_mobile_number_error).show()
    }

    private fun updateUIBasedOnValidationResult(validationResult: ValidationResult) {
        displayFieldInlineError(validationResult.mobileNumber, textInputLayoutMobileNumber, requireContext())
    }

    private fun scrollToError(validationResult: ValidationResult) {
        if (scrollToParticularError(validationResult.mobileNumber, textInputLayoutMobileNumber)) {
            return
        }
    }

    /**
     * Returns true if we scroll to the field where error occurred, otherwise false.
     */
    private fun scrollToParticularError(validationError: ValidationError?, textInputLayout: TextInputLayout): Boolean {
        validationError?.let {
            scrollToField(textInputLayout)
            return true
        } ?: return false
    }

    private fun scrollToField(textInputLayout: TextInputLayout) {
        onViewReady {
            scrollViewContainer.smoothScrollTo(0, textInputLayout.top)
        }
    }

    override fun overrideOnBackPressed(): Boolean {
        viewModel.goBack()
        activity?.finish()
        return true // the changing of fragments is managing parent activity
    }

    override fun onDestroyView() {
        textInputEditTextMobileNumber.removeTextChangedListener(textWatcherMobileNumber)
        super.onDestroyView()
    }
}
