package at.roteskreuz.stopcorona.screens.reporting.tanCheck

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.Constants.Misc.EMPTY_STRING
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.exceptions.handleBaseCoronaErrors
import at.roteskreuz.stopcorona.model.repositories.ReportingRepository
import at.roteskreuz.stopcorona.screens.reporting.personalData.ReportingPersonalDataFragment
import at.roteskreuz.stopcorona.screens.reporting.personalData.displayFieldInlineError
import at.roteskreuz.stopcorona.screens.reporting.personalData.listenForTextChanges
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.model.scope.connectToScope
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.dip
import at.roteskreuz.stopcorona.skeleton.core.utils.dipif
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import at.roteskreuz.stopcorona.skeleton.core.utils.visible
import at.roteskreuz.stopcorona.utils.KeyboardHelper
import at.roteskreuz.stopcorona.utils.view.applyText
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_reporting_tan_check.*
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Screen for checking the TAN received through SMS.
 */
class ReportingTanCheckFragment : BaseFragment(R.layout.fragment_reporting_tan_check) {

    companion object {
        const val CURRENT_SCREEN = 2
    }

    private val viewModel: ReportingTanCheckViewModel by viewModel()

    override val isToolbarVisible: Boolean = true

    override fun getTitle(): String? {
        return "" // blank, is depending on messageType
    }

    private val keyboardHelper = KeyboardHelper(
        this,
        onKeyboardShown = {
            btnNext.visible = false
        },
        onKeyboardHidden = {
            btnNext.visible = true
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        connectToScope(ReportingRepository.SCOPE_NAME)
        super.onCreate(savedInstanceState)
    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtProgress.text = getString(
            R.string.certificate_personal_progress_label,
            CURRENT_SCREEN,
            ReportingPersonalDataFragment.TOTAL_NUMBER_OF_SCREENS
        )

        disposables += viewModel.observeMessageType()
            .observeOnMainThread()
            .subscribe { messageType ->
                when (messageType) {
                    is MessageType.Revoke.Sickness -> setTitle(R.string.revoke_sickness_title)
                    else -> setTitle(R.string.certificate_tan_check_title)
                }
            }

        disposables += viewModel.observePersonalData()
            .observeOnMainThread()
            .subscribe {
                txtDescription.text =
                    getString(R.string.certificate_tan_check_description, it.mobileNumber)
            }

        disposables += viewModel.observeTanData()
            .observeOnMainThread()
            .subscribe {
                textInputEditTextTan.applyText(it.tan)
            }

        disposables += viewModel.observeValidationResult()
            .observeOnMainThread()
            .subscribe { validationResult ->
                displayFieldInlineError(validationResult.tan, textInputLayoutTan, requireContext())
            }

        disposables += viewModel.observeTanRequestState()
            .observeOnMainThread()
            .subscribe { state ->
                hideProgressDialog()
                when (state) {
                    is State.Loading -> {
                        showProgressDialog(R.string.general_loading)
                        textInputEditTextTan.applyText(EMPTY_STRING)
                    }
                    is State.Error -> {
                        handleBaseCoronaErrors(state.error)
                    }
                }
            }

        textInputEditTextTan.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnNext.performClick()
                true
            } else false
        }

        btnNext.setOnClickListener {
            viewModel.validate()
        }

        btnResendTan.setOnClickListener {
            viewModel.requestTan()
        }

        keyboardHelper.enable()
        listenForTextChanges(textInputLayoutTan, textInputEditTextTan, viewModel::setTan)

        scrollViewContainer.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            transparentAppBar.elevation = if (scrollY > requireContext().dip(ReportingPersonalDataFragment.SCROLLED_DISTANCE_THRESHOLD)) {
                requireContext().dipif(4)
            } else {
                0f
            }
        }
    }

    override fun overrideOnBackPressed(): Boolean {
        viewModel.goBack()
        return true // the changing of fragments is managing parent activity
    }

    override fun onDestroyView() {
        keyboardHelper.disable()
        super.onDestroyView()
    }
}

