package at.roteskreuz.stopcorona.screens.infection_info.epoxy

import android.graphics.Typeface
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.StyleSpan
import android.widget.TextView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.repositories.CombinedWarningType
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import at.roteskreuz.stopcorona.utils.getBoldSpan
import at.roteskreuz.stopcorona.utils.getClickableBoldSpan
import at.roteskreuz.stopcorona.utils.startCallWithPhoneNumber
import com.airbnb.epoxy.EpoxyModelClass

/**
 * Model with yellow guide info and important phone numbers.
 */
@EpoxyModelClass(layout = R.layout.guide_info_yellow_epoxy_model)
abstract class GuideInfoYellowModel(
    private val onPhoneClick: (phoneNumber: String) -> Unit,
    private val quarantinedUntil: String?
) : BaseEpoxyModel<GuideInfoYellowModel.Holder>() {

    override fun Holder.onBind() {
        txtUrgentHeadline.contentDescription =
            context.getString(R.string.sickness_certificate_urgent_help_headline) + context.getString(
                R.string.accessibility_heading_2
            )
        txtConsultingTitle.contentDescription =
            context.getString(R.string.sickness_certificate_guidelines_consulting_description_title) + context.getString(
                R.string.accessibility_heading_2
            )


        val spannable = SpannableString(quarantinedUntil)
        spannable.setSpan(StyleSpan(Typeface.BOLD), 0, spannable.length, 0)

        txtDescription1.text = SpannableStringBuilder().apply {
            append(context.getString(R.string.sickness_certificate_guidelines_first))
            append(context.getBoldSpan(R.string.sickness_certificate_guidelines_first_2))
            append(spannable)
            append(" ")
            append(context.getString(R.string.sickness_certificate_guidelines_first_3))
            append(context.getBoldSpan(R.string.sickness_certificate_guidelines_first_4))
        }

        txtDescription4.text = SpannableStringBuilder().apply {
            append(context.getString(R.string.sickness_certificate_guidelines_fourth))
            append(context.getBoldSpan(R.string.sickness_certificate_guidelines_fourth_2))
            append(context.getString(R.string.sickness_certificate_guidelines_fourth_3))
            append(context.getClickableBoldSpan(R.string.sickness_certificate_guidelines_fourth_4,
                colored = true,
                underline = false,
                onClick = {
                    context.startCallWithPhoneNumber(context.getString(R.string.sickness_certificate_guidelines_fourth_4))
                }))
            append(context.getString(R.string.sickness_certificate_guidelines_fourth_5))
        }

        txtDescription4.movementMethod = LinkMovementMethod()

        txtConsultingFirstPhone.startPhoneCallOnClick()
        txtConsultingSecondPhone.startPhoneCallOnClick()
        txtConsultingThirdPhone.startPhoneCallOnClick()
        txtUrgentNumber1.startPhoneCallOnClick()
        txtUrgentNumber2.startPhoneCallOnClick()
    }

    private fun TextView.startPhoneCallOnClick() {
        setOnClickListener {
            onPhoneClick(text.toString())
        }
    }

    class Holder : BaseEpoxyHolder() {
        val txtDescription1 by bind<TextView>(R.id.txtDescription1)
        val txtDescription4 by bind<TextView>(R.id.txtDescription4)
        val txtConsultingFirstPhone by bind<TextView>(R.id.txtConsultingFirstPhone)
        val txtConsultingSecondPhone by bind<TextView>(R.id.txtConsultingSecondPhone)
        val txtConsultingThirdPhone by bind<TextView>(R.id.txtConsultingThirdPhone)
        val txtUrgentNumber1 by bind<TextView>(R.id.txtUrgentNumber1)
        val txtUrgentNumber2 by bind<TextView>(R.id.txtUrgentNumber2)
        val txtUrgentHeadline by bind<TextView>(R.id.txtUrgentHelpHeadline)
        val txtConsultingTitle by bind<TextView>(R.id.txtConsultingTitle)
    }
}