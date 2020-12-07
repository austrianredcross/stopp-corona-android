package at.roteskreuz.stopcorona.screens.base.epoxy

import android.widget.ImageView
import androidx.annotation.DrawableRes
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import at.roteskreuz.stopcorona.skeleton.core.utils.visible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

@EpoxyModelClass(layout = R.layout.base_image_epoxy_model)
abstract class ImageModel : BaseEpoxyModel<ImageModel.Holder>() {

    @EpoxyAttribute
    @DrawableRes var imageRes: Int? = null

    @EpoxyAttribute
    var imageDesc: String? = null

    override fun Holder.onBind() {
        view.visible = imageRes?.let {
            imgContent.setImageResource(it)
            imgContent.contentDescription = imageDesc
            true
        } ?: false
    }

    class Holder : BaseEpoxyHolder() {
        val imgContent by bind<ImageView>(R.id.imgContent)
    }
}
