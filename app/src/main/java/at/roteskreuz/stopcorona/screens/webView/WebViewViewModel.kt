package at.roteskreuz.stopcorona.screens.webView

import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.webkit.*
import androidx.annotation.IntRange
import at.roteskreuz.stopcorona.constants.Constants.Misc.EMPTY_STRING
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import com.github.dmstocking.optional.java.util.Optional
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import java.lang.ref.WeakReference

/**
 * Handles webView states.
 */
open class WebViewViewModel(
    appDispatchers: AppDispatchers
) : ScopedViewModel(appDispatchers) {

    protected val webViewState = WebViewStateObserver()

    private val webViewChromeClient = BaseWebViewChromeClient(WeakReference(webViewState))
    protected open val webViewClient: WebViewClient = BaseWebViewClient(
        WeakReference(webViewState)
    )

    /**
     * Set up basic web view functionality.
     */
    open fun setUp(webView: WebView?) {
        webView?.webViewClient = webViewClient
        webView?.webChromeClient = webViewChromeClient
        webView?.settings?.javaScriptEnabled = false
    }

    /**
     * Release basic web view functionality to not leak some memory.
     */
    open fun release(webView: WebView?) {
        webView?.webViewClient = null
        webView?.webChromeClient = null
        webView?.settings?.javaScriptEnabled = false
    }

    /**
     * Can throw errors of type [WebViewError].
     */
    fun observeWebViewState(): Observable<WebViewStateObserver.State> = webViewState.observe()
}

open class BaseWebViewClient(
    private val stateObserver: WeakReference<WebViewStateObserver>
) : WebViewClient() {

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        stateObserver.get()?.loaded(url)
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        stateObserver.get()?.error(WebViewError.General(request, error))
    }

    override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
        super.onReceivedHttpError(view, request, errorResponse)
        stateObserver.get()?.error(WebViewError.Http(request, errorResponse))
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        super.onReceivedSslError(view, handler, error)
        stateObserver.get()?.error(WebViewError.SSL(error))
    }
}

class BaseWebViewChromeClient(
    private val stateObserver: WeakReference<WebViewStateObserver>
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        stateObserver.get()?.progress(newProgress)
    }
}

class WebViewStateObserver {
    private val progressSubject = NonNullableBehaviorSubject(0)
    private val urlSubject = NonNullableBehaviorSubject(EMPTY_STRING)
    private val errorSubject = NonNullableBehaviorSubject<Optional<WebViewError>>(Optional.ofNullable(null))

    fun progress(progress: Int) {
        if (progress < 100 && errorSubject.value.isPresent) {
            // on new loading the error state is reset
            errorSubject.onNext(Optional.ofNullable(null))
        }
        progressSubject.onNext(progress)
    }

    fun loaded(url: String) {
        urlSubject.onNext(url)
    }

    fun error(error: WebViewError) {
        errorSubject.onNext(Optional.of(error))
    }

    /**
     * Emit:
     * - loading from 0 to 100
     * - once loading is 100, loaded url
     * - error if happened
     */
    fun observe(): Observable<State> {
        return Observables.combineLatest(
            progressSubject,
            urlSubject,
            errorSubject
        ).map { (progress, url, error) ->
            when {
                error.isPresent -> State.Error(error.get())
                progress < 100 -> State.Loading(progress)
                url.isNotBlank() -> State.UrlLoaded(url)
                else -> State.Idle
            }
        }
    }

    sealed class State {
        object Idle : State()
        data class Loading(@IntRange(from = 0, to = 100) val progress: Int) : State()
        data class UrlLoaded(val url: String) : State()
        data class Error(val webViewError: WebViewError) : State()
    }
}

sealed class WebViewError(messageDescription: String) : Throwable(messageDescription) {
    data class General(
        val request: WebResourceRequest?,
        val webResourceError: WebResourceError?
    ) : WebViewError("WebView error: ${request?.getUrlOrNull()} ${webResourceError?.getErrorCodeOrNull()} ${webResourceError?.getDescOrNull()}")

    data class Http(
        val request: WebResourceRequest?,
        val errorResponse: WebResourceResponse?
    ) : WebViewError("WebView http error: ${request?.getUrlOrNull()} $errorResponse")

    data class SSL(
        val error: SslError?
    ) : WebViewError("WebView ssl error: $error")


}

fun WebResourceRequest?.getUrlOrNull(): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        this?.url
    } else {
        null
    }
}

fun WebResourceError?.getErrorCodeOrNull(): Int? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        this?.errorCode
    } else {
        null
    }
}

fun WebResourceError?.getDescOrNull(): CharSequence? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        this?.description
    } else {
        null
    }
}
