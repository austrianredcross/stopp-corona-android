package at.roteskreuz.stopcorona.screens.dashboard.changelog

import android.os.Bundle
import android.view.View
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.Constants.Misc.VERSION_NAME
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class ChangelogFragment : BaseFragment(R.layout.fragment_changelog) {

    private val viewModel: ChangelogViewModel by viewModel()

    private val controller: ChangelogController by lazy {
        ChangelogController(
            context = requireContext()
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewModel.getChangelogForVersion(VERSION_NAME)) {
            controller.changelog = this
        }
    }
}
