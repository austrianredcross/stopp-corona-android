package at.roteskreuz.stopcorona.utils

import android.graphics.Rect
import android.view.ViewTreeObserver
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment

/**
 * Keyboard helper to listen when the keyboard is being displayed or hidden.
 */
class KeyboardHelper(
    private val fragment: BaseFragment,
    onKeyboardShown: () -> Unit,
    onKeyboardHidden: () -> Unit
) {

    private var onGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener =
        ViewTreeObserver.OnGlobalLayoutListener {
            fragment.activity?.window?.decorView?.let { decorView ->
                val visibleDisplayFrame = Rect()
                decorView.getWindowVisibleDisplayFrame(visibleDisplayFrame)
                val screenHeight = decorView.context.resources.displayMetrics.heightPixels

                //https://stackoverflow.com/a/26964010/13782111
                val keypadHeight = screenHeight - visibleDisplayFrame.bottom

                if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                    // keyboard is opened
                    onKeyboardShown()
                }
                else {
                    // keyboard is closed
                    onKeyboardHidden()
                }
            }
        }

    /**
     * Call do start listening on keyboard shown event.
     */
    fun enable() {
        fragment.activity?.window?.decorView?.viewTreeObserver?.addOnGlobalLayoutListener(
            onGlobalLayoutListener
        )
    }

    /**
     * Call to stop listening on keyboard shown event.
     */
    fun disable() {
        fragment.activity?.window?.decorView?.viewTreeObserver?.removeOnGlobalLayoutListener(
            onGlobalLayoutListener
        )
    }
}
