package at.roteskreuz.stopcorona.screens.menu.epoxy

import android.widget.TextView
import at.roteskreuz.stopcorona.BuildConfig
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import at.roteskreuz.stopcorona.utils.string
import com.airbnb.epoxy.EpoxyModelClass

/**
 * Model as menu item with app version.
 */
@EpoxyModelClass(layout = R.layout.menu_item_version_epoxy_model)
abstract class MenuItemVersionModel(
    private val onClick: () -> Unit
) : BaseEpoxyModel<MenuItemVersionModel.Holder>() {

    override fun Holder.onBind() {
        txtVersion.text = string(R.string.start_menu_item_version, BuildConfig.VERSION_NAME)
        view.setOnClickListener {
            onClick()
        }
    }

    override fun Holder.onUnbind() {
        view.setOnClickListener(null)
    }

    class Holder : BaseEpoxyHolder() {
        val txtVersion by bind<TextView>(R.id.txtVersion)
    }
}