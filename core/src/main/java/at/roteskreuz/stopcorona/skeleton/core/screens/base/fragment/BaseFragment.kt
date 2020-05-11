package at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import at.roteskreuz.stopcorona.skeleton.core.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.BaseActivity
import at.roteskreuz.stopcorona.skeleton.core.utils.PermissionChecker
import at.roteskreuz.stopcorona.skeleton.core.utils.hideKeyboard
import io.reactivex.disposables.CompositeDisposable
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Fragment from which all applications fragment should inherit.
 */
abstract class BaseFragment(
    @LayoutRes private val layoutId: Int
) : Fragment() {

    val baseActivity: BaseActivity?
        get() = activity as? BaseActivity

    protected var disposables = CompositeDisposable()

    /**
     * Indicator if this fragment has its own toolbar.
     */
    open val isToolbarVisible: Boolean = false

    /**
     * Find toolbar that has to have specific id R.id.toolbar.
     */
    open val providedToolbar: Toolbar?
        get() = view?.findViewById(R.id.toolbar)

    init {
        if (arguments == null) {
            arguments = Bundle()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(layoutId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /**
         * Ask for permissions requested by fragment if needed.
         */
        if (this is PermissionChecker && this.askForPermissionOnViewCreated) {
            checkPermissions()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        if (isToolbarVisible) {
            baseActivity?.setSupportActionBar(providedToolbar)
        }
        onInitActionBar(baseActivity?.supportActionBar, providedToolbar)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        /**
         * Check if requested permissions have been granted.
         */
        if (this is PermissionChecker) {
            processRequestedPermissions(requestCode, permissions, grantResults)
        }
    }

    /**
     * Method that is called when actionbar (toolbar) should be initialized for this fragment.
     *
     * @param actionBar of activity
     * @param toolbar   representation of ActionBar
     */
    open fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        actionBar?.setDisplayShowTitleEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                activity?.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        setTitle(getTitle())
    }

    override fun onDestroyView() {
        // hide keyboard if it is shown
        view?.findFocus()?.hideKeyboard()
        disposables.clear()
        super.onDestroyView()
    }

    /**
     * Indicator if fragment handles [android.app.Activity.onBackPressed] call from activity. Useful for form fragments when
     * you want to show user warning dialog when back button is pressed.
     * Return true if the backPress was consumed by fragment, else to propagate to the activity.
     */
    open fun overrideOnBackPressed(): Boolean {
        return false
    }

    /**
     * Set title of toolbar.
     */
    protected fun setTitle(@StringRes title: Int) {
        setTitle(getString(title))
    }

    /**
     * Set title of toolbar.
     */
    protected open fun setTitle(title: String?) {
        // intentional not-safe way to call activity, we want to crash an app when activity is null because
        // we are probably doing something bad
        if (title != null) {
            activity!!.title = title
        }
    }

    /**
     * Get title of screen. Children should override this if they want custom title.
     * If null, the title will not be overwritten.
     */
    open fun getTitle(): String? {
        return null
    }

    /**
     * Show progress dialog with message.
     */
    fun showProgressDialog(@StringRes message: Int) {
        showProgressDialog(getString(message))
    }

    /**
     * Show progress dialog with message.
     */
    fun showProgressDialog(message: String) {
        val currentDialog =
            childFragmentManager.findFragmentByTag(ProgressDialogFragment::class.java.name) as? ProgressDialogFragment
        currentDialog?.dismissAllowingStateLoss()
        ProgressDialogFragment.newInstance(message).show()
        childFragmentManager.executePendingTransactions() // to be dialog in fragment manager
    }

    /**
     * Hide progress dialog
     */
    fun hideProgressDialog() {
        val currentDialog =
            childFragmentManager.findFragmentByTag(ProgressDialogFragment::class.java.name) as? ProgressDialogFragment

        currentDialog?.apply {
            dismissAllowingStateLoss()
            childFragmentManager.executePendingTransactions() // to prevent race condition
        }
    }

    inline fun <reified T : DialogFragment> T.show(fragmentManager: FragmentManager = this@BaseFragment.childFragmentManager) {
        show(fragmentManager, T::class.java.name)
    }

    inline fun <reified T : DialogFragment> T.showForResult(requestCode: Int) {
        setTargetFragment(this@BaseFragment, requestCode)
        show(this@BaseFragment.requireFragmentManager(), T::class.java.name)
    }
}

/**
 * Shortcut for parsing arguments.
 */
inline fun <reified T> Fragment.argument(argumentName: String): ReadOnlyProperty<Fragment, T> {
    return object : ReadOnlyProperty<Fragment, T> {
        override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
            return arguments?.get(argumentName) as T
        }
    }
}

/**
 * Shortcut for parsing arguments with default value.
 */
inline fun <reified T> Fragment.argument(argumentName: String, defaultValue: T): ReadOnlyProperty<Fragment, T> {
    return object : ReadOnlyProperty<Fragment, T> {
        override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
            return (arguments?.get(argumentName) as? T?) ?: defaultValue
        }
    }
}