package at.roteskreuz.stopcorona.screens.base

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.annotation.LayoutRes
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.isDebug

/**
 * CoronaBaseActivity fixed to Portrait.
 *
 * Use android:screenOrientation="portrait" in Android Manifest.
 */
open class CoronaPortraitBaseActivity(@LayoutRes layout: Int = R.layout.framelayout) : CoronaBaseActivity(layout) {

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = if (!isDebug) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}
