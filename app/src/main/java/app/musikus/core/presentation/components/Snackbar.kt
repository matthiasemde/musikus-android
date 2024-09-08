/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.core.presentation.components

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import app.musikus.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun showSnackbar(
    context: Context,
    scope: CoroutineScope,
    hostState: SnackbarHostState,
    message: String,
    onUndo: (() -> Unit)? = null
) {
    scope.launch {
        val result = hostState.showSnackbar(
            message,
            actionLabel = if (onUndo != null) context.getString(R.string.components_snackbar_undo) else null,
            duration = SnackbarDuration.Long
        )
        when (result) {
            SnackbarResult.ActionPerformed -> {
                onUndo?.invoke()
            }

            SnackbarResult.Dismissed -> {
                // do nothing
            }
        }
    }
}
