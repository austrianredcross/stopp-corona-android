package at.roteskreuz.stopcorona.screens.diary.epoxy

import android.view.View
import android.widget.TextView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.entities.diary.DbDiaryEntry
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import at.roteskreuz.stopcorona.utils.format
import at.roteskreuz.stopcorona.utils.string
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import org.threeten.bp.LocalDate

@EpoxyModelClass(layout = R.layout.diary_item_epoxy_model)
abstract class DiaryItemModel(
    val onClick: (date: LocalDate) -> Unit
) : BaseEpoxyModel<DiaryItemModel.Holder>() {

    @EpoxyAttribute
    var date: LocalDate? = null

    @EpoxyAttribute
    var count: Int = 0

    override fun Holder.onBind() {
        txtDate.text = date?.format(context.getString(R.string.general_date_format))
        if (count > 0) {
            if(count == 1) {
                txtEntryCnt.text = "$count " + context.string(R.string.diary_overview_cell_hint_1)
            }else{
                txtEntryCnt.text = "$count " + context.string(R.string.diary_overview_cell_hint_2)
            }
            txtEntryCnt.visibility = View.VISIBLE
        }else{
            txtEntryCnt.text = ""
            txtEntryCnt.visibility = View.GONE
        }
        view.setOnClickListener {
            date?.let { date -> onClick(date) }
        }
    }

    class Holder : BaseEpoxyHolder() {
        val txtDate by bind<TextView>(R.id.txtDate)
        val txtEntryCnt by bind<TextView>(R.id.txtEntryCnt)
    }
}