package at.roteskreuz.stopcorona.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.util.TypedValue
import android.view.View
import androidx.annotation.*
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.Constants.Misc.EMPTY_STRING
import at.roteskreuz.stopcorona.constants.Constants.Misc.SPACE
import at.roteskreuz.stopcorona.model.entities.configuration.ConfigurationLanguage
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import timber.log.Timber

/**
 * Extensions for context class.
 */

@ColorInt
fun Context.color(@ColorRes res: Int): Int {
    return ContextCompat.getColor(this, res)
}

@ColorInt
fun BaseEpoxyHolder.color(@ColorRes res: Int) = view.context.color(res)

fun Context.drawable(@DrawableRes res: Int): Drawable? {
    return ContextCompat.getDrawable(this, res)
}

fun BaseEpoxyHolder.drawable(@DrawableRes res: Int): Drawable? = view.context.drawable(res)

fun Context.tintedDrawable(@DrawableRes drawableId: Int, @ColorRes colorId: Int): Drawable? {
    val tint: Int = color(colorId)
    val drawable = drawable(drawableId)
    drawable?.mutate()
    drawable?.let {
        it.mutate()
        DrawableCompat.setTint(it, tint)
    }
    return drawable
}

fun BaseEpoxyHolder.tintedDrawable(@DrawableRes drawableId: Int, @ColorRes colorId: Int): Drawable? {
    return view.context.tintedDrawable(drawableId, colorId)
}

fun Context.string(@StringRes res: Int, vararg arguments: Any): String {
    return getString(res, *arguments)
}

fun BaseEpoxyHolder.string(@StringRes res: Int, vararg arguments: Any): String {
    return view.context.string(res, *arguments)
}

fun Context.colors(@ColorRes stateListRes: Int): ColorStateList? {
    return ContextCompat.getColorStateList(this, stateListRes)
}

fun BaseEpoxyHolder.colors(@ColorRes stateListRes: Int): ColorStateList? {
    return view.context.colors(stateListRes)
}

private fun Context.attribute(value: Int): TypedValue {
    val ret = TypedValue()
    theme.resolveAttribute(value, ret, true)
    return ret
}

/**
 * Get dimension defined by attribute [attr]
 */
fun Context.attrDimen(attr: Int): Int {
    return TypedValue.complexToDimensionPixelSize(attribute(attr).data, resources.displayMetrics)
}

/**
 * Get dimension defined in dimens.xml.
 */
fun Context.dimen(@DimenRes id: Int): Float {
    return resources.getDimension(id)
}

/**
 * Get drawable defined by attribute [attr]
 */
fun Context.attrDrawable(attr: Int): Drawable? {
    val a = theme.obtainStyledAttributes(intArrayOf(attr))
    val attributeResourceId = a.getResourceId(0, 0)
    a.recycle()
    return drawable(attributeResourceId)
}

/**
 * Get no animation settings as bundle for activity transitions.
 */
fun Context.noActivityAnimBundle(): Bundle? {
    return ActivityOptionsCompat.makeCustomAnimation(this,
        0, 0).toBundle()
}

/**
 * Created a bold span with a given [textRes]
 *
 * @param colored colors the bold span in red
 * @param insertLeadingSpace inserts a leading space in front of the span
 * @param insertTrailingSpace inserts a trailing space behind the span
 */
fun Context.getBoldSpan(
    @StringRes textRes: Int,
    colored: Boolean = false,
    insertLeadingSpace: Boolean = true,
    insertTrailingSpace: Boolean = true
): SpannableString {
    val spannable = SpannableString("${getSpace(insertLeadingSpace)}${getString(textRes)}${getSpace(insertTrailingSpace)}")
    spannable.setSpan(StyleSpan(Typeface.BOLD), 0, spannable.length, 0)

    if (colored) {
        spannable.setSpan(ForegroundColorSpan(color(R.color.rouge)), 0, spannable.length, 0)
    }
    return spannable
}

/**
 * Created a clickable bold span with a given [textRes]
 *
 * @param colored colors the bold span in red
 * @param insertLeadingSpace inserts a leading space in front of the span
 * @param insertTrailingSpace inserts a trailing space behind the span
 */
fun Context.getClickableBoldSpan(
    @StringRes textRes: Int,
    colored: Boolean = false,
    underline: Boolean = true,
    insertLeadingSpace: Boolean = true,
    insertTrailingSpace: Boolean = true,
    onClick: () -> Unit
): SpannableString {
    val clickableSpan = object : ClickableSpan() {

        override fun updateDrawState(ds: TextPaint) {
            ds.isUnderlineText = underline
        }

        override fun onClick(widget: View) {
            onClick()
        }
    }
    val spannable = SpannableString("${getSpace(insertLeadingSpace)}${getString(textRes)}${getSpace(insertTrailingSpace)}")
    spannable.setSpan(StyleSpan(Typeface.BOLD), 0, spannable.length, 0)
    spannable.setSpan(clickableSpan, 0, spannable.length, 0)

    if (colored) {
        spannable.setSpan(ForegroundColorSpan(color(R.color.rouge)), 0, spannable.length, 0)
    }

    return spannable
}

/**
 * Created a clickable span with a given [textRes]
 *
 * @param insertLeadingSpace inserts a leading space in front of the span
 * @param insertTrailingSpace inserts a trailing space behind the span
 */
fun Context.getClickableSpan(
    @StringRes textRes: Int,
    insertLeadingSpace: Boolean = true,
    insertTrailingSpace: Boolean = true,
    url: String
): SpannableString {

    val builder = SpannableStringBuilder()
    builder.append(getSpace(insertLeadingSpace))

    val spannable = SpannableString(getString(textRes))

    val clickableSpan = URLSpan(url)

    spannable.setSpan(clickableSpan, 0, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    spannable.setSpan(
        ForegroundColorSpan(color(R.color.black)),
        0,
        spannable.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    builder.append(spannable)
    builder.append(getSpace(insertTrailingSpace))

    return SpannableString.valueOf(builder)
}

private fun getSpace(insertSpace: Boolean): String {
    return if (insertSpace) {
        SPACE
    } else {
        EMPTY_STRING
    }
}

/**
 * Get the current language and convert it to configuration language.
 * Returns EN as fallback.
 */
fun Context.getCurrentConfigurationLanguage(): ConfigurationLanguage {
    return getString(R.string.current_language).let { currentLanguage ->
        ConfigurationLanguage.parse(currentLanguage).let {
            if (it == ConfigurationLanguage.UNDEFINED) {
                Timber.e(SilentError("Undefined language for questionnaire: $currentLanguage"))
                ConfigurationLanguage.EN // fallback when undefined
            } else it
        }
    }
}
