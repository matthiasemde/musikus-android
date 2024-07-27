/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde
 */

package app.musikus.library.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.R
import app.musikus.core.presentation.HomeUiEvent
import app.musikus.core.presentation.HomeUiEventHandler
import app.musikus.core.presentation.HomeUiState
import app.musikus.core.presentation.MainUiEvent
import app.musikus.core.presentation.MainUiEventHandler
import app.musikus.core.presentation.Screen
import app.musikus.core.presentation.components.ActionBar
import app.musikus.core.presentation.components.CommonMenuSelections
import app.musikus.core.presentation.components.DeleteConfirmationBottomSheet
import app.musikus.core.presentation.components.MainMenu
import app.musikus.core.presentation.components.MiniFABData
import app.musikus.core.presentation.components.MultiFAB
import app.musikus.core.presentation.components.MultiFabState
import app.musikus.core.presentation.components.Selectable
import app.musikus.core.presentation.components.SortMenu
import app.musikus.core.presentation.theme.MusikusColorSchemeProvider
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.core.presentation.theme.libraryItemColors
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.data.UUIDConverter
import app.musikus.library.data.daos.LibraryFolder
import app.musikus.library.data.daos.LibraryItem
import app.musikus.settings.domain.ColorSchemeSelections
import app.musikus.core.domain.DateFormat
import app.musikus.core.domain.LibraryFolderSortMode
import app.musikus.core.domain.LibraryItemSortMode
import app.musikus.core.presentation.utils.UiIcon
import app.musikus.core.presentation.utils.UiText
import app.musikus.core.domain.musikusFormat
import java.time.ZonedDateTime

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
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        floatingActionButton = {
            val fabUiState = uiState.fabUiState
            if(fabUiState.activeFolder != null) {
                FloatingActionButton(
                    onClick = {
                        eventHandler(LibraryUiEvent.AddItemButtonPressed)
                        homeEventHandler(HomeUiEvent.CollapseMultiFab)
                    },
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.library_screen_fab_description))
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
                    contentDescription = stringResource(id = R.string.library_screen_multi_fab_description),
                    miniFABs = listOf(
                        MiniFABData(
                            onClick = {
                                eventHandler(LibraryUiEvent.AddItemButtonPressed)
                                homeEventHandler(HomeUiEvent.CollapseMultiFab)
                            },
                            label = stringResource(id = R.string.library_screen_multi_fab_item_description),
                            icon = Icons.Rounded.MusicNote
                        ),
                        MiniFABData(
                            onClick = {
                                eventHandler(LibraryUiEvent.AddFolderButtonPressed)
                                homeEventHandler(HomeUiEvent.CollapseMultiFab)
                            },
                            label = stringResource(id = R.string.library_screen_multi_fab_folder_description),
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
                title = { Text(text = topBarUiState.title.asString()) },
                navigationIcon = {
                    if(topBarUiState.showBackButton) {
                        IconButton(onClick = { eventHandler(LibraryUiEvent.BackButtonPressed) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(
                                    id = R.string.components_top_bar_back_description
                                )
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        homeEventHandler(HomeUiEvent.ShowMainMenu)
                    }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(id = R.string.core_kebab_menu_description)
                        )
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
                        text = stringResource(id = R.string.library_screen_hint),
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
                val snackbarMessage = stringResource(id = R.string.library_screen_snackbar_deleted)

                val foldersSelected = deleteDialogUiState.numberOfSelectedFolders > 0
                val itemsSelected = deleteDialogUiState.numberOfSelectedItems > 0

                val totalSelections =
                    deleteDialogUiState.numberOfSelectedFolders + deleteDialogUiState.numberOfSelectedItems

                DeleteConfirmationBottomSheet(
                    explanation = 
                        if (foldersSelected && itemsSelected) {
                            UiText.StringResource(
                                R.string.library_screen_deletion_dialog_explanation_both,
                                deleteDialogUiState.numberOfSelectedFolders,
                                pluralStringResource(id = R.plurals.library_folder, deleteDialogUiState.numberOfSelectedFolders),
                                deleteDialogUiState.numberOfSelectedItems,
                                pluralStringResource(id = R.plurals.library_item, deleteDialogUiState.numberOfSelectedItems)
                            )
                        } else {
                            UiText.PluralResource(
                                R.plurals.library_screen_deletion_dialog_explanation,
                                totalSelections,
                                totalSelections,
                                if (foldersSelected) {
                                        pluralStringResource(id = R.plurals.library_folder, deleteDialogUiState.numberOfSelectedFolders)
                                } else {
                                        pluralStringResource(id = R.plurals.library_item, deleteDialogUiState.numberOfSelectedItems)
                                }
                            )
                        },
                    confirmationIcon = UiIcon.DynamicIcon(Icons.Default.Delete),
                    confirmationText = UiText.StringResource(
                        R.string.library_screen_deletion_dialog_confirm,
                        totalSelections
                    ),
                    onDismiss = { eventHandler(LibraryUiEvent.DeleteDialogDismissed) },
                    onConfirm = {
                        eventHandler(LibraryUiEvent.DeleteDialogConfirmed)
                        mainEventHandler(
                            MainUiEvent.ShowSnackbar(
                                message = snackbarMessage,
                                onUndo = { eventHandler(LibraryUiEvent.RestoreButtonPressed) }
                            )
                        )
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
                        text = stringResource(id = R.string.library_content_folders_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    val sortMenuUiState = foldersUiState.sortMenuUiState
                    SortMenu(
                        show = sortMenuUiState.show,
                        sortModes = LibraryFolderSortMode.entries,
                        currentSortMode = sortMenuUiState.mode,
                        currentSortDirection = sortMenuUiState.direction,
                        sortItemDescription = stringResource(id = R.string.library_content_folders_sort_menu_description),
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
                        text = stringResource(id = R.string.library_content_items_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    val sortMenuUiState = itemsUiState.sortMenuUiState
                    SortMenu(
                        show = sortMenuUiState.show,
                        sortModes = LibraryItemSortMode.entries,
                        currentSortMode = sortMenuUiState.mode,
                        currentSortDirection = sortMenuUiState.direction,
                        sortItemDescription = stringResource(id = R.string.library_content_items_sort_menu_description),
                        onShowMenuChanged = { eventHandler(LibraryUiEvent.ItemSortMenuPressed) },
                        onSelectionHandler = {
                            eventHandler(LibraryUiEvent.ItemSortModeSelected(it as LibraryItemSortMode))
                        }
                    )
                }
            }
            items(
                items=itemsUiState.itemsWithLastPracticedDate,
                key = { (item, _) -> item.id }
            ) { (item, lastPracticedDate) ->
                Box(
                    modifier = Modifier.animateItemPlacement()
                ) {
                    LibraryUiItem(
                        item = item,
                        lastPracticedDate = lastPracticedDate,
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
                    text = stringResource(id = R.string.library_content_folders_sub_title, numItems),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryUiItem(
    modifier: Modifier = Modifier,
    item: LibraryItem,
    lastPracticedDate: ZonedDateTime?,
    selected: Boolean,
    onShortClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
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
                .padding(vertical = MaterialTheme.spacing.small)
                .fillMaxWidth()
                .alpha(if (!enabled) 0.5f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraLarge))
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .align(Alignment.CenterVertically)
                    .background(libraryItemColors[item.colorIndex])
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
            Column {
                Text(
                    modifier = Modifier.basicMarquee(),
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(
                        id = R.string.library_content_items_last_practiced,
                        lastPracticedDate?.musikusFormat(DateFormat.DAY_MONTH_YEAR) ?: stringResource(
                            id = R.string.library_content_items_last_practiced_never
                        )
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewLibraryItem(
    @PreviewParameter(MusikusColorSchemeProvider::class) colorScheme: ColorSchemeSelections
) {
    MusikusThemedPreview (colorScheme) {
        LibraryUiItem(
            item = LibraryItem(
                id = UUIDConverter.deadBeef,
                name = "Item 1",
                colorIndex = 0,
                createdAt = ZonedDateTime.now(),
                modifiedAt = ZonedDateTime.now(),
                libraryFolderId = null,
                customOrder = null
            ),
            lastPracticedDate = ZonedDateTime.now(),
            selected = false,
            onShortClick = {},
            onLongClick = {},
        )
    }
}