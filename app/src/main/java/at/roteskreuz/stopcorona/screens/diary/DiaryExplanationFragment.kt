package at.roteskreuz.stopcorona.screens.diary

import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.utils.string

class DiaryExplanationFragment : BaseFragment(R.layout.fragment_diary_explanation) {

    override val isToolbarVisible: Boolean = true

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
        toolbar?.setNavigationContentDescription(R.string.general_back)
    }

    override fun getTitle(): String? {
        return requireContext().string(R.string.dairy_faq_title)
    }
}

fun Fragment.startDiaryExplanationFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = DiaryExplanationFragment::class.java.name
    )
}