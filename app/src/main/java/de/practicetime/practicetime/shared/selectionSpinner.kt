/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

enum class SpinnerState {
    EXPANDED,
    COLLAPSED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionSpinner(
    modifier: Modifier = Modifier,
    state: SpinnerState,
    label: @Composable () -> Unit,
    leadingIcon: @Composable () -> Unit,
    options: List<Pair<Long, String>>,
    selected: Long?,
    defaultOption: String?,
    onStateChange: (SpinnerState) -> Unit,
    onSelectedChange: (Long?) -> Unit
) {
    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = state == SpinnerState.EXPANDED,
        onExpandedChange = {
            when (state) {
                SpinnerState.EXPANDED -> onStateChange(SpinnerState.COLLAPSED)
                SpinnerState.COLLAPSED -> onStateChange(SpinnerState.EXPANDED)
            }
        }
    ) {
        var size by remember { mutableStateOf(0) }
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .onGloballyPositioned {
                    size = it.size.width
                },
            value = options.firstOrNull { it.first == selected }?.second ?: defaultOption?: "",
            label = label,
            onValueChange = {},
            readOnly = true,
            leadingIcon = leadingIcon,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = state == SpinnerState.EXPANDED)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            modifier = Modifier
                .heightIn(max = 350.dp),
            expanded = state == SpinnerState.EXPANDED,
            onDismissRequest = { onStateChange(SpinnerState.COLLAPSED) },
        ) {

            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .width(with(LocalDensity.current) { size.toDp() })
                    .height((48 * (options.size + if(defaultOption != null) 1 else 0)).coerceAtMost(240).dp)
                    .simpleVerticalScrollbar(listState)
            ) {
                defaultOption?.let {
                    item {
                        DropdownMenuItem(
                            modifier = Modifier.height(46.dp),
                            text = { Text(text = it) },
                            onClick = { onSelectedChange(null) }
                        )
                        Divider(thickness = 2.dp)
                    }
                }
                items(
                    items = options,
                    key = { it.first },
                ) { item ->
                    DropdownMenuItem(
                        modifier = Modifier.height(48.dp),
                        onClick = { onSelectedChange(item.first)  },
                        text = { Text(text = item.second) }
                    )
                }
            }
        }
    }
}