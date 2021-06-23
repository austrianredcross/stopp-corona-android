package at.roteskreuz.stopcorona.screens.base.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import at.roteskreuz.stopcorona.BuildConfig
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.utils.withCustomStyle

class GeneralErrorDialog(
    @StringRes private val title: Int,
    @StringRes private val messageId: Int,
    private val exceptionForDebugMessage: Throwable? = null
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val message = getString(messageId)
        val finalMessage = if(BuildConfig.DEBUG && exceptionForDebugMessage != null) {
            "$message\n${exceptionForDebugMessage}, Error Message: ${exceptionForDebugMessage.message}"
        } else {
            message
        }
        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(finalMessage)
            .setPositiveButton(R.string.general_ok) { _, _ -> dismiss() }
            .show()
            .withCustomStyle()
    }
}
