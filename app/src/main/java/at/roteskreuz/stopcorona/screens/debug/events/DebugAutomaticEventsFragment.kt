package at.roteskreuz.stopcorona.screens.debug.events

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.debug_automatic_events_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class DebugAutomaticEventsFragment : BaseFragment(R.layout.debug_automatic_events_fragment) {

    private val viewModel: DebugAutomaticEventsViewModel by viewModel()

    override val isToolbarVisible: Boolean
        get() = true

    override fun getTitle(): String? {
        return "Automatic events"
    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        disposables += viewModel.observe()
            .observeOnMainThread()
            .subscribe {
                txtResult.text = it
            }
    }
}

fun Activity.startDebugAutomaticEventsFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = DebugAutomaticEventsFragment::class.java.name
    )
}
