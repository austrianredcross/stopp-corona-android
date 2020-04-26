package at.roteskreuz.stopcorona.screens.base.epoxy

import android.widget.CheckBox
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass

/**
 * Epoxy model for a checkbox element.
 */
@EpoxyModelClass(layout = R.layout.base_checkbox_epoxy_model)
abstract class CheckboxModel(
    private val onCheckedChangedListener: (Boolean) -> Unit
) : BaseEpoxyModel<CheckboxModel.Holder>() {

    @EpoxyAttribute
    var label: String? = null

    @EpoxyAttribute
    var checked: Boolean = false

    /**
     * Text size of the label in sp.
     */
    @EpoxyAttribute
    var textSize: Int = 20

    @EpoxyAttribute
    var textStyle: Int = R.style.AppTheme_Copy

    override fun Holder.onBind() {
        checkbox.isChecked = checked
        checkboxLabel.text = label
        checkboxLabel.textSize = textSize.toFloat()
        checkboxLabel.setTextAppearance(textStyle)

        constraintLayoutCheckbox.setOnClickListener {
            checkbox.toggle()
        }
        checkbox.setOnCheckedChangeListener { _, checked ->
            onCheckedChangedListener(checked)
        }
    }

    override fun Holder.onUnbind() {
        checkbox.setOnCheckedChangeListener(null)
        constraintLayoutCheckbox.setOnClickListener(null)
    }

    class Holder : BaseEpoxyHolder() {
        val constraintLayoutCheckbox: ConstraintLayout by bind(R.id.constraintLayoutCheckbox)
        val checkbox: CheckBox by bind(R.id.checkbox)
        val checkboxLabel: TextView by bind(R.id.checkboxLabel)
    }
}