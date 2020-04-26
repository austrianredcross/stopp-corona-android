package at.roteskreuz.stopcorona.screens.dashboard.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.utils.withCustomStyle
import kotlinx.android.synthetic.main.handshake_microphone_explanation_dialog.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MicrophoneExplanationDialog : DialogFragment() {

    private val viewModel: MicrophoneExplanationDialogViewModel by viewModel()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setView(R.layout.handshake_microphone_explanation_dialog)
            .show()
            .withCustomStyle()
            .apply {
                btnOk.setOnClickListener {
                    if (checkbox.isChecked) {
                        viewModel.doNotShowAgain()
                    }
                    targetFragment
                        ?.onActivityResult(targetRequestCode, Activity.RESULT_OK, Intent())
                        ?: Timber.e(SilentError("Dialog is not shown for result."))
                    dismiss()
                }
                constraintLayoutCheckbox.setOnClickListener {
                    checkbox.toggle()
                }
            }
    }
}