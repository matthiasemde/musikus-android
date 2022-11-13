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
import de.practicetime.practicetime.shared.SpinnerState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryFolderDialog(
    mode: DialogMode,
    folderName: String,
    onFolderNameChange: (String) -> Unit,
    onDismissHandler: (Boolean) -> Unit, // true if folder was created
) {
    Dialog(onDismissRequest = { onDismissHandler(false) }) {
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
                    value = folderName, onValueChange = onFolderNameChange,
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
                        onClick = { onDismissHandler(false) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = "Cancel")
                    }
                    TextButton(
                        onClick = { onDismissHandler(true) },
                        enabled = folderName.isNotEmpty(),
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
    name: String,
    colorIndex: Int,
    folderId: Long?,
    folderSelectorExpanded: SpinnerState,
    onNameChange: (String) -> Unit,
    onColorIndexChange: (Int) -> Unit,
    onFolderIdChange: (Long?) -> Unit,
    onFolderSelectorExpandedChange: (SpinnerState) -> Unit,
    onDismissHandler: (Boolean) -> Unit, // true if dialog was canceled
) {
    Dialog(onDismissRequest = { onDismissHandler(true) }) {
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
                    value = name,
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
                        state = folderSelectorExpanded,
                        label = { Text(text = "Folder") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Folder",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        options = folders.map { folder -> Pair(folder.id, folder.name) },
                        selected = folderId,
                        defaultOption = "No folder",
                        onStateChange = onFolderSelectorExpandedChange,
                        onSelectedChange = onFolderIdChange,
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
                                    selected = colorIndex == 2*i+j,
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
                        onClick = { onDismissHandler(true) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = "Cancel")
                    }
                    TextButton(
                        onClick = { onDismissHandler(false) },
                        enabled = name.isNotEmpty(),
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