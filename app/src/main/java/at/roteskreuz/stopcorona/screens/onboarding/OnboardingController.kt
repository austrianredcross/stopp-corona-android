package at.roteskreuz.stopcorona.screens.onboarding

import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.onboarding.epoxy.onboardingDataPrivacy
import at.roteskreuz.stopcorona.screens.onboarding.epoxy.onboardingPage
import at.roteskreuz.stopcorona.screens.webView.WebViewWithAssetsResourcesFragment
import at.roteskreuz.stopcorona.utils.getBoldSpan
import at.roteskreuz.stopcorona.utils.getClickableSpan
import com.airbnb.epoxy.EpoxyController

/**
 * Describes the content of the onboarding carousel.
 */
class OnboardingController(
    private val context: Context,
    private val dataPrivacyAccepted: Boolean,
    private val onEnterLastPage: (pageNumber: Int) -> Unit,
    private val onDataPrivacyCheckBoxChanged: (checked: Boolean) -> Unit
) : EpoxyController() {

    override fun buildModels() {

        onboardingPage(onEnterLastPage) {
            id("onboarding_page_1")
            pageNumber(0)
            title(context.getString(R.string.onboarding_headline_1))
            heroImageRes(R.drawable.onboarding_hero_1)

            val builder = SpannableStringBuilder()
            builder.append(context.getString(R.string.onboarding_copy_1_1))
            builder.append(context.getBoldSpan(R.string.onboarding_copy_1_2, colored = true))
            builder.append(context.getString(R.string.onboarding_copy_1_3))
            builder.append(context.getBoldSpan(R.string.onboarding_copy_1_4, colored = true))
            builder.append(context.getString(R.string.onboarding_copy_1_5))
            description(builder)
        }

        onboardingPage(onEnterLastPage) {
            id("onboarding_page_2")
            pageNumber(1)
            title(context.getString(R.string.onboarding_headline_2))
            heroImageRes(R.drawable.onboarding_hero_2)

            val builder = SpannableStringBuilder()
            builder.append(context.getString(R.string.onboarding_copy_2_1))
            builder.append(context.getBoldSpan(R.string.onboarding_copy_2_2, colored = true))
            builder.append(context.getString(R.string.onboarding_copy_2_3))
            description(builder)
        }

        onboardingPage(onEnterLastPage) {
            id("onboarding_page_3")
            pageNumber(2)
            title(context.getString(R.string.onboarding_headline_3))
            heroImageRes(R.drawable.onboarding_hero_3)

            val builder = SpannableStringBuilder()
            builder.append(context.getString(R.string.onboarding_copy_3_1))
            builder.append(context.getBoldSpan(R.string.onboarding_copy_3_2, colored = true))
            builder.append(context.getString(R.string.onboarding_copy_3_3))
            description(builder)
        }

        onboardingPage(onEnterLastPage) {
            id("onboarding_page_4")
            pageNumber(3)
            title(context.getString(R.string.onboarding_headline_4))
            heroImageRes(R.drawable.onboarding_hero_4)

            val builder = SpannableStringBuilder()
            builder.append(context.getString(R.string.onboarding_copy_4_1))
            builder.append(context.getBoldSpan(R.string.onboarding_copy_4_2, colored = true))
            builder.append(context.getString(R.string.onboarding_copy_4_3))
            description(builder)
        }

        onboardingPage(onEnterLastPage) {
            id("onboarding_page_5")
            pageNumber(4)
            title(context.getString(R.string.onboarding_headline_5))
            heroImageVisible(false)

            val builder = SpannableStringBuilder()
            builder.append(context.getString(R.string.onboarding_copy_5_1))
            builder.append(
                context.getClickableSpan(
                    R.string.onboarding_copy_5_2,
                    url = WebViewWithAssetsResourcesFragment.DEEP_LINK_TERMS_OF_USE
                )
            )
            builder.append(context.getString(R.string.onboarding_copy_5_3))
            description(builder)
        }

        if (dataPrivacyAccepted.not()) {
            onboardingDataPrivacy(onEnterLastPage, onDataPrivacyCheckBoxChanged) {
                id("onboarding_page_6")
                pageNumber(5)
                title(context.getString(R.string.onboarding_headline_6))
                checkBoxDescription(context.getString(R.string.onboarding_dataprivacy_approval))
                firstDescription(SpannableString(context.getString(R.string.onboarding_dataprivacy_description_1)))

                val builder = SpannableStringBuilder()
                builder.append(context.getString(R.string.onboarding_dataprivacy_description_2_1))
                builder.append(
                    context.getClickableSpan(
                        R.string.onboarding_dataprivacy_description_2_2,
                        url = WebViewWithAssetsResourcesFragment.DEEP_LINK_PRIVACY
                    )
                )
                secondDescription(SpannableString.valueOf(builder))
            }
        }
    }
}