/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-2025 Matthias Emde
 */

package app.musikus.core.presentation.components

import androidx.compose.runtime.Composable
import app.musikus.core.presentation.utils.ObserveAsEvents
import kotlinx.coroutines.flow.Flow


@Composable
inline fun <reified T> ExceptionHandler(
    exceptionChannel: Flow<Exception>,
    crossinline exceptionHandler: (T) -> Unit,
    crossinline onUnhandledException: (Exception) -> Unit
) {
    ObserveAsEvents(exceptionChannel) {
        when (it) {
            is T -> exceptionHandler(it)
            else -> onUnhandledException(it)
        }
    }
}
