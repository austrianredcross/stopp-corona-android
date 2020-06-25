package at.roteskreuz.stopcorona.screens.questionnaire

import android.content.Context
import android.view.Gravity
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.entities.configuration.DbQuestionnaireWithAnswers
import at.roteskreuz.stopcorona.model.entities.configuration.Decision
import at.roteskreuz.stopcorona.screens.base.epoxy.EmptySpaceModel_
import at.roteskreuz.stopcorona.screens.base.epoxy.HeadlineH1Model_
import at.roteskreuz.stopcorona.screens.questionnaire.epoxy.QuestionnaireAnswerModel_
import at.roteskreuz.stopcorona.screens.questionnaire.epoxy.QuestionnaireRadioGroupModel_
import at.roteskreuz.stopcorona.screens.questionnaire.epoxy.questionnairePage
import at.roteskreuz.stopcorona.skeleton.core.utils.addTo
import at.roteskreuz.stopcorona.skeleton.core.utils.rawDimen
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
                .textColor(R.color.black)
                .textSize(context.rawDimen(R.dimen.questionnaire_headline))
                .marginHorizontal(0f)
                .addTo(questionPageContent)

            HeadlineH1Model_()
                .id("question_${questionIndex}")
                .text(questionWithAnswer.question.questionText)
                .textSize(context.rawDimen(R.dimen.questionnaire_question_text_size))
                .gravity(Gravity.START)
                .marginHorizontal(0f)
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

            if(answerList.isNotEmpty()) {
                QuestionnaireRadioGroupModel_(answerList)
                    .id("question_answer_group_$questionIndex")
                    .addTo(questionPageContent)
            }

            EmptySpaceModel_()
                .id(modelCountBuiltSoFar)
                .height(32)
                .addTo(questionPageContent)

            questionnairePage(onEnterPage, questionPageContent) {
                id("question_group_$questionIndex")
                pageNumber(questionIndex)
            }
        }
    }
}
