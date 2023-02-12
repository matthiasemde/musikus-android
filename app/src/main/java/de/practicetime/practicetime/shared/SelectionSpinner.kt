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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.*

sealed class SelectionSpinnerOption(val name: String)
class UUIDSelectionSpinnerOption(val id: UUID?, name: String) : SelectionSpinnerOption(name)
class IntSelectionSpinnerOption(val id: Int?, name: String) : SelectionSpinnerOption(name)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionSpinner(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    label: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    options: List<SelectionSpinnerOption>,
    selected: SelectionSpinnerOption,
    specialOption: SelectionSpinnerOption? = null,
    onExpandedChange: (Boolean) -> Unit,
    onSelectedChange: (SelectionSpinnerOption) -> Unit
) {
    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        var size by remember { mutableStateOf(0) }
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .onGloballyPositioned {
                    size = it.size.width
                },
            value = selected.name,
            label = label,
            onValueChange = {},
            readOnly = true,
            leadingIcon = leadingIcon,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {

            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .width(with(LocalDensity.current) { size.toDp() })
                    .height((48 * (options.size + if(specialOption != null) 1 else 0)).coerceAtMost(240).dp)
                    .simpleVerticalScrollbar(listState)
            ) {
                specialOption?.let {
                    item {
                        DropdownMenuItem(
                            modifier = Modifier
                                .padding(end= 12.dp)
                                .height(46.dp),
                            text = { Text(text = it.name) },
                            onClick = { onSelectedChange(it) }
                        )
                        Divider(Modifier.padding(end = 12.dp), thickness = Dp.Hairline)
                    }
                }
                items(
                    items = options,
                    key = {
                        when (it) {
                            is UUIDSelectionSpinnerOption -> it.id!!
                            is IntSelectionSpinnerOption -> it.id!!
                        }
                    },
                ) { item ->
                    DropdownMenuItem(
                        modifier = Modifier
                            .padding(end= 12.dp)
                            .height(48.dp),
                        onClick = { onSelectedChange(item) },
                        text = { Text(text = item.name) }
                    )
                }
            }
        }
    }
}