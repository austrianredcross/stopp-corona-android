package at.roteskreuz.stopcorona.screens.diary.new_entry.pickers

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TimePicker
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.diary.new_entry.DiaryNewEntryViewModel
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.argument
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.threeten.bp.LocalTime
import java.util.*

class TimePickerFragmentDialog : DialogFragment(), TimePickerDialog.OnTimeSetListener {

    companion object {
        private const val ARGUMENT_INPUT_ID = "argument_input_id"

        fun args(
            inputId: Int
        ): Bundle {
            return bundleOf(
                ARGUMENT_INPUT_ID to inputId
            )
        }
    }

    private val inputId: Int? by argument(ARGUMENT_INPUT_ID)

    private val viewModel: DiaryNewEntryViewModel by viewModel()

    private val calendar: Calendar = Calendar.getInstance()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        return TimePickerDialog(requireContext(), R.style.PickerDialogTheme, this, hour, minute, true)
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        val time = LocalTime.of(hourOfDay, minute)
        when(inputId) {
            R.id.textInputEditTextStartLocationTime -> viewModel.updatePublicTransportTime(time)
            R.id.textInputEditTextEventStartTime -> viewModel.updateEventStart(time)
            R.id.textInputEditTextEventEndTime -> viewModel.updateEventEnd(time)
        }
    }

}

fun Fragment.showTimePickerFragmentDialog(inputId: Int) {
    TimePickerFragmentDialog().apply {
        arguments = TimePickerFragmentDialog.args(inputId)
    }.show(requireFragmentManager(), TimePickerFragmentDialog::class.java.name)
}