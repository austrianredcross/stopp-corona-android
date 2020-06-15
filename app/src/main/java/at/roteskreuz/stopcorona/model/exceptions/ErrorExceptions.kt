package at.roteskreuz.stopcorona.model.exceptions

import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.api.ApiError
import at.roteskreuz.stopcorona.screens.base.dialog.GeneralErrorDialog
import at.roteskreuz.stopcorona.screens.mandatory_update.startMandatoryUpdateFragment
import at.roteskreuz.stopcorona.skeleton.core.model.exceptions.GeneralServerException
import at.roteskreuz.stopcorona.skeleton.core.model.exceptions.NoInternetConnectionException
import at.roteskreuz.stopcorona.skeleton.core.model.exceptions.UnexpectedError
import timber.log.Timber

/**
 * Extension for handling general errors in Corona app domain inspired by [handleBaseErrors] from skeleton.
 * Everything is logged, when use this method, there is no need to log it again.
 */
fun Fragment.handleBaseCoronaErrors(error: Throwable) {
    when (error) {
        is NoInternetConnectionException -> {
            Timber.i(error, "No internet connection or server timeout")
            GeneralErrorDialog(R.string.error_no_internet_title, R.string.error_no_internet_message)
                .show(childFragmentManager, GeneralErrorDialog::class.java.name)
        }
        is GeneralServerException -> {
            Timber.e(error, "Unhandled server exception")
            GeneralErrorDialog(R.string.error_server_title, R.string.error_server_message)
                .show(childFragmentManager, GeneralErrorDialog::class.java.name)
        }
        is UnexpectedError -> {
            Timber.e(error, "Unhandled unknown exception")
            GeneralErrorDialog(R.string.error_unknown_title, R.string.error_unknown_message)
                .show(childFragmentManager, GeneralErrorDialog::class.java.name)
        }
        is DataFetchFailedException -> {
            Timber.e(error, "Fetch failed exception")
            GeneralErrorDialog(R.string.error_unknown_title, R.string.error_unknown_message)
                .show(childFragmentManager, GeneralErrorDialog::class.java.name)
        }
        is ApiError.Critical.ForceUpdate -> {
            Timber.w(error, "Force update exception")
            startMandatoryUpdateFragment()
        }
        else -> {
            Timber.e(error, "Unhandled else exception")
            GeneralErrorDialog(R.string.error_unknown_title, R.string.error_unknown_message)
                .show(childFragmentManager, GeneralErrorDialog::class.java.name)
        }
    }
}
