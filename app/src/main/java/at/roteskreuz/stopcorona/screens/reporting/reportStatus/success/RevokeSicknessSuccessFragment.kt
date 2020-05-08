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
import at.roteskreuz.stopcorona.skeleton.core.utils.dip
import at.roteskreuz.stopcorona.skeleton.core.utils.dipif
import kotlinx.android.synthetic.main.fragment_revoke_sickness_success.*

class RevokeSicknessSuccessFragment : BaseFragment(R.layout.fragment_revoke_sickness_success) {

    companion object {
        const val SCROLLED_DISTANCE_THRESHOLD = 2 // dp
    }

    override val isToolbarVisible = true

    override fun getTitle(): String = requireContext().getString(R.string.revoke_sickness_title)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnFinish.setOnClickListener {
            activity?.finish()
        }

        scrollViewContainer.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            transparentAppBar.elevation = if (scrollY > requireContext().dip(SCROLLED_DISTANCE_THRESHOLD)) {
                requireContext().dipif(4)
            } else {
                0f
            }
        }
    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
    }
}

fun Fragment.startRevokeSicknessSuccessFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = RevokeSicknessSuccessFragment::class.java.name
    )
}
