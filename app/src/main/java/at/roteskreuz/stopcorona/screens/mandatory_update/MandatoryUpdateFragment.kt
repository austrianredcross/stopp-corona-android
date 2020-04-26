package at.roteskreuz.stopcorona.screens.mandatory_update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import kotlinx.android.synthetic.main.fragment_mandatory_update.*

class MandatoryUpdateFragment : BaseFragment(R.layout.fragment_mandatory_update) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnGoToPlaystore.setOnClickListener {
            val appPackageName = requireContext().packageName
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
            } catch (exc: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
            }
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
