package at.roteskreuz.stopcorona.skeleton.core.utils

import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import at.roteskreuz.stopcorona.skeleton.core.R
import at.roteskreuz.stopcorona.skeleton.core.constants.BaseAppRequest
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import com.google.android.material.snackbar.Snackbar

/**
 * Helper interface to check and request android permissions if needed.
 */
interface PermissionChecker {

    /**
     * List of permissions required by fragment.
     */
    val requiredPermissions: List<String>

    /**
     * Flag to indicate if asking a permission is performed immediately after fragment view is created.
     */
    val askForPermissionOnViewCreated: Boolean
        get() = true

    /**
     * Check if all [requiredPermissions] has been already granted.
     */
    fun checkAllPermissionsGranted(context: Context): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Called if permission is granted.
     */
    fun onPermissionGranted(permission: String)

    /**
     * Called if permission is not granted.
     */
    fun <T : BaseFragment> T.onPermissionDenied(permission: String, neverAskAgain: Boolean) {
        if (neverAskAgain) {
            PermissionDeniedDialog().show()
        } else {
            view?.let { view ->
                Snackbar.make(
                    view,
                    getString(R.string.general_permission_denied),
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(getString(R.string.general_again)) {
                    checkPermissions()
                }.show()
            }
        }
    }

    /**
     * Checking [requiredPermissions] state.
     * If permission is already granted, just call [onPermissionGranted].
     * Else request permission.
     */
    fun <T : BaseFragment> T.checkPermissions() {
        val permissionsToBeRequested = mutableListOf<String>()
        requiredPermissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permission)
            } else {
                permissionsToBeRequested += permission
            }
        }
        if (permissionsToBeRequested.isNotEmpty()) {
            requestPermissions(permissionsToBeRequested.toTypedArray(), BaseAppRequest.REQUEST_PERMISSION)
        }
    }

    /**
     * Process results of [requiredPermissions].
     * If permission is granted, just call [onPermissionGranted].
     * Else call [onPermissionDenied].
     */
    fun <T : BaseFragment> T.processRequestedPermissions(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == BaseAppRequest.REQUEST_PERMISSION) {
            for (i in 0 until grantResults.size) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    onPermissionGranted(permissions[i])
                } else {
                    val neverAskAgain =
                        !ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permissions[i])
                    onPermissionDenied(permissions[i], neverAskAgain)
                }
            }
        }
    }

    /**
     * Dialog shown when some permission has been denied.
     */
    class PermissionDeniedDialog : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.general_permission_denied))
                .setMessage(getString(R.string.general_permission_denied_description))
                .setPositiveButton(R.string.general_ok, null)
                .show()
        }
    }
}