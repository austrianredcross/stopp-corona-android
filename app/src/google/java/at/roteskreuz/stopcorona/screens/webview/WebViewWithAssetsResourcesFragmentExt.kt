package at.roteskreuz.stopcorona.screens.webview

import android.content.res.Configuration
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import at.roteskreuz.stopcorona.screens.webView.WebViewWithAssetsResourcesFragment
import kotlinx.android.synthetic.main.webview_fragment.*

fun WebViewWithAssetsResourcesFragment.setDarkMode() {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
        when (context?.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {
                WebSettingsCompat.setForceDark(webView.settings,
                    WebSettingsCompat.FORCE_DARK_ON)
            }
        }
    }
}