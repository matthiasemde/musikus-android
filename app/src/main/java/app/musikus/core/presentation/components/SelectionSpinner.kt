/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.core.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.musikus.core.presentation.utils.UiText
import java.util.*

sealed class SelectionSpinnerOption(val name: UiText)
class UUIDSelectionSpinnerOption(val id: UUID?, name: UiText) : SelectionSpinnerOption(name)
class IntSelectionSpinnerOption(val id: Int?, name: UiText) : SelectionSpinnerOption(name)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionSpinner(
    modifier: Modifier = Modifier,
    spinnerState: SelectionSpinnerState,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    semanticDescription: String,
    dropdownTestTag: String,
    onSelectedChange: (SelectionSpinnerOption) -> Unit = {},
) {
    require(label != null || placeholder != null) {
        throw IllegalArgumentException(
            "SelectionSpinner needs either a label or a placeholder if no option is selected"
        )
    }

    ExposedDropdownMenuBox(
        modifier = modifier.semantics {
            contentDescription = semanticDescription
        },
        expanded = spinnerState.expanded,
        onExpandedChange = { spinnerState.expand() }
    ) {
        CompositionLocalProvider(LocalTextInputService provides null) {
            OutlinedTextField(
                readOnly = true,
                modifier = Modifier.menuAnchor(),
                value = spinnerState.currentSelection?.name?.asString() ?: "", // if no option is selected, show nothing
                onValueChange = {},
                label = label,
                placeholder = placeholder,
                leadingIcon = leadingIcon,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = spinnerState.expanded)
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            )

            ExposedDropdownMenu(
                modifier = Modifier.testTag(dropdownTestTag),
                expanded = spinnerState.expanded,
                onDismissRequest = { spinnerState.collapse() },
            ) {
                val scrollState = rememberScrollState()
                val singleDropdownItemHeight = 48.dp
                val totalDropDownMenuHeight = (
                    singleDropdownItemHeight * (spinnerState.options.size + if (spinnerState.specialOption != null) 1 else 0)
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
                    spinnerState.specialOption?.let {
                        DropdownMenuItem(
                            modifier = Modifier
                                .conditional(scrollBarShowing) { padding(end = 12.dp) }
                                .height(singleDropdownItemHeight - 2.dp),
                            text = { Text(text = it.name.asString()) },
                            onClick = {
                                spinnerState.collapse()
                                onSelectedChange(it)
                            }
                        )
                        HorizontalDivider(
                            Modifier
                                .conditional(scrollBarShowing) { padding(end = 12.dp) },
                            thickness = Dp.Hairline
                        )
                    }
                    spinnerState.options.forEach { option ->
                        DropdownMenuItem(
                            modifier = Modifier
                                .conditional(scrollBarShowing) { padding(end = 12.dp) }
                                .height(singleDropdownItemHeight),
                            onClick = {
                                spinnerState.collapse()
                                onSelectedChange(option)
                            },
                            text = { Text(text = option.name.asString()) }
                        )
                    }
                }
            }
        }
    }
}


/**
 * State for the period selection spinner element.
 */
@Stable
class SelectionSpinnerState(
    initialSelection: SelectionSpinnerOption? = null,
    val options: List<SelectionSpinnerOption>,
    val specialOption: SelectionSpinnerOption? = null,
    initialExpand: Boolean = false,
) {
    init {
        require(options.contains(initialSelection)) {
            "The initial selection must be one of the options."
        }
    }

    var currentSelection by mutableStateOf(initialSelection)
        private set

    var expanded by mutableStateOf(initialExpand)
        private set

    /**
     * Saver to support rememberSaveable / Bundle saving and restoring.
     */
    companion object {
        fun Saver(): Saver<SelectionSpinnerState, *> = Saver(
            save = {
                listOf(
                    it.currentSelection,
                    it.options,
                    it.specialOption
                )
            },
            restore = {
                SelectionSpinnerState(
                    initialSelection = it[0] as SelectionSpinnerOption,
                    options = (it[1] as List<*>).filterIsInstance<SelectionSpinnerOption>(),
                    specialOption = it[2] as SelectionSpinnerOption?
                )
            }
        )
    }

    fun collapse() {
        expanded = false
    }

    fun expand() {
        expanded = true
    }

    fun select(option: SelectionSpinnerOption) {
        require(options.contains(option)) {
            "The selected option must be one of the options."
        }
        currentSelection = option
    }
}

/**
 * RememberSaveable wrapper for SelectionSpinnerState.
 * Use to generate a SelectionSpinnerState instance which survives configuration changes.
 */
@Composable
fun rememberSelectionSpinnerState(
    initialSelection: SelectionSpinnerOption? = null,
    options: List<SelectionSpinnerOption>,
    specialOption: SelectionSpinnerOption? = null,
    initialExpand: Boolean = false,
) = rememberSaveable(saver = SelectionSpinnerState.Saver()) {
    SelectionSpinnerState(
        initialSelection = initialSelection,
        options = options,
        specialOption = specialOption,
        initialExpand = initialExpand
    )
}
