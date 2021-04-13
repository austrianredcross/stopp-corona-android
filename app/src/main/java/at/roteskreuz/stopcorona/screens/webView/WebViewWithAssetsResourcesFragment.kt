package at.roteskreuz.stopcorona.screens.webView

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.BuildConfig
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.screens.base.FullScreenPortraitBaseActivity
import at.roteskreuz.stopcorona.screens.webview.setDarkMode
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.argument
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import at.roteskreuz.stopcorona.utils.darkTextInStatusBar
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.webview_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

/**
 * Screen displaying content from assets in webView.
 */
open class WebViewWithAssetsResourcesFragment : BaseFragment(R.layout.webview_fragment) {

    companion object {
        private const val ARGUMENT_TITLE_RES = "title_res"
        private const val ASSETS_RESOURCE_NAME = "assets_resource_name"

        const val DEEP_LINK_NAME_SCHEME = BuildConfig.DEEPLINK_SCHEME
        const val DEEP_LINK_NAME_HOST = BuildConfig.DEEPLINK_HOST
        const val DEEP_LINK_NAME_PRIVACY = "privacy"
        const val DEEP_LINK_NAME_TERMS_OF_USE = "terms-of-use"

        const val DEEP_LINK_TERMS_OF_USE =
            "$DEEP_LINK_NAME_SCHEME://$DEEP_LINK_NAME_HOST/$DEEP_LINK_NAME_TERMS_OF_USE"
        const val DEEP_LINK_PRIVACY =
            "$DEEP_LINK_NAME_SCHEME://$DEEP_LINK_NAME_HOST/$DEEP_LINK_NAME_PRIVACY"


        fun args(
            @StringRes titleRes: Int?,
            assetsResourceName: String?
        ): Bundle {
            return bundleOf(
                ARGUMENT_TITLE_RES to titleRes,
                ASSETS_RESOURCE_NAME to assetsResourceName
            )
        }
    }

    private val webViewTitleRes: Int? by argument(ARGUMENT_TITLE_RES)
    private val assetsResource: String? by argument(ASSETS_RESOURCE_NAME)

    protected open val viewModel: WebViewViewModel by viewModel()

    override val isToolbarVisible: Boolean = true

    override fun getTitle(): String? {
        return webViewTitleRes?.let { getString(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.darkTextInStatusBar()

        setDarkMode()

        val language = getString(R.string.current_language)
        with(webView) {
            viewModel.setUp(this)

            if (savedInstanceState == null) {
                assetsResource?.let {
                    webView.loadUrl("file:///android_asset/${assetsResource}_$language.html")
                }
            } else {
                restoreState(savedInstanceState)
            }
        }

        disposables += viewModel.observeWebViewState()
            .observeOnMainThread()
            .subscribe { state ->
                when (state) {
                    is WebViewStateObserver.State.Loading -> {
                        webViewProgressBar.progress = state.progress
                        if (webViewProgressBar.isVisible.not()) {
                            webViewProgressBar.show()
                        }
                    }
                    is WebViewStateObserver.State.UrlLoaded -> {
                        webViewProgressBar.hide()
                    }
                    is WebViewStateObserver.State.Error -> {
                        processWebViewError(state, view)
                    }
                }
            }
    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
        toolbar?.setNavigationContentDescription(R.string.general_back)
    }

    override fun onDestroyView() {
        viewModel.release(webView)
        super.onDestroyView()
    }

    override fun overrideOnBackPressed(): Boolean {
        return if (webView.canGoBack()) {
            webView.goBack()
            true
        } else super.overrideOnBackPressed()
    }

    protected open fun processWebViewError(state: WebViewStateObserver.State.Error, rootView: View) {
        Timber.e(SilentError(state.webViewError))
    }
}

/**
 * Start a web view.
 * For best Internationalization, the title is set from the [titleRes].
 *
 * @param titleRes String resource for the Title. (Title will be changed on language changes)
 * @param assetsResourceName raw resource file to open
 */
fun Fragment.startWebView(
    @StringRes titleRes: Int? = null,
    assetsResourceName: String
) {
    startFragmentActivity<FullScreenPortraitBaseActivity>(
        fragmentName = WebViewWithAssetsResourcesFragment::class.java.name,
        fragmentArgs = WebViewWithAssetsResourcesFragment.args(
            titleRes = titleRes,
            assetsResourceName = assetsResourceName
        )
    )
}
