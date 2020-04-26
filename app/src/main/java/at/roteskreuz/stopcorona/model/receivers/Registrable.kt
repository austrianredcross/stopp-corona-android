package at.roteskreuz.stopcorona.model.receivers

import android.content.Context

/**
 * Interface with ability to be be registered to context.
 */
interface Registrable {

    fun register(context: Context)

    fun unregister(context: Context)
}