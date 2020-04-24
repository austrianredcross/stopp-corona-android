package at.roteskreuz.stopcorona.utils.view

import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText

/**
 * Sets the [text] in the [TextInputEditText].
 */
fun TextInputEditText.applyText(text: String) {
    setText(text, TextView.BufferType.EDITABLE)
}