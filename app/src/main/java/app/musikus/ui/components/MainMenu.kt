/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.ui.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

enum class CommonMenuSelections {
    SETTINGS,
}

@Composable
fun CommonMenuItems(
    onSelectionHandler: (CommonMenuSelections) -> Unit
) {
    DropdownMenuItem(
        text = { Text(text = "Settings") },
        onClick = { onSelectionHandler(CommonMenuSelections.SETTINGS) }
    )
}

@Composable
fun MainMenu(
    show: Boolean,
    onDismissHandler: () -> Unit,
    onSelectionHandler: (
        commonSelection: CommonMenuSelections
    ) -> Unit,
    uniqueMenuItems: @Composable () -> Unit = {},
) {
    DropdownMenu(
        expanded = show,
        onDismissRequest = onDismissHandler,
        offset = DpOffset(x = (-80).dp, y = 0.dp) // no idea why this value has to be so large
    ) {
        uniqueMenuItems()
        CommonMenuItems(
            onSelectionHandler = onSelectionHandler
        )
    }
}
