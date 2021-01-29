package at.roteskreuz.stopcorona.screens.mandatory_update

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.utils.startPlatformAppStore
import kotlinx.android.synthetic.main.fragment_mandatory_update.*

class MandatoryUpdateFragment : BaseFragment(R.layout.fragment_mandatory_update) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtTitle.contentDescription = getString(R.string.mandatory_update_title) + getString(R.string.accessibility_heading_2)

        btnGoToPlaystore.setOnClickListener {
            val appPackageName = requireContext().packageName
            startPlatformAppStore(appPackageName)
        }
    }
}

fun Fragment.startMandatoryUpdateFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        clearTask = true,
        fragmentName = MandatoryUpdateFragment::class.java.name
    )
}

fun Context.startMandatoryUpdateFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        clearTask = true,
        fragmentName = MandatoryUpdateFragment::class.java.name
    )
}
