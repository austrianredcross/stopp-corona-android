package at.roteskreuz.stopcorona.utils

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import androidx.annotation.StringRes
import at.roteskreuz.stopcorona.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

fun listenForTextChanges(
    textInputLayout: TextInputLayout,
    textInputEditText: TextInputEditText,
    saveToViewModel: (String?) -> Unit
): TextWatcher {
    val textWatcher = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            saveToViewModel(s.toString())
            textInputLayout.error = null
        }

        override fun afterTextChanged(s: Editable?) {
            // Do nothing.
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // Do nothing.
        }
    }
    textInputEditText.addTextChangedListener(textWatcher)
    return textWatcher
}

fun displayFieldInlineError(validationError: ValidationError?, textInputLayout: TextInputLayout, context: Context) {
    validationError?.let {
        textInputLayout.error = context.getString(it.error)
    } ?: run {
        textInputLayout.error = null
    }
}


/**
 * Describes the errors that can appear in the form.
 */
sealed class ValidationError(@StringRes val error: Int) {

    /**
     * A mandatory field is empty.
     */
    object FieldEmpty : ValidationError(R.string.certificate_personal_data_field_mandatory)

    /**
     * Invalid phone number.
     */
    object InvalidPhoneNumber : ValidationError(R.string.certificate_personal_data_phone_field_invalid)
}


fun validateNotEmpty(text: String?): ValidationError? {
    return if (text?.isNotEmpty() == true) {
        null
    } else {
        return ValidationError.FieldEmpty
    }
}

fun validatePhoneNumber(text: String): ValidationError? {
    val pattern = "^\\+[0-9]+".toRegex()

    return if (pattern.matches(text).not()) {
        ValidationError.InvalidPhoneNumber
    } else {
        null
    }
}