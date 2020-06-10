package at.roteskreuz.stopcorona.screens.dashboard.changelog

import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import at.roteskreuz.stopcorona.model.managers.Changelog
import at.roteskreuz.stopcorona.model.managers.SpanTextWrapper
import at.roteskreuz.stopcorona.screens.base.epoxy.buttons.buttonType1
import at.roteskreuz.stopcorona.screens.base.epoxy.copyText
import at.roteskreuz.stopcorona.screens.base.epoxy.emptySpace
import at.roteskreuz.stopcorona.screens.base.epoxy.headlineH1
import at.roteskreuz.stopcorona.screens.base.epoxy.image
import at.roteskreuz.stopcorona.utils.getBoldSpan
import com.airbnb.epoxy.TypedEpoxyController

class ChangelogController(
    private val context: Context,
    private val onCtaClick: () -> Unit
) : TypedEpoxyController<Changelog>() {

    override fun buildModels(changelog: Changelog) {
        emptySpace(modelCountBuiltSoFar, 32)

        headlineH1 {
            id("changelog_headline")
            text(context.getString(changelog.title))
        }

        emptySpace(modelCountBuiltSoFar, 24)

        image {
            id("changelog_image")
            imageRes(changelog.image)
        }

        emptySpace(modelCountBuiltSoFar, 24)

        val descriptionBuilder = SpannableStringBuilder().apply {
            changelog.description.forEach { spanTextWrapper ->
                append(
                    when (spanTextWrapper) {
                        is SpanTextWrapper.NoStyle -> context.getString(spanTextWrapper.textRes)
                        is SpanTextWrapper.Styled ->
                            context.getBoldSpan(
                                spanTextWrapper.textRes,
                                spanTextWrapper.colored,
                                spanTextWrapper.insertLeadingSpace,
                                spanTextWrapper.insertTrailingSpace
                            )
                    }
                )
            }
        }

        copyText {
            id("changelog_description")
            text(SpannableString(descriptionBuilder))
        }

        emptySpace(modelCountBuiltSoFar, 36)

        buttonType1(onCtaClick) {
            id("changelog_cta")
            text(context.getString(changelog.callToAction))
        }

        emptySpace(modelCountBuiltSoFar, 26)
    }
}
