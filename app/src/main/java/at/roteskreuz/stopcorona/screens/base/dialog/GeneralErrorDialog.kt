package at.roteskreuz.stopcorona.screens.base.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.utils.withCustomStyle

class GeneralErrorDialog(
    @StringRes private val title: Int,
    @StringRes private val message: Int
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.general_ok) { _, _ -> dismiss() }
            .show()
            .withCustomStyle()
    }
}
