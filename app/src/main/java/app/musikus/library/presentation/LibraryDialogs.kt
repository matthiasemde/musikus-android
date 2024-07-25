/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde
 */

package app.musikus.library.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.musikus.R
import app.musikus.core.presentation.components.DialogActions
import app.musikus.core.presentation.components.DialogHeader
import app.musikus.core.presentation.components.SelectionSpinner
import app.musikus.core.presentation.components.UUIDSelectionSpinnerOption
import app.musikus.library.data.daos.LibraryFolder
import app.musikus.core.presentation.theme.libraryItemColors
import app.musikus.core.presentation.utils.TestTags
import app.musikus.core.presentation.utils.UiText
import java.util.UUID


@Composable
fun LibraryFolderDialog(
    uiState: LibraryFolderDialogUiState,
    eventHandler: LibraryUiEventHandler
) {
    Dialog(onDismissRequest = { eventHandler(LibraryUiEvent.FolderDialogDismissed) }) {
        Column(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            DialogHeader(
                title = stringResource(id = when (uiState.mode) {
                    DialogMode.ADD -> R.string.library_folder_dialog_title
                    DialogMode.EDIT ->R.string.library_folder_dialog_title_edit
                }),
            )
            Column {
                OutlinedTextField(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .testTag(TestTags.FOLDER_DIALOG_NAME_INPUT),
                    value = uiState.folderData.name,
                    onValueChange = { eventHandler(LibraryUiEvent.FolderDialogNameChanged(it)) },
                    label = { Text(text = stringResource(id = R.string.library_folder_dialog_name_label)) },
                    singleLine = true,
                )
                DialogActions(
                    confirmButtonText = stringResource(id = when(uiState.mode) {
                        DialogMode.ADD -> R.string.library_folder_dialog_confirm
                        DialogMode.EDIT -> R.string.library_folder_dialog_confirm_edit
                    }),
                    onDismissHandler = { eventHandler(LibraryUiEvent.FolderDialogDismissed) },
                    onConfirmHandler = { eventHandler(LibraryUiEvent.FolderDialogConfirmed) },
                    confirmButtonEnabled = uiState.folderData.name.isNotEmpty()
                )
            }
        }
    }
}

interface LibraryItemDialogUiState {
    val mode: DialogMode
    val itemData: LibraryItemEditData
    val folders : List<LibraryFolder>
    val isConfirmButtonEnabled: Boolean
}

typealias LibraryItemDialogUiEventHandler = (LibraryItemDialogUiEvent) -> Unit

sealed class LibraryItemDialogUiEvent {
    data class NameChanged(val name: String) : LibraryItemDialogUiEvent()
    data class ColorIndexChanged(val colorIndex: Int) : LibraryItemDialogUiEvent()
    data class FolderIdChanged(val folderId: UUID?) : LibraryItemDialogUiEvent()
    data object Confirmed : LibraryItemDialogUiEvent()
    data object Dismissed : LibraryItemDialogUiEvent()
}

@Composable
fun LibraryItemDialog(
    uiState: LibraryItemDialogUiState,
    eventHandler: LibraryItemDialogUiEventHandler
) {

    var folderSelectorExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { eventHandler(LibraryItemDialogUiEvent.Dismissed) }) {
        Column(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            DialogHeader(title = when(uiState.mode) {
                DialogMode.ADD -> stringResource(id = R.string.library_item_dialog_title)
                DialogMode.EDIT -> stringResource(id = R.string.library_item_dialog_title_edit)
            })
            Column {
                OutlinedTextField(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .testTag(TestTags.ITEM_DIALOG_NAME_INPUT),
                    value = uiState.itemData.name,
                    onValueChange = { eventHandler(LibraryItemDialogUiEvent.NameChanged(it)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = stringResource(id = R.string.library_item_dialog_name_label),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    label = { Text(text = stringResource(id = R.string.library_item_dialog_name_label)) },
                    singleLine = true,
                )
                if(uiState.folders.isNotEmpty()) {
                    SelectionSpinner(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .padding(horizontal = 24.dp),
                        expanded = folderSelectorExpanded,
                        label = { Text(text = stringResource(id = R.string.library_item_dialog_folder_selector_label)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = stringResource(id = R.string.library_item_dialog_folder_selector_label),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        options = uiState.folders.map { folder ->
                            UUIDSelectionSpinnerOption(folder.id, UiText.DynamicString(folder.name))
                        },
                        selected = UUIDSelectionSpinnerOption(
                            id = uiState.itemData.folderId,
                            name = uiState.folders.firstOrNull {
                                it.id == uiState.itemData.folderId
                            }?.name?.let { UiText.DynamicString(it) } ?: UiText.StringResource(R.string.library_item_dialog_folder_selector_no_folder) ),
                        specialOption = UUIDSelectionSpinnerOption(
                            null,
                            UiText.StringResource(R.string.library_item_dialog_folder_selector_no_folder)
                        ),
                        semanticDescription = stringResource(id = R.string.library_item_dialog_folder_selector_description),
                        dropdownTestTag = TestTags.ITEM_DIALOG_FOLDER_SELECTOR_DROPDOWN,
                        onExpandedChange = { folderSelectorExpanded = it },
                        onSelectedChange = {
                            eventHandler(LibraryItemDialogUiEvent.FolderIdChanged((it as UUIDSelectionSpinnerOption).id))
                            folderSelectorExpanded = false
                        },
                    )
                }
                Row(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for(i in 0..4) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (j in 0..1) {
                                val index = 2*i+j
                                ColorSelectRadioButton(
                                    color = libraryItemColors[index],
                                    selected = uiState.itemData.colorIndex == index,
                                    colorDescription = stringResource(id = R.string.library_item_dialog_color_selector_description, (index + 1)),
                                    onClick = { eventHandler(LibraryItemDialogUiEvent.ColorIndexChanged(index)) }
                                )
                            }
                        }
                    }
                }
            }
            DialogActions(
                confirmButtonText = when(uiState.mode) {
                    DialogMode.ADD -> stringResource(id = R.string.library_item_dialog_confirm)
                    DialogMode.EDIT -> stringResource(id = R.string.library_item_dialog_confirm_edit)
                },
                confirmButtonEnabled = uiState.isConfirmButtonEnabled,
                onDismissHandler = { eventHandler(LibraryItemDialogUiEvent.Dismissed) },
                onConfirmHandler = { eventHandler(LibraryItemDialogUiEvent.Confirmed) }
            )
        }
    }
}

@Composable
fun ColorSelectRadioButton(
    modifier: Modifier = Modifier,
    color: Color,
    colorDescription: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(35.dp)
            .clip(RoundedCornerShape(100))
            .background(color)
            .semantics {
                contentDescription = colorDescription
            }
    ) {
        RadioButton(
            colors = RadioButtonDefaults.colors(
                selectedColor = Color.White,
                unselectedColor = Color.White,
            ),
            selected = selected,
            onClick = onClick
        )
    }
}