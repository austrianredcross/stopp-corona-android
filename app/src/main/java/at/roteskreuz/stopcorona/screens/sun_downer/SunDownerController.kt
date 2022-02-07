package at.roteskreuz.stopcorona.screens.sun_downer

import android.content.Context
import android.text.SpannableStringBuilder
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.screens.sun_downer.epoxy.sunDownerPage
import at.roteskreuz.stopcorona.utils.formatDayAndMonthAndYear
import at.roteskreuz.stopcorona.utils.getClickableBoldSpan
import com.airbnb.epoxy.EpoxyController

class SunDownerController(
    private val context: Context,
    private val onEnterPage: (pageNumber: Int) -> Unit,
    private val onClickNewsletterButton: () -> Unit
) : EpoxyController() {

    override fun buildModels() {

        sunDownerPage(onEnterPage, null) {
            id("sun_downer_page_1")
            pageNumber(0)
            title(context.getString(R.string.sunDowner_first_page_title))
            heroImageVisible(true)
            heroImageRes(R.drawable.ic_sun_downer)
            heroImageDesc(context.getString(R.string.sunDowner1_img))

            val builder = SpannableStringBuilder()
            builder.append(context.getString(R.string.sunDowner_first_page_content))
            description(builder)
        }

        sunDownerPage(onEnterPage, null) {
            id("sun_downer_page_2")
            pageNumber(1)

            val downDate = Constants.Behavior.SUN_DOWNER_DATE.formatDayAndMonthAndYear(context)
            title(context.getString(R.string.sunDowner_second_page_title, downDate))
            heroImageVisible(false)

            val builder = SpannableStringBuilder()
            builder.append(context.getString(R.string.sunDowner_second_page_content, downDate, downDate))

            description(builder)
        }

        sunDownerPage(onEnterPage, onClickNewsletterButton) {
            id("sun_downer_page_3")
            pageNumber(2)

            title(context.getString(R.string.sunDowner_third_page_title))
            heroImageVisible(false)

            newsletterButtonVisible(true)
            redCrossLogoVisible(true)
            heroImageDesc(context.getString(R.string.sunDowner2_img))

            val builder = SpannableStringBuilder()
            builder.append(context.getString(R.string.sunDowner_third_page_content))

            description(builder)
        }
    }

}