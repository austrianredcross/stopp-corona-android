package at.roteskreuz.stopcorona.screens.base.dialog.datepicker

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.repositories.ReportingRepository
import at.roteskreuz.stopcorona.skeleton.core.model.scope.connectToScope
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.threeten.bp.DateTimeUtils
import java.util.*

/**
 * Special DatePicker to choose from when there was a suspicion or confirmation case for a maximum of five days in the past.
 */
class DatePickerFragmentDialog: DialogFragment(), DatePickerDialog.OnDateSetListener{
    val calendar = Calendar.getInstance()
    private val viewModel: DatePickerFragmentDialogViewModel by viewModel()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        connectToScope(ReportingRepository.SCOPE_NAME)
        // Use the current date as the default date in the picker
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Create a new instance of DatePickerDialog and return it
        val datePickerDialog = DatePickerDialog(requireContext(), R.style.DatePickerDialogTheme, this, year, month, day)
        datePickerDialog.datePicker.maxDate = calendar.timeInMillis

        // Subtract 5 days from Calendar
        calendar.add(Calendar.DAY_OF_MONTH, -5);
        datePickerDialog.datePicker.minDate = calendar.timeInMillis

        return datePickerDialog
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        viewModel.setDateOfInfection(DateTimeUtils.toZonedDateTime(calendar))
        dismiss()
    }
}