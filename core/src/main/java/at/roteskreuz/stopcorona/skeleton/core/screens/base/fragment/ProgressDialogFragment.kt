package at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment

import android.app.Dialog
import android.app.ProgressDialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import timber.log.Timber

/**
 * Progress dialog fragment with custom message.
 */
class ProgressDialogFragment : DialogFragment() {

    companion object {
        private const val ARGUMENT_MESSAGE = "message"

        fun newInstance(message: String): ProgressDialogFragment {
            return ProgressDialogFragment().apply {
                arguments = bundleOf(ARGUMENT_MESSAGE to message)
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val message = arguments?.getString(ARGUMENT_MESSAGE)
        message ?: Timber.e("message is null")
        val progress = ProgressDialog(activity)
        isCancelable = false
        progress.setCancelable(false)
        progress.setMessage(message)
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        return progress
    }
}
