package at.roteskreuz.stopcorona.screens.debug.contact_tracing

import android.app.Activity
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment

class DebugContactNotificationsFragment : BaseFragment(R.layout.debug_contact_tracing_fragment) {

    override val isToolbarVisible: Boolean
        get() = true

    override fun getTitle(): String? {
        return "Exposure Tracing"
    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
    }
}

fun Activity.startDebugExposureNotificationsFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = DebugContactNotificationsFragment::class.java.name
    )
}