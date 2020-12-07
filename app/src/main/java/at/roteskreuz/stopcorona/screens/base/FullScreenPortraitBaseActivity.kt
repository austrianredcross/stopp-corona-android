package at.roteskreuz.stopcorona.screens.base

import android.os.Bundle
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.webView.WebViewWithAssetsResourcesFragment


/**
 * Activity with fullscreen theme.
 */
open class FullScreenPortraitBaseActivity : CoronaPortraitBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        handleDeepLinkIntent()
        super.onCreate(savedInstanceState)

    }

    /**
     * Handles deep link data, if activity is started through an deep link
     * Opens the corresponding WebView fragment if "privacy" or "terms-of-use" is set as last path
     * segment of the deep link url
     */
    private fun handleDeepLinkIntent() {
        val data = intent.data
        data?.let {
            if (it.host == WebViewWithAssetsResourcesFragment.DEEP_LINK_NAME_HOST) {
                if (data.lastPathSegment == WebViewWithAssetsResourcesFragment.DEEP_LINK_NAME_PRIVACY) {
                    intent.putExtra(
                        ARGUMENT_FRAGMENT_ARGUMENTS,
                        WebViewWithAssetsResourcesFragment.args(
                            titleRes = R.string.onboarding_headline_data_privacy,
                            assetsResourceName = data.lastPathSegment
                        )
                    )
                } else if (data.lastPathSegment == WebViewWithAssetsResourcesFragment.DEEP_LINK_NAME_TERMS_OF_USE) {
                    intent.putExtra(
                        ARGUMENT_FRAGMENT_ARGUMENTS,
                        WebViewWithAssetsResourcesFragment.args(
                            titleRes = R.string.onboarding_headline_terms_of_use,
                            assetsResourceName = data.lastPathSegment
                        )
                    )
                } else {
                    return
                }
                intent.putExtra(
                    ARGUMENT_FRAGMENT_NAME,
                    WebViewWithAssetsResourcesFragment::class.java.name
                )
            }
        }
    }
}