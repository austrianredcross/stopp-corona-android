package at.roteskreuz.stopcorona.screens.dashboard.changelog

import android.content.Context
import at.roteskreuz.stopcorona.model.manager.Changelog
import at.roteskreuz.stopcorona.skeleton.core.utils.adapterProperty
import com.airbnb.epoxy.EpoxyController

class ChangelogController(
    private val context: Context
) : EpoxyController() {

    var changelog: Changelog? by adapterProperty(null as Changelog?)

    override fun buildModels() {
        TODO("Not yet implemented")
    }
}
