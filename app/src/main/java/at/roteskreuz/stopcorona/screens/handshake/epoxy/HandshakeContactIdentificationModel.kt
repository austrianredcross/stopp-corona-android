package at.roteskreuz.stopcorona.screens.handshake.epoxy

import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import at.roteskreuz.stopcorona.skeleton.core.utils.visible
import at.roteskreuz.stopcorona.utils.backgroundColor
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

@EpoxyModelClass(layout = R.layout.handshake_contact_identification_epoxy_model)
abstract class HandshakeContactIdentificationModel(
    private val onCheckedChanged: (Boolean) -> Unit
) : BaseEpoxyModel<HandshakeContactIdentificationModel.Holder>() {

    @EpoxyAttribute
    var contactIdentification: String? = null

    @EpoxyAttribute
    var contactSaved: Boolean = false

    @EpoxyAttribute
    var backgroundColor: Int = R.color.white

    @EpoxyAttribute
    var selected: Boolean = false

    override fun Holder.onBind() {
        txtIdentification.text = contactIdentification
        view.backgroundColor(backgroundColor)

        checkbox.setOnCheckedChangeListener(null)
        checkbox.isChecked = selected
        checkbox.setOnCheckedChangeListener { _, isChecked -> onCheckedChanged(isChecked) }

        with(contactSaved) {
            txtContactSaved.visible = this
            imgContactSaved.visible = this

            checkbox.isEnabled = this.not()

            if (this) {
                view.setOnClickListener(null)
            } else {
                view.setOnClickListener { checkbox.isChecked = checkbox.isChecked.not() }
            }
        }
    }

    override fun Holder.onUnbind() {
        checkbox.setOnCheckedChangeListener(null)
        view.setOnClickListener(null)
    }

    class Holder : BaseEpoxyHolder() {
        val checkbox by bind<CheckBox>(R.id.checkbox)
        val txtIdentification by bind<TextView>(R.id.txtIdentification)
        val txtContactSaved by bind<TextView>(R.id.txtContactSaved)
        val imgContactSaved by bind<AppCompatImageView>(R.id.imgContactSaved)
    }
}
