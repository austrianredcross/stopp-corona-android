package at.roteskreuz.stopcorona.skeleton.core.screens.base.view

import com.airbnb.epoxy.EpoxyModelWithHolder

/**
 * Base epoxy model with view holder.
 */
abstract class BaseEpoxyModel<ViewHolder : BaseEpoxyHolder> : EpoxyModelWithHolder<ViewHolder>() {

    abstract fun ViewHolder.onBind()

    open fun ViewHolder.onUnbind() {
    }

    final override fun bind(holder: ViewHolder) {
        super.bind(holder)
        holder.onBind()
    }

    override fun unbind(holder: ViewHolder) {
        super.unbind(holder)
        holder.onUnbind()
    }
}