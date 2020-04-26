package at.roteskreuz.stopcorona.screens.questionnaire.epoxy

import at.roteskreuz.stopcorona.R
import com.airbnb.epoxy.*

/**
 * @Author Justus Klawisch (jkl) on 2019-09-09
 */
@EpoxyModelClass
abstract class QuestionnairePageModel(
    private val onPageEnter: (pageNumber: Int) -> Unit,
    models: List<EpoxyModel<*>>
) : EpoxyModelGroup(R.layout.questionnaire_page_epoxy_model, models) {

    @EpoxyAttribute
    var pageNumber: Int = 0

    override fun onVisibilityStateChanged(visibilityState: Int, view: ModelGroupHolder) {
        if (visibilityState == VisibilityState.FOCUSED_VISIBLE) {
            onPageEnter(pageNumber)
        }
    }
}
