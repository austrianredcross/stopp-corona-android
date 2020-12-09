package at.roteskreuz.stopcorona.screens.reporting.reportStatus.success

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import kotlinx.android.synthetic.main.fragment_revoke_suspicion_success.*

class RevokeSuspicionSuccessFragment : BaseFragment(R.layout.fragment_revoke_suspicion_success) {

    override val isToolbarVisible = true

    override fun getTitle(): String = requireContext().getString(R.string.revoke_suspicion_title)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtHeadline.contentDescription = getString(R.string.revoke_suspicion_success_headline) + getString(R.string.accessibility_heading_1)

        btnFinish.setOnClickListener {
            activity?.finish()
        }
    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
        toolbar?.setNavigationContentDescription(R.string.general_back)
    }
}

fun Fragment.startRevokeSuspicionSuccessFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = RevokeSuspicionSuccessFragment::class.java.name
    )
}
