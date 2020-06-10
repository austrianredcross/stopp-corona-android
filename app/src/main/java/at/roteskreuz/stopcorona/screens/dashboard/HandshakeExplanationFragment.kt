package at.roteskreuz.stopcorona.screens.dashboard

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.utils.startDefaultBrowser
import kotlinx.android.synthetic.main.fragment_handshake_explanation.*

class HandshakeExplanationFragment : BaseFragment(R.layout.fragment_handshake_explanation) {

    override val isToolbarVisible = true

    override fun getTitle() = getString(R.string.automatic_handshake_dialog_title)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnFaq.setOnClickListener {
            startDefaultBrowser(getString(R.string.automatic_handshake_dialog_faq_link))
        }
    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
    }
}

fun Fragment.startHandshakeExplanationFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = HandshakeExplanationFragment::class.java.name
    )
}
