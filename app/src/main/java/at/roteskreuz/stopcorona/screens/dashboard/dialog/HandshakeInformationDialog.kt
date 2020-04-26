package at.roteskreuz.stopcorona.screens.dashboard.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.utils.withCustomStyle
import kotlinx.android.synthetic.main.handshake_explanation_dialog.*

class HandshakeInformationDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setView(R.layout.dashboard_handshake_information_dialog)
            .show()
            .withCustomStyle()
            .apply {
                btnClose.setOnClickListener {
                    dismiss()
                }
            }
    }
}