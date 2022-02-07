package at.roteskreuz.stopcorona.screens.sun_downer.epoxy

import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import at.roteskreuz.stopcorona.skeleton.core.utils.visible
import com.airbnb.epoxy.*

@EpoxyModelClass(layout = R.layout.sun_downer_page_epoxy_model)
abstract class SunDownerPageModel(
    private val onPageEnter: (pageNumber: Int) -> Unit = {},
    private val onClickNewsletterButton: (() -> Unit)?
) : BaseEpoxyModel<SunDownerPageModel.Holder>() {

    @EpoxyAttribute
    var pageNumber: Int = 0

    @EpoxyAttribute
    var title: String? = null

    @EpoxyAttribute
    var description: SpannableStringBuilder? = null

    @DrawableRes
    @EpoxyAttribute
    var heroImageRes: Int = R.drawable.ic_sun_downer

    @EpoxyAttribute
    var heroImageDesc: String? = null

    @EpoxyAttribute
    var heroImageVisible: Boolean = true

    @EpoxyAttribute
    var newsletterButtonVisible: Boolean = false

    @EpoxyAttribute
    var redCrossLogoVisible: Boolean = false

    override fun Holder.onBind() {
        txtTitle.text = title
        txtTitle.contentDescription = title + context.getString(R.string.accessibility_heading_1)
        txtDescription.text = description
        imgHero.setImageResource(heroImageRes)
        imgHero.contentDescription = heroImageDesc
        imgHero.visible = heroImageVisible

        btnNewsletter.visible = newsletterButtonVisible
        btnNewsletter.contentDescription = context.getString(R.string.sunDowner_newsletter_link_accessibility)
        redCrossLogo.visible = redCrossLogoVisible
        redCrossLogo.contentDescription = heroImageDesc

        onClickNewsletterButton?.let { onClickNewsletterButton ->
            btnNewsletter.setOnClickListener {
                onClickNewsletterButton()
            }
        }

        txtDescription.movementMethod = LinkMovementMethod()
    }

    class Holder : BaseEpoxyHolder() {
        val txtTitle by bind<TextView>(R.id.txtTitle)
        val txtDescription by bind<TextView>(R.id.txtDescription)
        val imgHero by bind<ImageView>(R.id.imgHero)
        val btnNewsletter by bind<Button>(R.id.btnNewsletter)
        val redCrossLogo by bind<ImageView>(R.id.redCrossLogo)
    }

    override fun onVisibilityStateChanged(visibilityState: Int, view: Holder) {
        if (visibilityState == VisibilityState.FOCUSED_VISIBLE) {
            onPageEnter(pageNumber)
        }
    }
}
