package at.roteskreuz.stopcorona.screens.dashboard.changelog

import android.content.Context
import at.roteskreuz.stopcorona.model.manager.Changelog
import at.roteskreuz.stopcorona.screens.base.epoxy.buttons.buttonType1
import at.roteskreuz.stopcorona.screens.base.epoxy.copyText
import at.roteskreuz.stopcorona.screens.base.epoxy.emptySpace
import at.roteskreuz.stopcorona.screens.base.epoxy.headlineH1
import at.roteskreuz.stopcorona.screens.base.epoxy.image
import at.roteskreuz.stopcorona.skeleton.core.utils.adapterProperty
import com.airbnb.epoxy.EpoxyController

class ChangelogController(
    private val context: Context,
    private val onCtaClick: () -> Unit
) : EpoxyController() {

    var changelog: Changelog? by adapterProperty(null as Changelog?)

    override fun buildModels() {
        changelog?.let { changelog ->
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

            copyText {
                id("changelog_description")
                text(changelog.description)
            }

            emptySpace(modelCountBuiltSoFar, 36)

            buttonType1(onCtaClick) {
                id("changelog_cta")
                text(context.getString(changelog.callToAction))
            }

            emptySpace(modelCountBuiltSoFar, 26)
        }
    }
}
