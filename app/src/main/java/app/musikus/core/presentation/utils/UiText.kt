/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde
 */

package app.musikus.core.presentation.utils

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.text.HtmlCompat

// source: https://www.youtube.com/watch?v=mB1Lej0aDus (Phillip Lackner)
sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    data class DynamicAnnotatedString(val value: AnnotatedString) : UiText()
    class StringResource(
        @StringRes val resId: Int,
        vararg val args: Any
    ) : UiText()

    class PluralResource(
        @PluralsRes val resId: Int,
        val quantity: Int,
        vararg val formatArgs: Any
    ) : UiText()

    class HtmlResource(
        @StringRes val resId: Int,
        vararg val args: Any
    ) : UiText()

    @Composable
    fun asAnnotatedString(): AnnotatedString {
        return when (this) {
            is DynamicString -> AnnotatedString(value)
            is DynamicAnnotatedString -> value
            is StringResource -> AnnotatedString(stringResource(resId, *args))
            is PluralResource -> AnnotatedString(pluralStringResource(resId, quantity, *formatArgs))
            is HtmlResource -> htmlResource(resId, *args)
        }
    }

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is DynamicAnnotatedString -> value.text
            is StringResource -> stringResource(resId, *args)
            is PluralResource -> pluralStringResource(resId, quantity, *formatArgs)
            is HtmlResource -> throw IllegalArgumentException("Cannot convert HTML resource to string")
        }
    }
}

@Composable
fun List<UiText>.asAnnotatedString(
    separator: String = " "
) = map { it.asAnnotatedString() }.joinToString(separator) { it }

// source: https://stackoverflow.com/questions/68549248/android-jetpack-compose-how-to-show-styled-text-from-string-resources
@Composable
fun htmlResource(@StringRes resId: Int, vararg formatArgs: Any): AnnotatedString {
    val rawHtml = stringResource(resId, *formatArgs)
    val spanned = HtmlCompat.fromHtml(rawHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)
    return AnnotatedString.Builder().apply {
        append(spanned.toString())
        spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
            println(span)
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)

            when (span) {
                is android.text.style.StyleSpan -> {
                    when (span.style) {
                        android.graphics.Typeface.BOLD -> {
                            addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                        }
                        android.graphics.Typeface.ITALIC -> {
                            addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                        }
                    }
                }
                is android.text.style.UnderlineSpan -> {
                    addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                }
            }
        }
    }.toAnnotatedString()
}
