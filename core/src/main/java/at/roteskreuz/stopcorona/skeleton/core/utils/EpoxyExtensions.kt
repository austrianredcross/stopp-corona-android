package at.roteskreuz.stopcorona.skeleton.core.utils

import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyModel
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Extensions for better life with Epoxy.
 **/

/**
 * Returns a property delegate for a read/write property that calls
 * [EpoxyController#requestModelBuild] method when property is changed
 * @param initialValue the initial value of the property.
 */
fun <T> EpoxyController.adapterProperty(initialValue: T, doAfterChange: ((T) -> Unit)? = null): ReadWriteProperty<Any?, T> =
    object : ObservableProperty<T>(initialValue) {
        override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
            doAfterChange?.invoke(newValue)
            requestModelBuild()
        }
    }

/**
 * Returns a property delegate for a read/write property that calls
 * [EpoxyController#requestModelBuild] method when property is changed
 * @param initialValue the initial value of the property.
 */
fun <T> EpoxyController.adapterProperty(initialValue: () -> T, doAfterChange: ((T) -> Unit)? = null): ReadWriteProperty<Any?, T> =
    object : ObservableProperty<T>(initialValue()) {
        override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
            doAfterChange?.invoke(newValue)
            requestModelBuild()
        }
    }

/**
 * Extension function to EpoxyModel which adds the created model to a given [list]
 */
fun EpoxyModel<*>.addTo(list: MutableList<EpoxyModel<*>>) {
    list.add(this)
}
