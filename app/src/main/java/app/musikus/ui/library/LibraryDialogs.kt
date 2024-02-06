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
import app.musikus.shared.DialogHeader
import app.musikus.shared.SelectionSpinner
import app.musikus.shared.UUIDSelectionSpinnerOption
import app.musikus.ui.theme.libraryItemColors
import app.musikus.utils.TestTags
import java.util.UUID


@Composable
fun LibraryFolderDialog(
    mode: DialogMode,
    folderData: LibraryFolderEditData,
    onFolderNameChange: (String) -> Unit,
    onConfirmHandler: () -> Unit,
    onDismissHandler: () -> Unit,
) {
    Dialog(onDismissRequest = onDismissHandler) {
        Column(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            DialogHeader(title = when(mode) {
                    DialogMode.ADD -> "Create folder"
                    DialogMode.EDIT -> "Edit folder"
                },
            )
            Column {
                OutlinedTextField(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .testTag(TestTags.FOLDER_DIALOG_NAME_INPUT),
                    value = folderData.name,
                    onValueChange = onFolderNameChange,
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
                        onClick = onDismissHandler,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = "Cancel")
                    }
                    val confirmText = when(mode) {
                        DialogMode.ADD -> "Create"
                        DialogMode.EDIT -> "Edit"
                    }
                    TextButton(
                        onClick = onConfirmHandler,
                        enabled = folderData.name.isNotEmpty(),
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
fun LibraryItemDialog(
    mode: DialogMode,
    folders: List<LibraryFolder>,
    itemData: LibraryItemEditData,
    isConfirmButtonEnabled: Boolean,
    onNameChange: (String) -> Unit,
    onColorIndexChange: (Int) -> Unit,
    onSelectedFolderIdChange: (UUID?) -> Unit,
    onConfirmHandler: () -> Unit,
    onDismissHandler: () -> Unit,
) {

    var folderSelectorExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismissHandler) {
        Column(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            DialogHeader(title = when(mode) {
                DialogMode.ADD -> stringResource(id = R.string.addLibraryItemDialogTitle)
                DialogMode.EDIT -> stringResource(id = R.string.addLibraryItemDialogTitleEdit)
            })
            Column {
                OutlinedTextField(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .testTag(TestTags.ITEM_DIALOG_NAME_INPUT),
                    value = itemData.name,
                    onValueChange = onNameChange,
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
                if(folders.isNotEmpty()) {
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
                        options = folders.map { folder -> UUIDSelectionSpinnerOption(folder.id, folder.name) },
                        selected = UUIDSelectionSpinnerOption(
                            id = itemData.folderId,
                            name = folders.firstOrNull {
                                it.id == itemData.folderId
                            }?.name ?: "No folder" ),
                        specialOption = UUIDSelectionSpinnerOption(null, "No folder"),
                        semanticDescription = "Select folder",
                        dropdownTestTag = TestTags.ITEM_DIALOG_FOLDER_SELECTOR_DROPDOWN,
                        onExpandedChange = { folderSelectorExpanded = it },
                        onSelectedChange = {
                            onSelectedFolderIdChange((it as UUIDSelectionSpinnerOption).id)
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
                                    selected = itemData.colorIndex == index,
                                    colorDescription = "Color ${index+1}",
                                    onClick = { onColorIndexChange(index) }
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
                        onClick = onDismissHandler,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = "Cancel")
                    }
                    val confirmText = when(mode) {
                        DialogMode.ADD -> "Create"
                        DialogMode.EDIT -> "Edit"
                    }
                    TextButton(
                        onClick = onConfirmHandler,
                        enabled = isConfirmButtonEnabled,
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