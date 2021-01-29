package at.roteskreuz.stopcorona.screens.dashboard.privacy_update

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.webView.WebViewWithAssetsResourcesFragment
import at.roteskreuz.stopcorona.utils.getClickableBoldUrlSpan
import kotlinx.android.synthetic.main.fragment_privacy_update.*
import kotlinx.android.synthetic.main.fragment_privacy_update.txtTitle
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Screen to accepted new data privacy.
 */
class PrivacyUpdateFragment : DialogFragment() {

    private val viewModel: PrivacyUpdateViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        isCancelable = false
        return inflater.inflate(R.layout.fragment_privacy_update, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtTitle.contentDescription = getString(R.string.interoperability_title) + getString(R.string.accessibility_heading_2)

        val builder = SpannableStringBuilder()
        builder.append(context?.getString(R.string.interoperability_info_content_1))
        builder.append(
            context?.getClickableBoldUrlSpan(
                R.string.interoperability_info_content_2,
                colored = true,
                url = WebViewWithAssetsResourcesFragment.DEEP_LINK_PRIVACY
            )
        )
        builder.append(context?.getString(R.string.interoperability_info_content_3))
        builder.append(
            context?.getClickableBoldUrlSpan(
                R.string.interoperability_info_content_4,
                colored = true,
                url = getString(R.string.start_menu_item_1_2_red_cross_link_link_target),
                drawableRes = R.drawable.ic_external_link
            )
        )

        txtDescription.text = builder
        txtDescription.movementMethod = LinkMovementMethod()

        btnToAccept.setOnClickListener {
            viewModel.markNewPrivacyAsAccepted()
            dismiss()
        }

    }

    // use Apptheme for full screen
    override fun getTheme(): Int {
        return R.style.AppTheme
    }

}

fun Fragment.showPrivacyUpdateFragment() {
    PrivacyUpdateFragment().show(requireFragmentManager(), PrivacyUpdateFragment::class.java.name)
}