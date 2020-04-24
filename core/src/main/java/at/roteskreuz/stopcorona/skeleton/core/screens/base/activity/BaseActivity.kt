package at.roteskreuz.stopcorona.skeleton.core.screens.base.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import at.roteskreuz.stopcorona.skeleton.core.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Base of all activities that contains logic for operating with fragments.
 *
 * @param layout layout inflated for this activity.
 */
open class BaseActivity(@LayoutRes private val layout: Int = R.layout.framelayout) : AppCompatActivity() {

    companion object {
        const val ARGUMENT_FRAGMENT_NAME = "fragment_name"
        const val ARGUMENT_FRAGMENT_ARGUMENTS = "fragment_arguments"
    }

    /**
     * Reference to the [ViewGroup] id which the fragment is inflated in.
     */
    @IdRes
    protected open val contentViewId: Int = R.id.fragment_container

    /**
     * Get currently displayed fragment.
     */
    val currentFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(contentViewId)

    /**
     * Returns the name of the fragment to be instantiated.
     *
     * Note: If you will inherit from FragmentActivity and do not provide fragment Name via intent or
     * with overriding this attribute, exception will be thrown
     */
    open val fragmentName: String?
        get() = intent?.getStringExtra(ARGUMENT_FRAGMENT_NAME)

    protected var disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)

        val fragmentName = fragmentName ?: throw IllegalStateException("Fragment name must be provided")
        val args = intent?.getBundleExtra(ARGUMENT_FRAGMENT_ARGUMENTS)

        var fragment: Fragment? = supportFragmentManager.findFragmentByTag(fragmentName)
        if (fragment == null && savedInstanceState == null) {
            fragment = supportFragmentManager.fragmentFactory.instantiate(classLoader, fragmentName)
            if (args != null) {
                fragment.arguments = args
            }
            supportFragmentManager.beginTransaction()
                .add(contentViewId, fragment, fragmentName)
                .commit()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        currentFragment?.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * Override this method to update parent activity intent.
     */
    protected open fun getInternalParentActivityIntent(): Intent? = NavUtils.getParentActivityIntent(this)

    /**
     * When activity is closing (by back pressing) it will go to the parent activity defined in AndroidManifest.
     * If the parent activity is in backStack it is restored, otherwise it is recreated.
     */
    open val navigateUpOnBackPress: Boolean = false

    /**
     * Navigate to the parent activity or recreate it if needed.
     */
    open fun navigateUp() {
        val upIntent: Intent? = getInternalParentActivityIntent()

        when {
            upIntent == null -> Timber.e(IllegalStateException("No Parent Activity Intent"))
            NavUtils.shouldUpRecreateTask(this, upIntent) || isTaskRoot -> {
                TaskStackBuilder.create(this)
                    .addNextIntentWithParentStack(upIntent)
                    .startActivities()
            }
            else -> {
                NavUtils.navigateUpTo(this, upIntent)
            }
        }
    }

    /**
     * Replace fragment in content view with [fragment] provided in property
     * @param fragment fragment for container to be replaced with
     * @param name of the transaction, null if not needed
     * @param addToBackStack if fragment is added to backstack or not
     * @param transition transition animation between new and old fragment
     * @param transactionTransformer transform the fragment transaction (i.e. you can add shared elements)
     */
    @JvmOverloads
    fun replaceFragment(
        fragment: Fragment,
        name: String = fragment.javaClass.name,
        addToBackStack: Boolean = true,
        transition: Int = FragmentTransaction.TRANSIT_FRAGMENT_FADE,
        transactionTransformer: (FragmentTransaction.() -> FragmentTransaction)? = null
    ) {
        val transaction = supportFragmentManager.beginTransaction()
            .apply {
                transactionTransformer?.invoke(this)
            }
            .replace(contentViewId, fragment, name)
        if (addToBackStack) {
            transaction.addToBackStack(name)
        }
        transaction.setTransition(transition).commit()
    }

    override fun onBackPressed() {
        // back press is not working if fragment handle this callback on its own
        if (currentFragment == null || !(currentFragment as BaseFragment).overrideOnBackPressed()) {
            if (navigateUpOnBackPress && supportFragmentManager.backStackEntryCount == 0) {
                navigateUp()
            } else super.onBackPressed()
        }
    }

    override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }
}

/**
 * Extension to Activity that starts activity that is child of FragmentActivity and set activity properties
 */
inline fun <reified T : BaseActivity> Activity.startFragmentActivity(
    fragmentName: String? = null,
    fragmentArgs: Bundle? = null,
    activityBundle: Bundle? = null,
    singleTop: Boolean = false,
    clearTask: Boolean = false,
    options: Bundle? = null
) {
    startActivity(getFragmentActivityIntent<T>(
        this,
        fragmentName, fragmentArgs, activityBundle)
        .apply { if (singleTop) this.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) }
        .apply { if (clearTask) this.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK) },
        options
    )
}

/**
 * Extension to Fragment that starts activity that is child of FragmentActivity and set activity properties
 */
inline fun <reified T : BaseActivity> Fragment.startFragmentActivity(
    fragmentName: String? = null,
    fragmentArgs: Bundle? = null,
    activityBundle: Bundle? = null,
    singleTop: Boolean = false,
    clearTask: Boolean = false,
    options: Bundle? = null
) {
    startActivity(getFragmentActivityIntent<T>(
        activity, fragmentName, fragmentArgs, activityBundle)
        .apply { if (singleTop) this.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) }
        .apply { if (clearTask) this.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK) },
        options
    )
}

/**
 * Extension to Activity that starts activity  that is child of FragmentActivity and set activity properties and expects result
 */
inline fun <reified T : BaseActivity> Activity.startFragmentActivityForResult(
    fragmentName: String? = null,
    fragmentArgs: Bundle? = null,
    activityBundle: Bundle? = null,
    requestCode: Int
) {
    startActivityForResult(
        getFragmentActivityIntent<T>(
            this,
            fragmentName,
            fragmentArgs,
            activityBundle
        ), requestCode
    )
}

/**
 * Extension to Context that starts activity that is child of FragmentActivity and set activity properties
 */
inline fun <reified T : BaseActivity> Context.startFragmentActivity(
    fragmentName: String? = null,
    fragmentArgs: Bundle? = null,
    activityBundle: Bundle? = null,
    singleTop: Boolean = false,
    clearTask: Boolean = false,
    options: Bundle? = null
) {
    startActivity(getFragmentActivityIntent<T>(
        this, fragmentName, fragmentArgs, activityBundle)
        .apply { if (singleTop) this.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) }
        .apply { if (clearTask) this.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK) },
        options
    )
}

/**
 * Extension to Fragment that starts activity  that is child of FragmentActivity and set activity properties and expects result
 */
inline fun <reified T : BaseActivity> Fragment.startFragmentActivityForResult(
    fragmentName: String? = null,
    fragmentArgs: Bundle? = null,
    activityBundle: Bundle? = null,
    requestCode: Int
) {
    startActivityForResult(
        getFragmentActivityIntent<T>(
            activity,
            fragmentName,
            fragmentArgs,
            activityBundle
        ), requestCode
    )
}

/**
 * Get intent with properties for starting of FragmentActivity
 */
inline fun <reified T : BaseActivity> getFragmentActivityIntent(
    ctx: Context?,
    fragmentName: String? = null,
    fragmentArgs: Bundle? = null,
    activityBundle: Bundle? = null
): Intent {
    val intent = Intent(ctx, T::class.java)
    intent.putExtra(BaseActivity.ARGUMENT_FRAGMENT_NAME, fragmentName)
    intent.putExtra(BaseActivity.ARGUMENT_FRAGMENT_ARGUMENTS, fragmentArgs)
    if (activityBundle != null) {
        intent.putExtras(activityBundle)
    }
    return intent
}

/**
 * Shortcut for parsing arguments.
 */
inline fun <reified T> Activity.argument(argumentName: String): ReadOnlyProperty<Activity, T> {
    return object : ReadOnlyProperty<Activity, T> {
        override fun getValue(thisRef: Activity, property: KProperty<*>): T {
            return intent?.extras?.get(argumentName) as T
        }
    }
}

/**
 * Shortcut for parsing arguments with default value.
 */
inline fun <reified T> Activity.argument(argumentName: String, defaultValue: T): ReadOnlyProperty<Activity, T> {
    return object : ReadOnlyProperty<Activity, T> {
        override fun getValue(thisRef: Activity, property: KProperty<*>): T {
            return intent?.extras?.get(argumentName) as? T? ?: defaultValue
        }
    }
}