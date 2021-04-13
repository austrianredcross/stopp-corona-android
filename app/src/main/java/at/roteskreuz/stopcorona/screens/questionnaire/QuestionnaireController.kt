package at.roteskreuz.stopcorona.screens.questionnaire

import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.view.Gravity
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.entities.configuration.DbQuestionnaireWithAnswers
import at.roteskreuz.stopcorona.model.entities.configuration.Decision
import at.roteskreuz.stopcorona.screens.base.epoxy.*
import at.roteskreuz.stopcorona.screens.questionnaire.epoxy.QuestionnaireAnswerModel_
import at.roteskreuz.stopcorona.screens.questionnaire.epoxy.QuestionnaireRadioGroupModel_
import at.roteskreuz.stopcorona.screens.questionnaire.epoxy.questionnairePage
import at.roteskreuz.stopcorona.screens.webView.WebViewWithAssetsResourcesFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.addTo
import at.roteskreuz.stopcorona.skeleton.core.utils.rawDimen
import at.roteskreuz.stopcorona.utils.getClickableBoldUrlSpan
import at.roteskreuz.stopcorona.utils.string
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.TypedEpoxyController

class QuestionnaireController(
    private val context: Context,
    private val onEnterPage: (pageNumber: Int) -> Unit,
    private val onAnswerSelected: (pageNumber: Int, decision: Decision) -> Unit
) : TypedEpoxyController<List<DbQuestionnaireWithAnswers>>() {

    override fun buildModels(questionsWithAnsers: List<DbQuestionnaireWithAnswers>?) {
        questionsWithAnsers?.forEachIndexed { questionIndex, questionWithAnswer ->
            val questionPageContent = mutableListOf<EpoxyModel<*>>()

            EmptySpaceModel_()
                .id(modelCountBuiltSoFar)
                .height(context.rawDimen(R.dimen.questionnaire_headline_top_margin).toInt())
                .addTo(questionPageContent)

            HeadlineH1Model_()
                .id("question_${questionIndex}_headline")
                .text(context.getString(R.string.questionnaire_headline_1, (questionIndex + 1)))
                .textColor(R.color.questionnaire_headline1)
                .textSize(context.rawDimen(R.dimen.questionnaire_headline))
                .marginHorizontal(0f)
                .addTo(questionPageContent)

            HeadlineH2Model_()
                .id("question_${questionIndex}")
                .title(questionWithAnswer.question.questionText)
                .textColor(R.color.questionnaire_headline2)
                .gravity(Gravity.START)
                .addTo(questionPageContent)

            EmptySpaceModel_()
                .id(modelCountBuiltSoFar)
                .height(40)
                .addTo(questionPageContent)

            val answerList = mutableListOf<EpoxyModel<*>>()

            questionWithAnswer.answers.forEachIndexed { answerIndex, answer ->
                val id = "answer_${questionIndex}_${answerIndex}"
                QuestionnaireAnswerModel_ { decision -> onAnswerSelected(questionIndex, decision) }
                    .id(id)
                    .hash(id.hashCode())
                    .answer(answer.text)
                    .decision(answer.decision)
                    .textSize(context.rawDimen(R.dimen.questionnaire_answer))
                    .addTo(answerList)

                if (answerIndex < questionWithAnswer.answers.size - 1) {
                    EmptySpaceModel_()
                        .id(modelCountBuiltSoFar)
                        .height(context.rawDimen(R.dimen.questionnaire_answer_space).toInt())
                        .addTo(answerList)
                }
            }

            if (answerList.isNotEmpty()) {
                QuestionnaireRadioGroupModel_(answerList)
                    .id("question_answer_group_$questionIndex")
                    .addTo(questionPageContent)
            }

            if (questionIndex == 1) {
                LinkModel_()
                    .id("question_source")
                    .text(R.string.self_testing_symptoms_source_text)
                    .link(context.getString(R.string.self_testing_symptoms_source_link))
                    .imageRes(R.drawable.ic_external_link)
                    .imageDesc(context.getString(R.string.start_menu_item_external_link))
                    .textColor(R.color.text_link)
                    .addTo(questionPageContent)

                EmptySpaceModel_()
                    .id(modelCountBuiltSoFar)
                    .height(16)
                    .addTo(questionPageContent)
            } else {
                EmptySpaceModel_()
                    .id(modelCountBuiltSoFar)
                    .height(32)
                    .addTo(questionPageContent)
            }

            questionnairePage(onEnterPage, questionPageContent) {
                id("question_group_$questionIndex")
                pageNumber(questionIndex)
            }
        }
    }
}
