package at.roteskreuz.stopcorona.screens.questionnaire.epoxy

import at.roteskreuz.stopcorona.R
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelGroup

/**
 * @Author Justus Klawisch (jkl) on 2019-09-09
 */
@EpoxyModelClass
abstract class QuestionnaireRadioGroupModel(models: List<EpoxyModel<*>>) : EpoxyModelGroup(R.layout.questionnaire_radio_group_epoxy_model, models)
