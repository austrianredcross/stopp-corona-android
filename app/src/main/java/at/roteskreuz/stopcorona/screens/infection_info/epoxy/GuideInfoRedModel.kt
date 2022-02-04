package at.roteskreuz.stopcorona.screens.infection_info.epoxy

import android.graphics.Typeface
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
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

        txtDescription3Phone.text = SpannableStringBuilder().apply {
            append(context.getBoldSpan(R.string.contact_sickness_guidelines_third_1, insertLeadingSpace = false))
            append(context.getClickableBoldSpan(R.string.contact_sickness_guidelines_third_2,
                colored = true,
                underline = false,
                insertLeadingSpace = false,
                onClick = {
                    context.startCallWithPhoneNumber(context.getString(R.string.contact_sickness_guidelines_third_2))
                }))
        }

        txtDescription4Phone.text = SpannableStringBuilder().apply {
            append(context.getClickableBoldSpan(R.string.contact_sickness_guidelines_fourth_1,
                colored = true,
                underline = false,
                insertLeadingSpace = false,
                onClick = {
                    context.startCallWithPhoneNumber(context.getString(R.string.contact_sickness_guidelines_fourth_1))
                }))
        }

        txtDescription5.text = SpannableStringBuilder().apply {
            append(context.getString(R.string.contact_sickness_guidelines_fifth))
            append(context.getBoldSpan(R.string.contact_sickness_guidelines_fifth_1))
            append(context.getString(R.string.contact_sickness_guidelines_fifth_2))
            append(context.getClickableBoldSpan(R.string.contact_sickness_guidelines_fifth_3,
                colored = true,
                underline = false,
                onClick = {
                    context.startCallWithPhoneNumber(context.getString(R.string.contact_sickness_guidelines_fifth_3))
                }))
            append(context.getString(R.string.contact_sickness_guidelines_fifth_4))
        }

        txtDescription3Phone.movementMethod = LinkMovementMethod()
        txtDescription4Phone.movementMethod = LinkMovementMethod()
        txtDescription5.movementMethod = LinkMovementMethod()

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
        val txtDescription3Phone by bind<TextView>(R.id.txtDescription3Phone)
        val txtDescription4Phone by bind<TextView>(R.id.txtDescription4Phone)
        val txtDescription5 by bind<TextView>(R.id.txtDescription5)
        val txtConsultingFirstPhone by bind<TextView>(R.id.txtConsultingFirstPhone)
        val txtConsultingSecondPhone by bind<TextView>(R.id.txtConsultingSecondPhone)
        val txtConsultingThirdPhone by bind<TextView>(R.id.txtConsultingThirdPhone)
        val txtUrgentNumber1 by bind<TextView>(R.id.txtUrgentNumber1)
        val txtUrgentNumber2 by bind<TextView>(R.id.txtUrgentNumber2)
        val txtUrgentHeadline by bind<TextView>(R.id.txtUrgentHelpHeadline)
        val txtConsultingTitle by bind<TextView>(R.id.txtConsultingTitle)
    }
}