package at.roteskreuz.stopcorona.screens.diary.epoxy

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.entities.diary.DbDiaryEntry
import at.roteskreuz.stopcorona.model.entities.diary.DiaryEntryType
import at.roteskreuz.stopcorona.model.repositories.ContactEntry
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import at.roteskreuz.stopcorona.utils.format
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import org.threeten.bp.LocalDate

@EpoxyModelClass(layout = R.layout.diary_day_item_epoxy_model)
abstract class DiaryDayItemModel(
    val onClick: (id: Long?) -> Unit
) : BaseEpoxyModel<DiaryDayItemModel.Holder>() {

    @EpoxyAttribute
    var entry: DbDiaryEntry? = null

    @EpoxyAttribute
    var description: String? = null

    @EpoxyAttribute
    var details: String? = null

    @EpoxyAttribute
    var time: String? = null

    override fun Holder.onBind() {
        txtDescription.text = description
        txtDetails.text = details

        details?.let {
            txtDetails.visibility = View.VISIBLE
            txtDetails.text = details
        } ?: run {
            txtDetails.visibility = View.GONE
        }

        time?.let {
            txtTime.visibility = View.VISIBLE
            txtTime.text = time
        } ?: run {
            txtTime.visibility = View.GONE
        }

        when (entry?.type) {
            DiaryEntryType.PERSON -> imgIcon.setImageResource(R.drawable.ic_contact_person)
            DiaryEntryType.LOCATION -> imgIcon.setImageResource(R.drawable.ic_contact_location)
            DiaryEntryType.PUBLIC_TRANSPORT -> imgIcon.setImageResource(R.drawable.ic_contact_public_transport)
            DiaryEntryType.EVENT -> imgIcon.setImageResource(R.drawable.ic_contact_event)
        }

        btnClose.setOnClickListener {
            onClick(entry?.id)
        }
    }

    class Holder : BaseEpoxyHolder() {
        val txtDescription by bind<TextView>(R.id.txtDescription)
        val imgIcon by bind<ImageView>(R.id.imgIcon)
        val txtDetails by bind<TextView>(R.id.txtDetails)
        val txtTime by bind<TextView>(R.id.txtTime)
        val btnClose by bind<ImageView>(R.id.btnClose)
    }
}