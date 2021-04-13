package at.roteskreuz.stopcorona.screens.diary.new_entry.pickers

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.NumberPicker
import androidx.fragment.app.DialogFragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.repositories.ContactEntry
import at.roteskreuz.stopcorona.screens.diary.new_entry.DiaryNewEntryViewModel
import at.roteskreuz.stopcorona.utils.string
import org.koin.androidx.viewmodel.ext.android.viewModel

class NumberPickerFragmentDialog : DialogFragment(), NumberPicker.OnValueChangeListener {

    private val viewModel: DiaryNewEntryViewModel by viewModel()

    private lateinit var entries : Map<ContactEntry,String>
    private var selectedContactEntry: ContactEntry = ContactEntry.Person

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val numberPicker = NumberPicker(context)
        entries = mapOf(
            ContactEntry.Person to requireContext().string(R.string.diary_add_person_pickerview),
            ContactEntry.Location to requireContext().string(R.string.diary_add_location_pickerview),
            ContactEntry.PublicTransport to requireContext().string(R.string.diary_add_public_transport_pickerview),
            ContactEntry.Event to requireContext().string(R.string.diary_add_event_pickerview)
        )
        numberPicker.minValue = 0
        numberPicker.maxValue = entries.size - 1
        numberPicker.displayedValues = entries.values.toTypedArray()
        numberPicker.setOnValueChangedListener(this)

        val builder = AlertDialog.Builder(requireContext())
            .setPositiveButton(R.string.general_ok) { _, _ -> setValue() }
            .setNegativeButton(R.string.general_cancel) { _, _ -> dismiss() }
            .setView(numberPicker)

        builder.create()
        return builder.show()
    }


    override fun onValueChange(picker: NumberPicker?, oldVal: Int, newVal: Int) {
        selectedContactEntry =
            entries.filterValues { (it == entries.values.toTypedArray()[newVal]) }.keys.elementAt(0)
    }

    private fun setValue() {
        viewModel.updateSelectedContactEntry(selectedContactEntry)
        dismiss()
    }
}