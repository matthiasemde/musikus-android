/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.core.presentation.components

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.musikus.core.presentation.theme.spacing
import app.musikus.utils.UiIcon
import app.musikus.utils.UiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteConfirmationBottomSheet(
    explanation: UiText? = null,
    confirmationIcon: UiIcon,
    confirmationText: UiText,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        windowInsets = WindowInsets(top = 0.dp), // makes sure the scrim covers the status bar
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {

        if(explanation != null) {
            Text(
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
                text = explanation.asString(),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.copy(alpha = 0.8f),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onConfirm)
                .padding(
                    vertical = MaterialTheme.spacing.medium,
                    horizontal = MaterialTheme.spacing.large
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(confirmationIcon.asIcon(), contentDescription = confirmationText.asString())
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraLarge))
            Text(
                text = confirmationText.asString(),
                style = MaterialTheme.typography.titleMedium
            )
        }

        // WindowsInsets.navigationBars doesn't work on API level < 30,
        // so we need to add a fixed padding to make sure
        // the bottom sheet is above the navigation bar
        val navigationBarHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        } else {
            val content = LocalContext.current.contentResolver
            if (Settings.Secure.getInt(content, "navigation_mode", 0) == 2) {
                16.dp // height of the gesture navigation
            } else {
                48.dp // height of the 2 or 3-button navigation
            }
        }

        Spacer(
            modifier = Modifier.height(navigationBarHeight + MaterialTheme.spacing.extraSmall)
        )
    }
}