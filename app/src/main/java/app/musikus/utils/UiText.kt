/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.utils

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString


// source: https://www.youtube.com/watch?v=mB1Lej0aDus (Phillip Lackner)
sealed class UiText {
    data class DynamicString(val value: String): UiText()
    data class DynamicAnnotatedString(val value: AnnotatedString): UiText()
    class StringResource(
        @StringRes val resId: Int,
        vararg val args: Any
    ): UiText()

    class PluralResource(
        @PluralsRes val resId: Int,
        val quantity: Int,
        vararg val formatArgs: Any
    ): UiText()

    @Composable
    fun asString(): AnnotatedString {
        return when(this) {
            is DynamicString -> AnnotatedString(value)
            is DynamicAnnotatedString -> value
            is StringResource -> AnnotatedString(stringResource(resId, *args))
            is PluralResource -> AnnotatedString(pluralStringResource(resId, quantity, *formatArgs))
        }
    }
}

@Composable
fun List<UiText>.asString() = map { it.asString() }.joinToString(separator = " ") { it }
