package at.roteskreuz.stopcorona.screens.diary.new_entry

import android.os.Bundle
import android.view.*
import android.widget.ToggleButton
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.repositories.ContactEntry
import at.roteskreuz.stopcorona.screens.base.dialog.FullWidthDialog
import at.roteskreuz.stopcorona.screens.diary.new_entry.pickers.NumberPickerFragmentDialog
import at.roteskreuz.stopcorona.screens.diary.new_entry.pickers.showTimePickerFragmentDialog
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.argument
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import at.roteskreuz.stopcorona.utils.displayFieldInlineError
import at.roteskreuz.stopcorona.utils.format
import at.roteskreuz.stopcorona.utils.listenForTextChanges
import at.roteskreuz.stopcorona.utils.string
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.diary_new_entry_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.threeten.bp.LocalDate
import java.io.Serializable

class DiaryNewEntryFragment : FullWidthDialog() {

    companion object {
        private const val ARGUMENT_DAY = "argument_day"

        fun args(
            date: Serializable
        ): Bundle {
            return bundleOf(
                ARGUMENT_DAY to date
            )
        }
    }

    private val day: LocalDate? by argument(ARGUMENT_DAY)

    private val viewModel: DiaryNewEntryViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.diary_new_entry_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.selectedDay = day

        disposables += viewModel.observeSelectedContactEntry()
            .observeOnMainThread()
            .subscribe { selectedContactEntry ->
                viewModel.selectedContactEntry = selectedContactEntry
                personWrapper.visibility = View.GONE
                locationWrapper.visibility = View.GONE
                publicTransportWrapper.visibility = View.GONE
                eventWrapper.visibility = View.GONE

                when (selectedContactEntry) {
                    is ContactEntry.Person -> {
                        personWrapper.visibility = View.VISIBLE
                        numberPicker.text = requireContext().string(R.string.diary_add_person_pickerview)
                    }
                    is ContactEntry.Location -> {
                        locationWrapper.visibility = View.VISIBLE
                        numberPicker.text = requireContext().string(R.string.diary_add_location_pickerview)
                    }
                    is ContactEntry.PublicTransport -> {
                        publicTransportWrapper.visibility = View.VISIBLE
                        numberPicker.text = requireContext().string(R.string.diary_add_public_transport_pickerview)
                    }
                    is ContactEntry.Event -> {
                        eventWrapper.visibility = View.VISIBLE
                        numberPicker.text = requireContext().string(R.string.diary_add_event_pickerview)
                    }
                }
            }

        disposables += viewModel.observeValidationResult()
            .observeOnMainThread()
            .subscribe { validationResult ->
                displayFieldInlineError(
                    validationResult.description,
                    textInputLayoutDesc,
                    requireContext()
                )
            }

        numberPicker.setOnClickListener {
            NumberPickerFragmentDialog().show(
                requireFragmentManager(),
                NumberPickerFragmentDialog::class.java.name
            )
        }

        val timeOfDayToggleBtns = arrayListOf<ToggleButton>(
            view.findViewById(R.id.toggleMorning),
            view.findViewById(R.id.toggleNoon),
            view.findViewById(R.id.toggleAfternoon),
            view.findViewById(R.id.toggleEvening)
        )
        timeOfDayToggleBtns.forEach {
            it.setOnCheckedChangeListener { v, isChecked ->
                if (isChecked) {
                    viewModel.setLocationTimeOfDay(v.text.toString())
                    timeOfDayToggleBtns.filter { it != v && it.isChecked }
                        .forEach { it.isChecked = false }
                } else {
                    viewModel.setLocationTimeOfDay(null)
                }
            }
        }

        textInputEditTextStartLocationTime.setOnClickListener {
            showTimePickerFragmentDialog(it.id)
        }
        disposables += viewModel.observePublicTransportTime()
            .observeOnMainThread()
            .subscribe { time ->
                viewModel.publicTransportTime = time
                textInputEditTextStartLocationTime.setText(time.format(requireContext().string(R.string.diary_time_format)))
            }

        textInputEditTextEventStartTime.setOnClickListener {
            showTimePickerFragmentDialog(it.id)
        }
        disposables += viewModel.observeEventStart()
            .observeOnMainThread()
            .subscribe { time ->
                viewModel.eventStart = time
                textInputEditTextEventStartTime.setText(time.format(requireContext().string(R.string.diary_time_format)))
            }

        textInputEditTextEventEndTime.setOnClickListener {
            showTimePickerFragmentDialog(it.id)
        }
        disposables += viewModel.observeEventEnd()
            .observeOnMainThread()
            .subscribe { time ->
                viewModel.eventEnd = time
                textInputEditTextEventEndTime.setText(time.format(requireContext().string(R.string.diary_time_format)))
            }

        btnClose.setOnClickListener {
            dismiss()
        }

        saveEntryBtn.setOnClickListener {
            viewModel.validate { onSaved() }
        }

        listenForTextChanges(textInputLayoutDesc, textInputEditTextDesc, viewModel::setDescription)
        listenForTextChanges(textInputLayoutPersonNotes, textInputEditTextPersonNotes, viewModel::setPersonNotes)
        listenForTextChanges(textInputLayoutStartLocation, textInputEditTextStartLocation, viewModel::setPublicTransportStart)
        listenForTextChanges(textInputLayoutEndLocation, textInputEditTextEndLocation, viewModel::setPublicTransportEnd)

        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }

    private fun onSaved(){
        dismiss()
    }
}

fun Fragment.showDiaryNewEntryFragment(day: LocalDate) {
    DiaryNewEntryFragment().apply {
        arguments = DiaryNewEntryFragment.args(day)
    }.show(requireFragmentManager(), DiaryNewEntryFragment::class.java.name)
}