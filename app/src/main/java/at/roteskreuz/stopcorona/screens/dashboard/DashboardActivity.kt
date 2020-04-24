package at.roteskreuz.stopcorona.screens.dashboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.getFragmentActivityIntent
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity

/**
 * Activity for the dashboard screen.
 */
class DashboardActivity : CoronaPortraitBaseActivity() {

    override val fragmentName: String?
        get() = super.fragmentName ?: DashboardFragment::class.java.name

}

fun Activity.startDashboardActivity() {
    startFragmentActivity<DashboardActivity>()
}

fun Context.getDashboardActivityIntent(): Intent {
    return getFragmentActivityIntent<DashboardActivity>(this)
}

fun Fragment.goBackToDashboardActivity() {
    startActivity(
        requireContext().getDashboardActivityIntent()
            .apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK) }
    )
}