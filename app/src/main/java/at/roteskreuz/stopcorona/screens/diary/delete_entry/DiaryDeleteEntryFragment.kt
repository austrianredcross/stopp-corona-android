package at.roteskreuz.stopcorona.screens.diary.delete_entry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.dialog.FullWidthDialog
import at.roteskreuz.stopcorona.screens.diary.new_entry.DiaryNewEntryFragment
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.argument
import kotlinx.android.synthetic.main.diary_delete_entry_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class DiaryDeleteEntryFragment : FullWidthDialog() {

    companion object {
        private const val ARGUMENT_ID_TO_DELETE = "argument_id_to_delete"

        fun args(
            id: Long
        ): Bundle {
            return bundleOf(
                ARGUMENT_ID_TO_DELETE to id
            )
        }
    }

    private val id: Long? by argument(ARGUMENT_ID_TO_DELETE)

    private val viewModel: DiaryDeleteEntryViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.diary_delete_entry_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnDelete.setOnClickListener {
            id?.let { it1 ->
                viewModel.deleteDiaryEntry(it1)
                dismiss()
            }
        }
    }
}

fun Fragment.showDiaryDeleteEntryFragment(id: Long) {
    DiaryDeleteEntryFragment().apply {
        arguments = DiaryDeleteEntryFragment.args(id)
    }.show(
        requireFragmentManager(),
        DiaryDeleteEntryFragment::class.java.name
    )
}