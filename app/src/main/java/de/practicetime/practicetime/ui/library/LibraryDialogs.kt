/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.LibraryFolder
import de.practicetime.practicetime.shared.DialogHeader
import de.practicetime.practicetime.shared.SelectionSpinner
import de.practicetime.practicetime.shared.UUIDSelectionSpinnerOption
import de.practicetime.practicetime.viewmodel.DialogMode
import de.practicetime.practicetime.viewmodel.LibraryFolderEditData
import de.practicetime.practicetime.viewmodel.LibraryItemEditData
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
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
                    modifier = Modifier.padding(horizontal = 24.dp),
                    value = folderData.name, onValueChange = onFolderNameChange,
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
                    TextButton(
                        onClick = onConfirmHandler,
                        enabled = folderData.name.isNotEmpty(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = when(mode) {
                            DialogMode.ADD -> "Create"
                            DialogMode.EDIT -> "Edit"
                        })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryItemDialog(
    mode: DialogMode,
    folders: List<LibraryFolder>,
    itemData: LibraryItemEditData,
    folderSelectorExpanded: Boolean,
    onNameChange: (String) -> Unit,
    onColorIndexChange: (Int) -> Unit,
    onSelectedFolderIdChange: (UUID?) -> Unit,
    onFolderSelectorExpandedChange: (Boolean) -> Unit,
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
                DialogMode.ADD -> stringResource(id = R.string.addLibraryItemDialogTitle)
                DialogMode.EDIT -> stringResource(id = R.string.addLibraryItemDialogTitleEdit)
            })
            Column {
                OutlinedTextField(
                    modifier = Modifier.padding(horizontal = 24.dp),
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
                        onExpandedChange = onFolderSelectorExpandedChange,
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
                                ColorSelectRadioButton(
                                    color = Color(PracticeTime.getLibraryItemColors(LocalContext.current)[2*i+j]),
                                    selected = itemData.colorIndex == 2*i+j,
                                    onClick = { onColorIndexChange(2*i+j) }
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
                    TextButton(
                        onClick = onConfirmHandler,
                        enabled = itemData.name.isNotEmpty(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = when(mode) {
                            DialogMode.ADD -> "Create"
                            DialogMode.EDIT -> "Edit"
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun ColorSelectRadioButton(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(35.dp)
            .clip(RoundedCornerShape(100))
            .background(color)
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