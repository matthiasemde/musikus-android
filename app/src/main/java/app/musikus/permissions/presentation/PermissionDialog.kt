/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.permissions.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.window.Dialog
import app.musikus.core.presentation.components.DialogActions
import app.musikus.core.presentation.components.DialogHeader
import app.musikus.core.presentation.theme.MusikusColorSchemeProvider
import app.musikus.core.presentation.theme.MusikusPreviewElement1
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.core.presentation.theme.spacing
import app.musikus.settings.domain.ColorSchemeSelections

@Composable
fun PermissionDialog(
    description: String,
    isPermanentlyDeclined: Boolean,
    onDismiss: () -> Unit,
    onOkClick: () -> Unit,
    onGoToAppSettingsClick: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column {
                DialogHeader("Permission required")
                Column(Modifier.padding(horizontal = MaterialTheme.spacing.medium)) {
                    Text(text = description)
                    DialogActions(
                        onDismissHandler = onDismiss,
                        onConfirmHandler = if (isPermanentlyDeclined) onGoToAppSettingsClick else onOkClick,
                        confirmButtonText = if (isPermanentlyDeclined) "Go to App Settings" else "Ok",
                        dismissButtonText = "Cancel",
                    )
                }
            }
        }
    }
}

@MusikusPreviewElement1
@Composable
fun PermissionDialogPreview(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme) {
        PermissionDialog(
            description = "Permission",
            isPermanentlyDeclined = true,
            onDismiss = {},
            onOkClick = {},
            onGoToAppSettingsClick = {},
        )
    }
}