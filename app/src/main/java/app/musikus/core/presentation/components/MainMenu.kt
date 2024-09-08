/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde
 */

package app.musikus.core.presentation.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.musikus.R

enum class CommonMenuSelections {
    SETTINGS,
}

@Composable
fun CommonMenuItems(
    onSelection: (CommonMenuSelections) -> Unit
) {
    DropdownMenuItem(
        text = { Text(text = stringResource(R.string.components_main_menu)) },
        onClick = { onSelection(CommonMenuSelections.SETTINGS) }
    )
}

@Composable
fun MainMenu(
    show: Boolean,
    onDismiss: () -> Unit,
    onSelection: (commonSelection: CommonMenuSelections) -> Unit,
    uniqueMenuItems: @Composable () -> Unit = {},
) {
    DropdownMenu(
        expanded = show,
        onDismissRequest = onDismiss,
        offset = DpOffset(x = (-80).dp, y = 0.dp) // no idea why this value has to be so large
    ) {
        uniqueMenuItems()
        CommonMenuItems(
            onSelection = onSelection
        )
    }
}
