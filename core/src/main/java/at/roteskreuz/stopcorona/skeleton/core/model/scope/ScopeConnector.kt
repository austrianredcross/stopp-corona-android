package at.roteskreuz.stopcorona.skeleton.core.model.scope

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.LifecycleOwner
import at.roteskreuz.stopcorona.skeleton.core.utils.onDestroyed
import org.koin.core.Koin
import org.koin.core.scope.Scope
import org.koin.standalone.KoinComponent
import org.koin.standalone.StandAloneContext.getKoin
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Helper class to keep [Scope] live when all attached lifecycle owners ([Fragment], [Activity]) are live.
 * Once the last lifecycle owner has destroyed, the scope is destroyed as well.
 *
 * Usage:
 *
 * If there are many lifecycle owners that needs to access to the one scope (i.e. some repository),
 * which is living only when lifecycle owners are live. Every lifecycle owner needs to call `connectToScope(scopeId)`
 * in his `onCreate` method. The scope is created automatically and is automatically closed
 * after all of lifecycle owners are destroyed.
 *
 * Second use case is to prevent accessing already created scope. Imagine a situation
 * you have a fragment A with list of items and fragment B which represent the detail of the item.
 * You open the detail from the list, go back and immediately open the detail fragment again.
 * Fragment B create a scope in his `onCreate` method. The problem could appear when there is
 * used fragment transition to change fragments (i.e. [FragmentTransaction.TRANSIT_FRAGMENT_FADE])
 * because the closing fragment B implicit calls `onDestroy` method AFTER the new fragment B
 * is created. It causes [ScopeAlreadyExistsException].
 * This class solves this situation, because fragment B which is using the one scope is registered
 * into a map and once the scope is used by the another instance of fragment B, the scope
 * is not recreating again. It is used already created scope. Than, the old fragment B is
 * destroyed and automatically removed from the map. In the map is only the new instance of fragment B.
 *
 * This class additionally solve the situation when the lifecycle owner ([Fragment], [Activity]) is in rotation and the scope
 * is recreating by default implementation of [Koin]. It this case this class prevent the scope
 * closing when detect some configuration changes before lifecycle owner is destroyed.
 */
class ScopeConnector : KoinComponent {

    /**
     * First param is scope id and the set are lifecycle owner using the scope at the same time.
     */
    private val registeredScopes = HashMap<String, HashSet<WeakReference<LifecycleOwner>>>()

    /**
     * Register the [lifecycleOwnerReference] to the scope [scopeId].
     * If not scope exists, the new one is created.
     * Once all the connected lifecycle owners has destroyed the scope is destroyed as well.
     */
    fun registerToScope(scopeId: String, lifecycleOwnerReference: WeakReference<LifecycleOwner>) {
        if (!registeredScopes.containsKey(scopeId)) {
            getKoin().createScope(scopeId)
            registeredScopes[scopeId] = HashSet<WeakReference<LifecycleOwner>>().apply { add(lifecycleOwnerReference) }
        } else {
            registeredScopes[scopeId]?.let { set ->
                set.add(lifecycleOwnerReference)
                registeredScopes[scopeId] = set
            } ?: Timber.e("scope $scopeId not found")
        }
        lifecycleOwnerReference.get()?.onDestroyed {
            registeredScopes[scopeId]?.let { set ->
                set.remove(lifecycleOwnerReference)
                if (set.isEmpty()) {
                    // handle the screen rotation, the scope is not closing
                    val isChangingConfigurations = when (val owner = lifecycleOwnerReference.get()) {
                        is Activity -> owner.isChangingConfigurations
                        is Fragment -> owner.activity?.isChangingConfigurations == true
                        else -> false
                    }

                    if (isChangingConfigurations) {
                        registeredScopes[scopeId] = set
                    } else {
                        registeredScopes.remove(scopeId)
                        getKoin().getScope(scopeId).close()
                    }
                } else {
                    registeredScopes[scopeId] = set
                }
            } ?: Timber.e("scope $scopeId not found")
        }
    }
}

/**
 * Helper function to connect the current lifecycle owner ([Fragment] or [Activity])
 * to a scope with id [scopeId].
 * If the scope has not been created yet the scope is than created as new.
 * Once all the connected lifecycle owners has destroyed the scope is destroyed as well.
 */
fun LifecycleOwner.connectToScope(scopeId: String) {
    val scopeConnector: ScopeConnector = getKoin().koinContext.get()
    scopeConnector.registerToScope(scopeId, WeakReference(this))
}