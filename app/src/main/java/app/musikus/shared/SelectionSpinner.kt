/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    semanticDescription: String,
    dropdownTestTag: String,
    onExpandedChange: (Boolean) -> Unit,
    onSelectedChange: (SelectionSpinnerOption) -> Unit
) {
    ExposedDropdownMenuBox(
        modifier = modifier.semantics {
            contentDescription = semanticDescription
        },
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        CompositionLocalProvider(LocalTextInputService provides null) {
            OutlinedTextField(
                readOnly = true,
                modifier = Modifier
                    .menuAnchor(),
                value = selected.name,
                onValueChange = {},
                label = label,
                leadingIcon = leadingIcon,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            )
            ExposedDropdownMenu(
                modifier = Modifier.testTag(dropdownTestTag),
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
            ) {

                val scrollState = rememberScrollState()
                val singleDropdownItemHeight = 48.dp
                val totalDropDownMenuHeight = (
                        singleDropdownItemHeight * (options.size + if(specialOption != null) 1 else 0)
                ).coerceAtMost(220.dp)
                val totalDropDownMenuHeightInPx = with(LocalDensity.current) { totalDropDownMenuHeight.toPx() }

                val scrollBarShowing = scrollState.maxValue > 0

                Column(
                    modifier = Modifier
                        .height(totalDropDownMenuHeight)
                        .verticalScroll(scrollState)
                        .conditional(scrollBarShowing) {
                            simpleVerticalScrollbar(scrollState, totalDropDownMenuHeightInPx)
                        }
                ) {
                    specialOption?.let {
                            DropdownMenuItem(
                                modifier = Modifier
                                    .conditional(scrollBarShowing) { padding(end = 12.dp) }
                                    .height(singleDropdownItemHeight - 2.dp),
                                text = { Text(text = it.name) },
                                onClick = { onSelectedChange(it) }
                            )
                            HorizontalDivider(
                                Modifier
                                    .conditional(scrollBarShowing) { padding(end = 12.dp) },
                                thickness = Dp.Hairline
                            )
                    }
                    options.forEach { option ->
                        DropdownMenuItem(
                            modifier = Modifier
                                .conditional(scrollBarShowing) { padding(end = 12.dp) }
                                .height(singleDropdownItemHeight),
                            onClick = { onSelectedChange(option) },
                            text = { Text(text = option.name) }
                        )
                    }
                }
            }
        }
    }
}