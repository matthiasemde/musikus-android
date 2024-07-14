/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

// inspired by: https://www.youtube.com/watch?v=njchj9d_Lf8 (Phillip Lackner)

@Composable
inline fun <reified T> ExceptionHandler(
    exceptionChannel: Flow<Exception>,
    crossinline exceptionHandler: (T) -> Unit,
    crossinline onUnhandledException: (Exception) -> Unit
) {
    val lifeCycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(exceptionChannel, lifeCycleOwner.lifecycle) {
        lifeCycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            exceptionChannel.collect {
                when (it) {
                    is T -> exceptionHandler(it)
                    else -> onUnhandledException(it)
                }
            }
        }
    }

}