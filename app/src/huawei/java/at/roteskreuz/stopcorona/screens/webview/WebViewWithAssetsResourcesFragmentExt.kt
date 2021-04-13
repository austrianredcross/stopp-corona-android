package at.roteskreuz.stopcorona.screens.webview

import android.content.res.Configuration
import android.os.Build
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import at.roteskreuz.stopcorona.screens.webView.WebViewWithAssetsResourcesFragment
import kotlinx.android.synthetic.main.webview_fragment.*

fun WebViewWithAssetsResourcesFragment.setDarkMode() {
    if(context?.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES)
        return

    //Huawei Android 10 and above have dark mode supported, don't use androidx WebViewFeature API, since it doesn't work
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        webView.settings.forceDark = android.webkit.WebSettings.FORCE_DARK_ON
    } else { //previous to Android 10, we rely on androidx WebViewFeature API, since Huawei phones should still have GMS webview
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_ON)
        }
    }
}