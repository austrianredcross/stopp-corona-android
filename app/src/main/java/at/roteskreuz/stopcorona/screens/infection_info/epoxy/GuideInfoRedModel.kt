package at.roteskreuz.stopcorona.screens.infection_info.epoxy

import android.graphics.Typeface
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.widget.TextView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import at.roteskreuz.stopcorona.utils.getBoldSpan
import at.roteskreuz.stopcorona.utils.getClickableBoldSpan
import at.roteskreuz.stopcorona.utils.startCallWithPhoneNumber
import com.airbnb.epoxy.EpoxyModelClass

/**
 * Model with red guide info and important phone numbers.
 */
@EpoxyModelClass(layout = R.layout.guide_info_red_epoxy_model)
abstract class GuideInfoRedModel(
    private val onPhoneClick: (phoneNumber: String) -> Unit,
    private val quarantinedUntil: String?,
    private val lastContactDate: String?
) : BaseEpoxyModel<GuideInfoRedModel.Holder>() {

    override fun Holder.onBind() {
        txtUrgentHeadline.contentDescription = context.getString(R.string.sickness_certificate_urgent_help_headline) + context.getString(R.string.accessibility_heading_2)
        txtConsultingTitle.contentDescription = context.getString(R.string.sickness_certificate_guidelines_consulting_description_title) + context.getString(R.string.accessibility_heading_2)

        val quarantinedSpannable = SpannableString(quarantinedUntil)
        quarantinedSpannable.setSpan(StyleSpan(Typeface.BOLD), 0, quarantinedSpannable.length, 0)
        val lastContactSpannable = SpannableString(lastContactDate)
        lastContactSpannable.setSpan(StyleSpan(Typeface.BOLD), 0, lastContactSpannable.length, 0)

        txtDescription1.text = SpannableStringBuilder().apply {
            append(context.getBoldSpan(R.string.contact_sickness_guidelines_first, false, false))
            append(quarantinedSpannable)
            append(context.getBoldSpan(R.string.contact_sickness_guidelines_first_2))
            append(context.getString(R.string.contact_sickness_guidelines_first_3))
        }

        txtDescription2.text = SpannableStringBuilder().apply {
            append(context.getString(R.string.contact_sickness_guidelines_second))
            append(context.getBoldSpan(R.string.contact_sickness_guidelines_second_2))
            append(context.getClickableBoldSpan(R.string.contact_sickness_guidelines_second_3,
                colored = true,
                underline = false,
                onClick = {
                    context.startCallWithPhoneNumber(context.getString(R.string.contact_sickness_guidelines_second_3))
                }))
            append(context.getString(R.string.contact_sickness_guidelines_second_4))
            append(" ")
            append(lastContactSpannable)
            append(" ")
            append(context.getString(R.string.contact_sickness_guidelines_second_5))
            append(context.getBoldSpan(R.string.contact_sickness_guidelines_second_6))
            append(context.getString(R.string.contact_sickness_guidelines_second_7))
            append(context.getBoldSpan(R.string.contact_sickness_guidelines_second_8))
            append(").")
        }

        txtDescription6Phone.text  = SpannableStringBuilder().apply {
            append(context.getClickableBoldSpan(R.string.contact_sickness_guidelines_sixth_2,
                colored = true,
                underline = false,
                insertLeadingSpace = false,
                onClick = {
                    context.startCallWithPhoneNumber(context.getString(R.string.contact_sickness_guidelines_sixth_2))
                }))
        }

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
        val txtDescription2 by bind<TextView>(R.id.txtDescription2)
        val txtDescription6Phone by bind<TextView>(R.id.txtDescription6Phone)
        val txtConsultingFirstPhone by bind<TextView>(R.id.txtConsultingFirstPhone)
        val txtConsultingSecondPhone by bind<TextView>(R.id.txtConsultingSecondPhone)
        val txtConsultingThirdPhone by bind<TextView>(R.id.txtConsultingThirdPhone)
        val txtUrgentNumber1 by bind<TextView>(R.id.txtUrgentNumber1)
        val txtUrgentNumber2 by bind<TextView>(R.id.txtUrgentNumber2)
        val txtUrgentHeadline by bind<TextView>(R.id.txtUrgentHelpHeadline)
        val txtConsultingTitle by bind<TextView>(R.id.txtConsultingTitle)
    }
}