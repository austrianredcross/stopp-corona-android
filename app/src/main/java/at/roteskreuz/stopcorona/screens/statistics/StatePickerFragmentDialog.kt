package at.roteskreuz.stopcorona.screens.statistics

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.NumberPicker
import androidx.fragment.app.DialogFragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.entities.statistics.Bundesland
import org.koin.androidx.viewmodel.ext.android.viewModel

class StatePickerFragmentDialog : DialogFragment(), NumberPicker.OnValueChangeListener {

    private val viewModel: StatisticsViewModel by viewModel()

    private lateinit var entries : Map<Bundesland, String>
    private var selectedState: Bundesland = Bundesland.Oesterreich

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val numberPicker = NumberPicker(context)
        entries = mapOf(
            Bundesland.Oesterreich to requireContext().getString(R.string.covid_statistics_state_id_all),
            Bundesland.Wien to requireContext().getString(R.string.covid_statistics_state_id_9),
            Bundesland.Vorarlberg to requireContext().getString(R.string.covid_statistics_state_id_8),
            Bundesland.Tirol to requireContext().getString(R.string.covid_statistics_state_id_7),
            Bundesland.Steiermark to requireContext().getString(R.string.covid_statistics_state_id_6),
            Bundesland.Salzburg to requireContext().getString(R.string.covid_statistics_state_id_5),
            Bundesland.Oberoesterreich to requireContext().getString(R.string.covid_statistics_state_id_4),
            Bundesland.Niederoesterreich to requireContext().getString(R.string.covid_statistics_state_id_3),
            Bundesland.Kaernten to requireContext().getString(R.string.covid_statistics_state_id_2),
            Bundesland.Burgenland to requireContext().getString(R.string.covid_statistics_state_id_1)
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
        selectedState =
            entries.filterValues { (it == entries.values.toTypedArray()[newVal]) }.keys.elementAt(0)
    }

    private fun setValue() {
        viewModel.updateSelectedState(selectedState)
        dismiss()
    }
}