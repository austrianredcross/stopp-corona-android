package at.roteskreuz.stopcorona.screens.dashboard.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.utils.startDefaultBrowser
import at.roteskreuz.stopcorona.utils.withCustomStyle
import kotlinx.android.synthetic.main.automatic_handshake_explanation_dialog.*

class AutomaticHandshakeExplanationDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setView(R.layout.automatic_handshake_explanation_dialog)
            .show()
            .withCustomStyle()
            .apply {
                btnClose.setOnClickListener {
                    dismiss()
                }
                btnFaq.setOnClickListener {
                    startDefaultBrowser(getString(R.string.automatic_handshake_dialog_faq_link))
                }
            }
    }
}
