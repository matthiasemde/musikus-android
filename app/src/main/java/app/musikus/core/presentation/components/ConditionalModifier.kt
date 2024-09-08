/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde
 */

package app.musikus.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// source: https://stackoverflow.com/questions/67768746/chaining-modifier-based-on-certain-conditions-in-android-compose

@Composable
fun Modifier.conditional(
    condition: Boolean,
    alternativeModifier: @Composable Modifier.() -> Modifier = { this },
    modifier: @Composable Modifier.() -> Modifier,
): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        then(alternativeModifier(Modifier))
    }
}
