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

package de.practicetime.practicetime.ui.library

import android.util.Log
import android.view.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.LibraryFolder
import de.practicetime.practicetime.database.entities.LibraryItem
import de.practicetime.practicetime.datastore.LibraryFolderSortMode
import de.practicetime.practicetime.datastore.LibraryItemSortMode
import de.practicetime.practicetime.datastore.SortDirection
import de.practicetime.practicetime.datastore.ThemeSelections
import de.practicetime.practicetime.shared.*
import de.practicetime.practicetime.viewmodel.DialogMode
import de.practicetime.practicetime.viewmodel.LibraryMenuSelections
import de.practicetime.practicetime.viewmodel.LibraryViewModel
import de.practicetime.practicetime.viewmodel.MainViewModel
import java.util.*


//
//    // catch the back press for the case where the selection should be reverted
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
//                if(selectedItems.isNotEmpty()){
//                    actionMode?.finish()
//                }else{
//                    isEnabled = false
//                    activity?.onBackPressed()
//                }
//            }
//        })
//    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Library(mainViewModel: MainViewModel) {
    Log.d("Library", "${LocalViewModelStoreOwner.current}")
    val libraryViewModel = viewModel<LibraryViewModel>()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        contentWindowInsets = WindowInsets(bottom = 0.dp), // makes sure FAB is not shifted up
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        floatingActionButton = {
            libraryViewModel.activeFolder.value?.let { folder ->
                FloatingActionButton(
                    onClick = {
                        libraryViewModel.itemDialogMode.value = DialogMode.ADD
                        libraryViewModel.itemDialogFolderId.value = folder.id
                        libraryViewModel.showItemDialog.value = true
                        libraryViewModel.multiFABState.value = MultiFABState.COLLAPSED
                        mainViewModel.showNavBarScrim.value = false
                    },
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New item")
                }
            } ?: MultiFAB(
                state = libraryViewModel.multiFABState.value,
                onStateChange = { state ->
                    libraryViewModel.multiFABState.value = state
                    mainViewModel.showNavBarScrim.value = (state == MultiFABState.EXPANDED)
                    if(state == MultiFABState.EXPANDED) {
                        libraryViewModel.clearActionMode()
                    }
                },
                miniFABs = listOf(
                    MiniFABData(
                        onClick = {
                            libraryViewModel.itemDialogMode.value = DialogMode.ADD
                            libraryViewModel.showItemDialog.value = true
                            libraryViewModel.multiFABState.value = MultiFABState.COLLAPSED
                            mainViewModel.showNavBarScrim.value = false
                        },
                        label = "Item",
                        icon = Icons.Rounded.MusicNote
                    ),
                    MiniFABData(
                        onClick = {
                            libraryViewModel.folderDialogMode.value = DialogMode.ADD
                            libraryViewModel.showFolderDialog.value = true
                            libraryViewModel.multiFABState.value = MultiFABState.COLLAPSED
                            mainViewModel.showNavBarScrim.value = false
                        },
                        label = "Folder",
                        icon = Icons.Rounded.Folder
                    )
                )
            )
        },
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(text = libraryViewModel.activeFolder.value?.name ?: "Library") },
                navigationIcon = {
                    if(libraryViewModel.activeFolder.value != null){
                        IconButton(onClick = {
                            libraryViewModel.activeFolder.value = null
                        }) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        mainViewModel.showMainMenu.value = true
                    }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "more")
                        MainMenu (
                            show = mainViewModel.showMainMenu.value,
                            onDismissHandler = { mainViewModel.showMainMenu.value = false },
                            onSelectionHandler = { commonSelection ->
                                mainViewModel.showMainMenu.value = false

                                when (commonSelection) {
                                    CommonMenuSelections.APP_INFO -> {}
                                    CommonMenuSelections.THEME -> {
                                        mainViewModel.showThemeSubMenu.value = true
                                    }
                                    CommonMenuSelections.BACKUP -> {
                                        mainViewModel.showExportImportDialog.value = true
                                    }
                                }
                            },
                            uniqueMenuItems = { LibraryMenuItems(
                                onSelectionHandler = { }
                            ) }
                        )
                        ThemeMenu(
                            expanded = mainViewModel.showThemeSubMenu.value,
                            currentTheme = mainViewModel.activeTheme.collectAsState(initial = ThemeSelections.DAY).value,
                            onDismissHandler = { mainViewModel.showThemeSubMenu.value = false },
                            onSelectionHandler = { theme ->
                                mainViewModel.showThemeSubMenu.value = false
                                mainViewModel.setTheme(theme)
                            }
                        )
                    }
                }
            )

            // Action bar

            if(libraryViewModel.actionMode.collectAsState().value) {
                ActionBar(
                    numSelectedItems =
                        libraryViewModel.selectedItems.collectAsState().value.size +
                        libraryViewModel.selectedFolders.collectAsState().value.size,
                    onDismissHandler = libraryViewModel::clearActionMode,
                    onEditHandler = libraryViewModel::onEditAction,
                    onDeleteHandler = libraryViewModel::onDeleteAction
                )
            }
        },
        content = { innerPadding ->
            LibraryContent(
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                ),
                activeFolder = libraryViewModel.activeFolder.value,
                showFolderSortMenu = libraryViewModel.showFolderSortModeMenu.value,
                folderSortMode = libraryViewModel.folderSortMode.collectAsState().value,
                folderSortDirection = libraryViewModel.folderSortDirection.collectAsState().value,
                folders = libraryViewModel.sortedFolders.collectAsState().value,
                selectedFolders = libraryViewModel.selectedFolders.collectAsState().value,
                showItemSortMenu = libraryViewModel.showItemSortModeMenu.value,
                itemSortMode = libraryViewModel.itemSortMode.collectAsState().value,
                itemSortDirection = libraryViewModel.itemSortDirection.collectAsState().value,
                items = libraryViewModel.sortedItems.collectAsState().value,
                selectedItems = libraryViewModel.selectedItems.collectAsState().value,
                onShowFolderSortMenuChange = { libraryViewModel.showFolderSortModeMenu.value = it },
                onFolderSortModeSelected = libraryViewModel::onFolderSortModeSelected,
                onShowItemSortMenuChange = { libraryViewModel.showItemSortModeMenu.value = it },
                onItemSortModeSelected = libraryViewModel::onItemSortModeSelected,
                onFolderClicked = libraryViewModel::onFolderClicked,
                onItemClicked = libraryViewModel::onItemClicked,
            )

            // Show hint if no items or folders are in the library
            if (libraryViewModel.showHint.collectAsState().value) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.libraryHint),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if(libraryViewModel.showFolderDialog.value) {
                LibraryFolderDialog(
                    mode = libraryViewModel.folderDialogMode.value,
                    folderName = libraryViewModel.folderDialogName.value,
                    onFolderNameChange = { libraryViewModel.folderDialogName.value = it },
                    onDismissHandler = { create ->
                        if(create) {
                            libraryViewModel.onFolderDialogConfirmed()
                        }
                        libraryViewModel.clearFolderDialog()
                    },
                )
            }

            if(libraryViewModel.showItemDialog.value) {
                LibraryItemDialog(
                    mode = libraryViewModel.itemDialogMode.value,
                    folders = libraryViewModel.sortedFolders.collectAsState().value,
                    name = libraryViewModel.itemDialogName.value,
                    colorIndex = libraryViewModel.itemDialogColorIndex.value,
                    folderId = libraryViewModel.itemDialogFolderId.value,
                    folderSelectorExpanded = libraryViewModel.itemDialogFolderSelectorExpanded.value,
                    onNameChange = { libraryViewModel.itemDialogName.value = it },
                    onColorIndexChange = { libraryViewModel.itemDialogColorIndex.value = it },
                    onFolderIdChange = {
                        libraryViewModel.itemDialogFolderId.value = it
                        libraryViewModel.itemDialogFolderSelectorExpanded.value = SpinnerState.COLLAPSED
                    },
                    onFolderSelectorExpandedChange = { libraryViewModel.itemDialogFolderSelectorExpanded.value = it },
                    onDismissHandler = { cancel ->
                        if(!cancel) {
                            libraryViewModel.onItemDialogConfirmed()
                        }
                        libraryViewModel.clearItemDialog()
                    },
                )
            }

            // Content Scrim for multiFAB
            AnimatedVisibility(
                modifier = Modifier
                    .zIndex(1f),
                visible = libraryViewModel.multiFABState.value == MultiFABState.EXPANDED,
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
                        ) {
                            libraryViewModel.multiFABState.value = MultiFABState.COLLAPSED
                            mainViewModel.showNavBarScrim.value = false
                        }
                )
            }
        }
    )
}

@Preview
@Composable
fun LibraryMenuItems(
    onSelectionHandler: (LibraryMenuSelections) -> Unit = {}
) { }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryContent(
    contentPadding: PaddingValues,
    activeFolder: LibraryFolder?,
    showFolderSortMenu: Boolean,
    folderSortMode: LibraryFolderSortMode,
    folderSortDirection: SortDirection,
    folders: List<LibraryFolder>,
    selectedFolders: Set<LibraryFolder>,
    showItemSortMenu: Boolean,
    itemSortMode: LibraryItemSortMode,
    itemSortDirection: SortDirection,
    items: List<LibraryItem>,
    selectedItems: Set<LibraryItem>,
    onShowFolderSortMenuChange: (Boolean) -> Unit,
    onFolderSortModeSelected: (LibraryFolderSortMode) -> Unit,
    onShowItemSortMenuChange: (Boolean) -> Unit,
    onItemSortModeSelected: (LibraryItemSortMode) -> Unit,
    onFolderClicked: (LibraryFolder, Boolean) -> Unit,
    onItemClicked: (LibraryItem, Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 56.dp,
        ),
    ) {
        // if active folder ist null, we are in the top level
        if(activeFolder == null && folders.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = "Folders", style = MaterialTheme.typography.titleLarge
                    )
                    SortMenu(
                        show = showFolderSortMenu,
                        sortModes = LibraryFolderSortMode.values().toList(),
                        currentSortMode = folderSortMode,
                        currentSortDirection = folderSortDirection,
                        label = { LibraryFolderSortMode.toString(it) },
                        onShowMenuChanged = onShowFolderSortMenuChange,
                        onSelectionHandler = onFolderSortModeSelected
                    )
                }
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // header and footer items replace contentPadding
                    // but also serve to fixate the list when inserting items
                    item { Spacer(modifier = Modifier.width(4.dp)) }
                    items(
                        items = folders,
                        key = { folder -> folder.id }
                    ) { folder ->
                        Row(
                            modifier = Modifier
                                .animateItemPlacement()
                        ) {
                            LibraryFolder(
                                folder = folder,
                                numItems = items.filter { it.libraryFolderId == folder.id }.size,
                                selected = folder in selectedFolders,
                                onShortClick = { onFolderClicked(folder, false) },
                                onLongClick = { onFolderClicked(folder, true) }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.width(4.dp)) }
                }
            }
        }
        val itemsInActiveFolder = items.filter { it.libraryFolderId == activeFolder?.id }
        if(itemsInActiveFolder.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .padding(8.dp),
                        text = "Items",
                        style = MaterialTheme.typography.titleLarge
                    )
                    SortMenu(
                        show = showItemSortMenu,
                        sortModes = LibraryItemSortMode.values().toList(),
                        currentSortMode = itemSortMode,
                        currentSortDirection = itemSortDirection,
                        label = { LibraryItemSortMode.toString(it) },
                        onShowMenuChanged = onShowItemSortMenuChange,
                        onSelectionHandler = onItemSortModeSelected
                    )
                }
            }
            items(
                items=itemsInActiveFolder,
                key = { item -> item.id }
            ) { item ->
                Box(
                    modifier = Modifier.animateItemPlacement()
                ) {
                    LibraryItem(
                        modifier = Modifier
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        libraryItem = item,
                        selected = item in selectedItems,
                        onShortClick = { onItemClicked(item, false) },
                        onLongClick = { onItemClicked(item, true) }
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
            color = colorScheme.surface,
            tonalElevation = 1.dp,
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
fun LibraryItem(
    modifier: Modifier,
    libraryItem: LibraryItem,
    selected: Boolean,
    onShortClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Selectable(
        selected = selected,
        onShortClick = onShortClick,
        onLongClick = onLongClick,
        shape = RoundedCornerShape(0.dp),
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .align(Alignment.CenterVertically)
                    .background(
                        Color(PracticeTime.getLibraryItemColors(LocalContext.current)[libraryItem.colorIndex])
                    ),
            )
            Column(
                modifier = Modifier
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = libraryItem.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "last practiced: yesterday",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
