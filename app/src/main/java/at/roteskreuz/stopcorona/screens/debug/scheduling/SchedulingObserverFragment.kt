package at.roteskreuz.stopcorona.screens.debug.scheduling

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import kotlinx.android.synthetic.main.debug_scheduling_observer_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Sample dashboard.
 */
class SchedulingObserverFragment : BaseFragment(R.layout.debug_scheduling_observer_fragment) {

    override val isToolbarVisible: Boolean = true

    override fun getTitle(): String? {
        return "Scheduling observer"
    }

    private val viewModel: SchedulingObserverViewModel by viewModel()

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
        toolbar?.setNavigationContentDescription(R.string.general_back)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.observeExposureMatchingState()
            .observe(this, Observer { workInfos ->
                if (workInfos.isNotEmpty()) {
                    txtExposureMatchingWorkerState.text = workInfos.joinToString("\n")
                }
            })
    }
}

fun Activity.startDebugSchedulingObserverFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = SchedulingObserverFragment::class.java.name
    )
}