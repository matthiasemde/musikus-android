/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2025 Michael Prommersberger, Matthias Emde
 */
package app.musikus.activesession.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.musikus.R
import app.musikus.core.presentation.theme.spacing
import app.musikus.library.presentation.LibraryCoreUiEvent
import app.musikus.library.presentation.LibraryFoldersSwipeRow
import app.musikus.library.presentation.LibraryUiEvent
import app.musikus.library.presentation.LibraryUiEventHandler
import app.musikus.library.presentation.libraryItemsComponent
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import java.util.UUID


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewItemSelectorBottomSheet(
    uiState: State<NewItemSelectorUiState?>,
    sheetState: SheetState,
    eventHandler: LibraryUiEventHandler,
    onDismissed: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        contentWindowInsets = { WindowInsets(top = 0.dp) }, // makes sure the scrim covers the status bar
        onDismissRequest = remember { onDismissed },
        sheetState = sheetState,
        shape = RectangleShape,
        dragHandle = {},
    ) {
        NewItemSelectorLayout(
            modifier = Modifier
                .fillMaxHeight(0.6f), // avoid jumping height when changing folders
            uiState = uiState,
            eventHandler = eventHandler,
            onClose = remember {
                {
                    scope.launch {
                        sheetState.hide()
                        onDismissed()
                    }
                }
            },
        )
    }
}


@Composable
private fun NewItemSelectorLayout(
    uiState: State<NewItemSelectorUiState?>,
    modifier: Modifier = Modifier,
    eventHandler: LibraryUiEventHandler,
    onClose: () -> Unit = {},
) {
    val _uiState = uiState.value ?: return // unpacking & null check

    var selectedFolder: UUID? by remember { mutableStateOf(_uiState.runningItem?.libraryFolderId) }

    // selectedFolder has to be tied to ViewModel state to be consistent with displayed items
    LaunchedEffect(selectedFolder) {
        eventHandler(LibraryUiEvent.FolderPressed(selectedFolder, longClick = false))
    }

    var createMenuShown by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        // Header + Close Button
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
            Text(
                text = stringResource(id = R.string.active_session_new_item_selector_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.weight(1f))

            /* TODO implement Creating Folders + Items
            IconButton(
                onClick = { createMenuShown = true },
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small)
            ) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)

                DropdownMenu(
                    expanded = createMenuShown,
                    onDismissRequest = { createMenuShown = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {

                    DropdownMenuItem(
                        onClick = onNewItem,
                        text = { Text(text = stringResource(id = R.string.active_session_new_item_selector_create_item) }
                    )
                    DropdownMenuItem(
                        onClick = onNewFolder,
                        text = { Text(text = stringResource(id = R.string.active_session_new_item_selector_create_folder) }
                    )
                }
            }
             */

            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null)
            }
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        // Folders
        val folders = _uiState.libraryFoldersUiState.foldersWithItems.toImmutableList()
        if (folders.isNotEmpty()) {
            LibraryFoldersSwipeRow(
                folders = folders,
                highlightedFolderId = selectedFolder,
                showBadge = _uiState.runningItem != null,
                folderWithBadge = _uiState.runningItem?.libraryFolderId,
                onFolderSelected = remember {
                    {
                            folderId ->
                        selectedFolder = folderId
                    }
                }
            )
        }

        // use own divider to avoid padding of default one from TabRow
        // and also to show it when folders are not shown
        HorizontalDivider()

        /** Items */
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = WindowInsets(
                top = MaterialTheme.spacing.small,
            ).add(WindowInsets.navigationBars).asPaddingValues() // don't get covered by navbars
        ) {
            libraryItemsComponent(
                uiState = _uiState.libraryItemsUiState,
                libraryCoreEventHandler = {
                    if (it is LibraryCoreUiEvent.ItemPressed && it.longClick == false) {
                        onClose() // close bottom sheet when an item is selected
                    }
                    eventHandler( LibraryUiEvent.CoreUiEvent(it) )
               },
            )
        }

    }
}
