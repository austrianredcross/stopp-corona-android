package at.roteskreuz.stopcorona.screens.diary.day

import org.threeten.bp.LocalDate
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.screens.diary.delete_entry.showDiaryDeleteEntryFragment
import at.roteskreuz.stopcorona.screens.diary.new_entry.showDiaryNewEntryFragment
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.argument
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import at.roteskreuz.stopcorona.utils.format
import at.roteskreuz.stopcorona.utils.string
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.diary_day_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.Serializable

class DiaryDayFragment : BaseFragment(R.layout.diary_day_fragment) {

    companion object {
        private const val ARGUMENT_SELECTED_DAY = "argument_selected_day"

        fun args(
            date: Serializable
        ): Bundle {
            return bundleOf(
                ARGUMENT_SELECTED_DAY to date
            )
        }
    }

    private val day: LocalDate? by argument(ARGUMENT_SELECTED_DAY)

    private val viewModel: DiaryDayViewModel by viewModel()

    override val isToolbarVisible: Boolean = true

    private val controller: DiaryDayController by lazy {
        DiaryDayController(
            requireContext(),
            onDeleteDiaryEntryClick = {
                showDiaryDeleteEntryFragment(it)
            },
            onAddDiaryEntryClick = {
                day?.let { showDiaryNewEntryFragment(it) }
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(contentRecyclerView) {
            setController(controller)
        }

        day?.let { day ->
            disposables += viewModel.observeDiaryEntryForDate(day)
                .observeOnMainThread()
                .subscribe { entries ->
                    val diaryList = entries.toList()
                    controller.entries = diaryList
                    controller.requestModelBuild()
                }
        }

    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
        toolbar?.setNavigationContentDescription(R.string.general_back)
    }

    override fun getTitle(): String? {
        return day?.format(requireContext().string(R.string.diary_date_format))
    }
}

fun Fragment.startDiaryDayFragment(day: LocalDate) {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = DiaryDayFragment::class.java.name,
        fragmentArgs = DiaryDayFragment.args(day)
    )
}