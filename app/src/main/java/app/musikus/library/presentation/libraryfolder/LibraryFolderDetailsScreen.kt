/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.library.presentation.libraryfolder

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.R
import app.musikus.core.presentation.MainUiEvent
import app.musikus.core.presentation.MainUiEventHandler
import app.musikus.core.presentation.MusikusTopBar
import app.musikus.core.presentation.components.ActionBar
import app.musikus.core.presentation.components.SortMenu
import app.musikus.library.data.LibraryItemSortMode
import app.musikus.library.presentation.LibraryCoreUiEvent
import app.musikus.library.presentation.LibraryDialogs
import app.musikus.library.presentation.libraryItemsComponent
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryFolderDetailsScreen(
    mainEventHandler: MainUiEventHandler,
    viewModel: LibraryFolderDetailsViewModel = hiltViewModel(),
    folderId: UUID,
    navigateUp: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val eventHandler = viewModel::onUiEvent

    LaunchedEffect(folderId) {
        viewModel.setActiveFolder(folderId)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // Main top bar
            MusikusTopBar(
                isTopLevel = false,
                title = uiState.folderName,
                scrollBehavior = scrollBehavior,
                navigateUp = navigateUp,
                actions = {
                    val sortMenuUiState = uiState.itemsSortMenuUiState

                    SortMenu(
                        show = sortMenuUiState.show,
                        sortModes = LibraryItemSortMode.entries,
                        currentSortMode = sortMenuUiState.mode,
                        currentSortDirection = sortMenuUiState.direction,
                        sortItemDescription = stringResource(id = R.string.library_content_items_sort_menu_description),
                        onShowMenuChanged = { eventHandler(LibraryFolderDetailsUiEvent.CoreUiEvent(
                            LibraryCoreUiEvent.ItemSortMenuPressed)) },
                        onSelectionHandler = {
                            eventHandler(LibraryFolderDetailsUiEvent.CoreUiEvent(LibraryCoreUiEvent.ItemSortModeSelected(it as LibraryItemSortMode)))
                        }
                    )

                    IconButton(onClick = {
                        eventHandler(
                            LibraryFolderDetailsUiEvent.CoreUiEvent(
                                LibraryCoreUiEvent.EditButtonPressed
                            )
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(id = R.string.components_action_bar_edit_button_description)
                        )
                    }
                }
            )

            // Action bar
            val actionModeUiState = uiState.actionModeUiState
            if (actionModeUiState.isActionMode) {
                ActionBar(
                    numSelectedItems = actionModeUiState.numberOfSelections,
                    onDismissHandler = { eventHandler(LibraryFolderDetailsUiEvent.CoreUiEvent(LibraryCoreUiEvent.ClearActionMode)) },
                    onEditHandler = { eventHandler(LibraryFolderDetailsUiEvent.CoreUiEvent(LibraryCoreUiEvent.EditButtonPressed)) },
                    onDeleteHandler = {
                        eventHandler(LibraryFolderDetailsUiEvent.CoreUiEvent(LibraryCoreUiEvent.DeleteButtonPressed))
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    eventHandler(LibraryFolderDetailsUiEvent.CoreUiEvent(LibraryCoreUiEvent.AddItemButtonPressed))
                    mainEventHandler(MainUiEvent.CollapseMultiFab)
                },
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.library_screen_fab_description)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 56.dp,
            ),
        ) {
            val itemsUiState = uiState.itemsUiState

            /** Items */
            if (itemsUiState != null) {
                libraryItemsComponent(
                    uiState = itemsUiState,
                    libraryCoreEventHandler = { eventHandler(LibraryFolderDetailsUiEvent.CoreUiEvent(it)) },
                    showHeader = false
                )
            }
        }

        // Dialogs
        LibraryDialogs(
            uiState = uiState.dialogsUiState,
            libraryCoreEventHandler = { eventHandler(LibraryFolderDetailsUiEvent.CoreUiEvent(it)) },
            mainEventHandler = mainEventHandler
        )
    }
}
