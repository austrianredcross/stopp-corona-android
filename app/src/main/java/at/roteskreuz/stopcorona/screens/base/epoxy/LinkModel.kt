package at.roteskreuz.stopcorona.screens.base.epoxy

import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import at.roteskreuz.stopcorona.skeleton.core.utils.visible
import at.roteskreuz.stopcorona.utils.getClickableUrlSpan
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

@EpoxyModelClass(layout = R.layout.base_link_epoxy_model)
abstract class LinkModel : BaseEpoxyModel<LinkModel.Holder>() {

    @EpoxyAttribute
    @StringRes
    var text: Int? = null

    @EpoxyAttribute
    var link: String? = null

    @EpoxyAttribute
    @DrawableRes
    var imageRes: Int? = null

    @EpoxyAttribute
    var imageDesc: String? = null

    @EpoxyAttribute
    var gravity: Int = Gravity.START

    override fun Holder.onBind() {
        val builder = SpannableStringBuilder()
        builder.append(
            context.getClickableUrlSpan(
                text,
                url = link
            )
        )

        txtLink.text = builder
        txtLink.movementMethod = LinkMovementMethod()

        imgLink.visible = imageRes?.let {
            imgLink.setImageResource(it)
            imgLink.contentDescription = imageDesc
            true
        } ?: false
    }

    class Holder : BaseEpoxyHolder() {
        val txtLink by bind<TextView>(R.id.txtLink)
        val imgLink by bind<ImageView>(R.id.imgLink)
    }
}