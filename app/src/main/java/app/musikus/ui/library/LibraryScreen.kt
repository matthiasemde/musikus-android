/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package app.musikus.ui.library

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.R
import app.musikus.database.daos.LibraryFolder
import app.musikus.database.daos.LibraryItem
import app.musikus.ui.MainUiEvent
import app.musikus.ui.MainUiEventHandler
import app.musikus.ui.Screen
import app.musikus.ui.components.ActionBar
import app.musikus.ui.components.CommonMenuSelections
import app.musikus.ui.components.DeleteConfirmationBottomSheet
import app.musikus.ui.components.MainMenu
import app.musikus.ui.components.MiniFABData
import app.musikus.ui.components.MultiFAB
import app.musikus.ui.components.MultiFabState
import app.musikus.ui.components.Selectable
import app.musikus.ui.components.SortMenu
import app.musikus.ui.home.HomeUiEvent
import app.musikus.ui.home.HomeUiEventHandler
import app.musikus.ui.home.HomeUiState
import app.musikus.ui.theme.libraryItemColors
import app.musikus.ui.theme.spacing
import app.musikus.utils.LibraryFolderSortMode
import app.musikus.utils.LibraryItemSortMode
import app.musikus.utils.UiIcon
import app.musikus.utils.UiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Library(
    homeUiState: HomeUiState,
    viewModel: LibraryViewModel = hiltViewModel(),
    mainEventHandler: MainUiEventHandler,
    homeEventHandler: HomeUiEventHandler,
    navigateTo: (Screen) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val eventHandler = viewModel::onUiEvent

    BackHandler(
        enabled = uiState.topBarUiState.showBackButton,
        onBack = { eventHandler(LibraryUiEvent.BackButtonPressed) }
    )

    BackHandler(
        enabled = uiState.actionModeUiState.isActionMode,
        onBack = { eventHandler(LibraryUiEvent.ClearActionMode) }
    )

    BackHandler(
        enabled = homeUiState.multiFabState == MultiFabState.EXPANDED,
        onBack = { homeEventHandler(HomeUiEvent.CollapseMultiFab) }
    )

    Scaffold(
        contentWindowInsets = WindowInsets(bottom = 0.dp), // makes sure FAB is not shifted up
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        floatingActionButton = {
            val fabUiState = uiState.fabUiState
            if(fabUiState.activeFolder != null) {
                FloatingActionButton(
                    onClick = {
                        eventHandler(LibraryUiEvent.AddItemButtonPressed)
                        homeEventHandler(HomeUiEvent.CollapseMultiFab)
                    },
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add item")
                }
            } else {
                MultiFAB(
                    state = homeUiState.multiFabState,
                    onStateChange = { newState ->
                        if(newState == MultiFabState.EXPANDED) {
                            homeEventHandler(HomeUiEvent.ExpandMultiFab)
                            eventHandler(LibraryUiEvent.ClearActionMode)
                        } else {
                            homeEventHandler(HomeUiEvent.CollapseMultiFab)
                        }
                    },
                    contentDescription = "Add",
                    miniFABs = listOf(
                        MiniFABData(
                            onClick = {
                                eventHandler(LibraryUiEvent.AddItemButtonPressed)
                                homeEventHandler(HomeUiEvent.CollapseMultiFab)
                            },
                            label = "Item",
                            icon = Icons.Rounded.MusicNote
                        ),
                        MiniFABData(
                            onClick = {
                                eventHandler(LibraryUiEvent.AddFolderButtonPressed)
                                homeEventHandler(HomeUiEvent.CollapseMultiFab)
                            },
                            label = "Folder",
                            icon = Icons.Rounded.Folder
                        )
                    )
                )
            }
        },
        topBar = {
            val topBarUiState = uiState.topBarUiState
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(text = topBarUiState.title) },
                navigationIcon = {
                    if(topBarUiState.showBackButton) {
                        IconButton(onClick = { eventHandler(LibraryUiEvent.BackButtonPressed) }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        homeEventHandler(HomeUiEvent.ShowMainMenu)
                    }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "more")
                        MainMenu (
                            show = homeUiState.showMainMenu,
                            onDismiss = { homeEventHandler(HomeUiEvent.HideMainMenu) },
                            onSelection = { commonSelection ->
                                homeEventHandler(HomeUiEvent.HideMainMenu)

                                when (commonSelection) {
                                    CommonMenuSelections.SETTINGS -> { navigateTo(Screen.Settings) }
                                }
                            }
                        )
                    }
                }
            )

            // Action bar
            val actionModeUiState = uiState.actionModeUiState
            if(actionModeUiState.isActionMode) {
                ActionBar(
                    numSelectedItems = actionModeUiState.numberOfSelections,
                    onDismissHandler = { eventHandler(LibraryUiEvent.ClearActionMode) },
                    onEditHandler = { eventHandler(LibraryUiEvent.EditButtonPressed) },
                    onDeleteHandler = {
                        eventHandler(LibraryUiEvent.DeleteButtonPressed)
                    }
                )
            }
        },
        content = { paddingValues ->
            val contentUiState = uiState.contentUiState

            LibraryContent(
                contentPadding = paddingValues,
                contentUiState = contentUiState,
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
                        text = stringResource(id = R.string.libraryHint),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            val dialogUiState = uiState.dialogUiState

            val folderDialogUiState = dialogUiState.folderDialogUiState
            val itemDialogUiState = dialogUiState.itemDialogUiState

            if(folderDialogUiState != null) {
                LibraryFolderDialog(
                    uiState = folderDialogUiState,
                    eventHandler = eventHandler
                )
            }

            if(itemDialogUiState != null) {
                LibraryItemDialog(
                    uiState = itemDialogUiState,
                    eventHandler = { eventHandler(LibraryUiEvent.ItemDialogUiEvent(it)) }
                )
            }

            val deleteDialogUiState = dialogUiState.deleteDialogUiState

            if (deleteDialogUiState != null) {

                val foldersSelected = deleteDialogUiState.numberOfSelectedFolders > 0
                val itemsSelected = deleteDialogUiState.numberOfSelectedItems > 0

                val totalSelections =
                    deleteDialogUiState.numberOfSelectedFolders + deleteDialogUiState.numberOfSelectedItems

                DeleteConfirmationBottomSheet(
                    explanation = UiText.DynamicString(
                        "Delete " +
                                (if (foldersSelected) "folders" else "") +
                                (if (foldersSelected && itemsSelected) " and " else "") +
                                (if (itemsSelected) "items" else "") +
                                "? They will remain in your statistics, but you will no longer be able to practice them."
                    ),
                    confirmationIcon = UiIcon.DynamicIcon(Icons.Default.Delete),
                    confirmationText = UiText.DynamicString("Delete forever ($totalSelections)"),
                    onDismiss = { eventHandler(LibraryUiEvent.DeleteDialogDismissed) },
                    onConfirm = {
                        eventHandler(LibraryUiEvent.DeleteDialogConfirmed)
                        mainEventHandler(MainUiEvent.ShowSnackbar(
                            message = "Deleted",
                            onUndo = { eventHandler(LibraryUiEvent.RestoreButtonPressed) }
                        ))
                    }
                )
            }

            // Content Scrim for multiFAB
            AnimatedVisibility(
                modifier = Modifier
                    .zIndex(1f),
                visible = homeUiState.multiFabState == MultiFabState.EXPANDED,
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
                            onClick = { homeEventHandler(HomeUiEvent.CollapseMultiFab) }
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
    eventHandler: LibraryUiEventHandler
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
        if(foldersUiState != null) {
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
                        text = "Folders",
                        style = MaterialTheme.typography.titleLarge
                    )
                    val sortMenuUiState = foldersUiState.sortMenuUiState
                    SortMenu(
                        show = sortMenuUiState.show,
                        sortModes = LibraryFolderSortMode.entries,
                        currentSortMode = sortMenuUiState.mode,
                        currentSortDirection = sortMenuUiState.direction,
                        sortItemDescription = "folders",
                        onShowMenuChanged = { eventHandler(LibraryUiEvent.FolderSortMenuPressed) },
                        onSelectionHandler = {
                            eventHandler(LibraryUiEvent.FolderSortModeSelected(it as LibraryFolderSortMode))
                        }
                    )
                }
            }
            item {
                LazyRow(
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
                        Row(
                            modifier = Modifier
                                .animateItemPlacement()
                        ) {
                            LibraryFolder(
                                folder = folder,
                                numItems = folderWithItems.items.size,
                                selected = folder.id in foldersUiState.selectedFolderIds,
                                onShortClick = {
                                    eventHandler(LibraryUiEvent.FolderPressed(folder, longClick = false))
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
        if(itemsUiState != null) {
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
                        text = "Items",
                        style = MaterialTheme.typography.titleLarge
                    )
                    val sortMenuUiState = itemsUiState.sortMenuUiState
                    SortMenu(
                        show = sortMenuUiState.show,
                        sortModes = LibraryItemSortMode.entries,
                        currentSortMode = sortMenuUiState.mode,
                        currentSortDirection = sortMenuUiState.direction,
                        sortItemDescription = "items",
                        onShowMenuChanged = { eventHandler(LibraryUiEvent.ItemSortMenuPressed) },
                        onSelectionHandler = {
                            eventHandler(LibraryUiEvent.ItemSortModeSelected(it as LibraryItemSortMode))
                        }
                    )
                }
            }
            items(
                items=itemsUiState.items,
                key = { item -> item.id }
            ) { item ->
                Box(
                    modifier = Modifier.animateItemPlacement()
                ) {
                    LibraryUiItem(
                        modifier = Modifier.padding(
                            vertical = MaterialTheme.spacing.small,
                            horizontal = MaterialTheme.spacing.large
                        ),
                        item = item,
                        selected = item.id in itemsUiState.selectedItemIds,
                        onShortClick = { eventHandler(LibraryUiEvent.ItemPressed(item, longClick = false)) },
                        onLongClick = { eventHandler(LibraryUiEvent.ItemPressed(item, longClick = true)) }
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryFolder(
    folder: LibraryFolder,
    numItems: Int,
    selected: Boolean,
    onShortClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Selectable(
        selected = selected,
        onShortClick = onShortClick,
        onLongClick = onLongClick,
        shape = MaterialTheme.shapes.large,
    ) {
        Surface(
            modifier = Modifier
                .size(150.dp),
            color = colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = "$numItems items",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun LibraryUiItem(
    modifier: Modifier = Modifier,
    item: LibraryItem,
    selected: Boolean,
    onShortClick: () -> Unit,
    onLongClick: () -> Unit,
    compact: Boolean = false,
    enabled: Boolean = true,
) {
    Selectable(
        selected = selected,
        onShortClick = onShortClick,
        onLongClick = onLongClick,
        enabled = enabled,
        shape = RoundedCornerShape(0.dp),
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .alpha(if (!enabled) 0.5f else 1f),
        ) {
            Box(
                modifier = Modifier
                    .width(if (compact) 8.dp else 10.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .align(Alignment.CenterVertically)
                    .background(libraryItemColors[item.colorIndex])
            )
            Column(
                modifier = Modifier
                    .padding(start = MaterialTheme.spacing.small),
            ) {
                Text(
                    text = item.name,
                    style = if (compact) {
                        MaterialTheme.typography.titleSmall
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                )
                Text(
                    text = "last practiced: yesterday",
                    style = if(compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}