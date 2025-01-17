/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2025 Matthias Emde
 */

package app.musikus.library.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.R
import app.musikus.core.presentation.MainUiEvent
import app.musikus.core.presentation.MainUiEventHandler
import app.musikus.core.presentation.MainUiState
import app.musikus.core.presentation.MusikusTopBar
import app.musikus.core.presentation.Screen
import app.musikus.core.presentation.components.ActionBar
import app.musikus.core.presentation.components.MiniFABData
import app.musikus.core.presentation.components.MultiFAB
import app.musikus.core.presentation.components.MultiFabState
import app.musikus.core.presentation.components.SortMenu
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.ObserveAsEvents
import app.musikus.core.presentation.utils.UiText
import app.musikus.library.data.LibraryFolderSortMode
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Library(
    mainUiState: MainUiState,
    mainEventHandler: MainUiEventHandler,
    navigateToFolderDetails: (Screen.LibraryFolderDetails) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
    bottomBarHeight: Dp,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val eventHandler = viewModel::onUiEvent

    BackHandler(
        enabled = uiState.actionModeUiState.isActionMode,
        onBack = { eventHandler(LibraryUiEvent.CoreUiEvent(LibraryCoreUiEvent.ClearActionMode)) }
    )

    BackHandler(
        enabled = mainUiState.multiFabState == MultiFabState.EXPANDED,
        onBack = { mainEventHandler(MainUiEvent.CollapseMultiFab) }
    )

    val foldersListState = rememberLazyListState()
    val itemsListState = rememberLazyListState()

    /**
     * Collect and handle events from the view model
     */
    ObserveAsEvents(viewModel.eventChannel) { event ->
        when (event) {
            is LibraryCoreEvent.ScrollToFolder -> {
                // The short delay improves the feel of the animation.
                delay(300.milliseconds)
                foldersListState.animateScrollToItem(
                    index = event.folderIndex,
                    scrollOffset = -150 // small scroll offset to make sure the item above is partially visible
                )
            }
            is LibraryCoreEvent.ScrollToItem -> {
                // The short delay improves the feel of the animation.
                delay(300.milliseconds)
                itemsListState.animateScrollToItem(
                    index = event.itemIndex,
                    scrollOffset = -150 // small scroll offset to make sure the item above is partially visible
                )
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(bottom = bottomBarHeight), // makes sure FAB is above the bottom Bar
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        floatingActionButton = {
            MultiFAB(
                state = mainUiState.multiFabState,
                onStateChange = { newState ->
                    if (newState == MultiFabState.EXPANDED) {
                        mainEventHandler(MainUiEvent.ExpandMultiFab)
                        eventHandler(LibraryUiEvent.CoreUiEvent(LibraryCoreUiEvent.ClearActionMode))
                    } else {
                        mainEventHandler(MainUiEvent.CollapseMultiFab)
                    }
                },
                contentDescription = stringResource(id = R.string.library_screen_multi_fab_description),
                miniFABs = listOf(
                    MiniFABData(
                        onClick = {
                            eventHandler(LibraryUiEvent.CoreUiEvent(LibraryCoreUiEvent.AddItemButtonPressed))
                            mainEventHandler(MainUiEvent.CollapseMultiFab)
                        },
                        label = stringResource(id = R.string.library_screen_multi_fab_item_description),
                        icon = Icons.Rounded.MusicNote
                    ),
                    MiniFABData(
                        onClick = {
                            eventHandler(LibraryUiEvent.AddFolderButtonPressed)
                            mainEventHandler(MainUiEvent.CollapseMultiFab)
                        },
                        label = stringResource(id = R.string.library_screen_multi_fab_folder_description),
                        icon = Icons.Rounded.Folder
                    )
                )
            )
        },
        topBar = {
            // Main top bar
            MusikusTopBar(
                isTopLevel = true,
                title = UiText.StringResource(R.string.library_title),
                scrollBehavior = scrollBehavior,
                openMainMenu = { mainEventHandler(MainUiEvent.OpenMainMenu) }
            )

            // Action bar
            val actionModeUiState = uiState.actionModeUiState
            if (actionModeUiState.isActionMode) {
                ActionBar(
                    numSelectedItems = actionModeUiState.numberOfSelections,
                    onDismissHandler = { eventHandler(LibraryUiEvent.CoreUiEvent(LibraryCoreUiEvent.ClearActionMode)) },
                    onEditHandler = { eventHandler(LibraryUiEvent.CoreUiEvent(LibraryCoreUiEvent.EditButtonPressed)) },
                    onDeleteHandler = {
                        eventHandler(LibraryUiEvent.CoreUiEvent(LibraryCoreUiEvent.DeleteButtonPressed))
                    }
                )
            }
        },
        content = { paddingValues ->
            val contentUiState = uiState.contentUiState

            LibraryContent(
                contentPadding = paddingValues,
                contentUiState = contentUiState,
                navigateToFolderDetails = navigateToFolderDetails,
                eventHandler = eventHandler
            )

            // Show hint if no items or folders are in the library
            if (contentUiState.showHint) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(MaterialTheme.spacing.extraLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.library_screen_hint),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Dialogs
            LibraryDialogs(
                uiState = uiState.dialogsUiState,
                libraryCoreEventHandler = { eventHandler(LibraryUiEvent.CoreUiEvent(it)) },
                mainEventHandler = mainEventHandler
            )

            // Content Scrim for multiFAB
            AnimatedVisibility(
                modifier = Modifier
                    .zIndex(1f),
                visible = mainUiState.multiFabState == MultiFabState.EXPANDED,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorScheme.surface.copy(alpha = 0.9f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { mainEventHandler(MainUiEvent.CollapseMultiFab) }
                        )
                )
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryContent(
    contentPadding: PaddingValues,
    contentUiState: LibraryContentUiState,
    navigateToFolderDetails: (Screen.LibraryFolderDetails) -> Unit,
    eventHandler: LibraryUiEventHandler,
    foldersListState: LazyListState,
    itemsListState: LazyListState,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 56.dp,
        ),
    ) {
        val foldersUiState = contentUiState.foldersUiState
        val itemsUiState = contentUiState.itemsUiState

        /** Folders */
        if (foldersUiState != null) {
            /** Header (with sort menu) */
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = MaterialTheme.spacing.small,
                            horizontal = MaterialTheme.spacing.large
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.library_content_folders_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    val sortMenuUiState = foldersUiState.sortMenuUiState
                    SortMenu(
                        show = sortMenuUiState.show,
                        sortModes = LibraryFolderSortMode.entries,
                        currentSortMode = sortMenuUiState.mode,
                        currentSortDirection = sortMenuUiState.direction,
                        sortItemDescription = stringResource(
                            id = R.string.library_content_folders_sort_menu_description
                        ),
                        onShowMenuChanged = { eventHandler(LibraryUiEvent.FolderSortMenuPressed) },
                        onSelectionHandler = {
                            eventHandler(LibraryUiEvent.FolderSortModeSelected(it as LibraryFolderSortMode))
                        }
                    )
                }
            }

            /** Folders */
            item {
                LazyRow(
                    state = foldersListState,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // header and footer items replace contentPadding
                    // but also serve to fixate the list when inserting items
                    item {
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                    }
                    items(
                        items = foldersUiState.foldersWithItems,
                        key = { it.folder.id }
                    ) { folderWithItems ->
                        val folder = folderWithItems.folder
                        Row(modifier = Modifier.animateItem()) {
                            LibraryFolderComponent(
                                folder = folder,
                                numItems = folderWithItems.items.size,
                                selected = folder.id in foldersUiState.selectedFolderIds,
                                onShortClick = {
                                    if (!eventHandler(LibraryUiEvent.FolderPressed(folder, longClick = false))) {
                                        navigateToFolderDetails(Screen.LibraryFolderDetails(folder.id.toString()))
                                    }
                                },
                                onLongClick = {
                                    eventHandler(LibraryUiEvent.FolderPressed(folder, longClick = true))
                                }
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                    }
                }
            }
        }

        /** Items */
        if (itemsUiState != null) {
            libraryItemsComponent(
                uiState = itemsUiState,
                libraryCoreEventHandler = { eventHandler(LibraryUiEvent.CoreUiEvent(it)) },
            )
        }
    }
}
