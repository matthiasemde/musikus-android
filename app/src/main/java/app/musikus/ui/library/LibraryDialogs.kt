/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.ui.library

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import app.musikus.database.daos.LibraryFolder
import app.musikus.ui.components.DialogHeader
import app.musikus.ui.components.SelectionSpinner
import app.musikus.ui.components.UUIDSelectionSpinnerOption
import app.musikus.ui.theme.libraryItemColors
import app.musikus.utils.TestTags
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
            DialogHeader(title = when(uiState.mode) {
                    DialogMode.ADD -> "Create folder"
                    DialogMode.EDIT -> "Edit folder"
                },
            )
            Column {
                OutlinedTextField(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .testTag(TestTags.FOLDER_DIALOG_NAME_INPUT),
                    value = uiState.folderData.name,
                    onValueChange = { eventHandler(LibraryUiEvent.FolderDialogNameChanged(it)) },
                    label = { Text(text = "Folder name") },
                    singleLine = true,
                )
                Row(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { eventHandler(LibraryUiEvent.FolderDialogDismissed) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = "Cancel")
                    }
                    val confirmText = when(uiState.mode) {
                        DialogMode.ADD -> "Create"
                        DialogMode.EDIT -> "Edit"
                    }
                    TextButton(
                        onClick = { eventHandler(LibraryUiEvent.FolderDialogConfirmed) },
                        enabled = uiState.folderData.name.isNotEmpty(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = confirmText
                        }
                    ) {
                        Text(text = confirmText)
                    }
                }
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
                DialogMode.ADD -> stringResource(id = R.string.addLibraryItemDialogTitle)
                DialogMode.EDIT -> stringResource(id = R.string.addLibraryItemDialogTitleEdit)
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
                            contentDescription = "Item name",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    label = { Text(text = "Item name") },
                    singleLine = true,
                )
                if(uiState.folders.isNotEmpty()) {
                    SelectionSpinner(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .padding(horizontal = 24.dp),
                        expanded = folderSelectorExpanded,
                        label = { Text(text = "Folder") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Folder",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        options = uiState.folders.map { folder -> UUIDSelectionSpinnerOption(folder.id, folder.name) },
                        selected = UUIDSelectionSpinnerOption(
                            id = uiState.itemData.folderId,
                            name = uiState.folders.firstOrNull {
                                it.id == uiState.itemData.folderId
                            }?.name ?: "No folder" ),
                        specialOption = UUIDSelectionSpinnerOption(null, "No folder"),
                        semanticDescription = "Select folder",
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
                                    colorDescription = "Color ${index+1}",
                                    onClick = { eventHandler(LibraryItemDialogUiEvent.ColorIndexChanged(index)) }
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { eventHandler(LibraryItemDialogUiEvent.Dismissed) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = "Cancel")
                    }
                    val confirmText = when(uiState.mode) {
                        DialogMode.ADD -> "Create"
                        DialogMode.EDIT -> "Edit"
                    }
                    TextButton(
                        onClick = { eventHandler(LibraryItemDialogUiEvent.Confirmed) },
                        enabled = uiState.isConfirmButtonEnabled,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = confirmText
                        }
                    ) {
                        Text(text = confirmText)
                    }
                }
            }
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