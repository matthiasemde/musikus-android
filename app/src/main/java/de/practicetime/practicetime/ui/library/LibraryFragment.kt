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

import android.view.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.LibraryFolder
import de.practicetime.practicetime.database.entities.LibraryItem
import de.practicetime.practicetime.shared.*
import de.practicetime.practicetime.ui.MainState
import de.practicetime.practicetime.ui.SortDirection


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
fun Library(mainState: MainState) {
    val libraryState = rememberLibraryState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        contentWindowInsets = WindowInsets(bottom = 0.dp), // makes sure FAB is not shifted up
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        floatingActionButton = {
            libraryState.activeFolder.value?.let { folder ->
                FloatingActionButton(
                    onClick = {
                        libraryState.itemDialogMode.value = DialogMode.ADD
                        libraryState.itemDialogFolderId.value = folder.id
                        libraryState.showItemDialog.value = true
                        libraryState.multiFABState.value = MultiFABState.COLLAPSED
                        mainState.showNavBarScrim.value = false
                    },
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New item")
                }
            } ?: MultiFAB(
                state = libraryState.multiFABState.value,
                onStateChange = { state ->
                    libraryState.multiFABState.value = state
                    mainState.showNavBarScrim.value = (state == MultiFABState.EXPANDED)
                    if(state == MultiFABState.EXPANDED) {
                        libraryState.clearActionMode()
                    }
                },
                miniFABs = listOf(
                    MiniFABData(
                        onClick = {
                            libraryState.itemDialogMode.value = DialogMode.ADD
                            libraryState.showItemDialog.value = true
                            libraryState.multiFABState.value = MultiFABState.COLLAPSED
                            mainState.showNavBarScrim.value = false
                        },
                        label = "Item",
                        icon = Icons.Rounded.MusicNote
                    ),
                    MiniFABData(
                        onClick = {
                            libraryState.folderDialogMode.value = DialogMode.ADD
                            libraryState.showFolderDialog.value = true
                            libraryState.multiFABState.value = MultiFABState.COLLAPSED
                            mainState.showNavBarScrim.value = false
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
                title = { Text(text = libraryState.activeFolder.value?.name ?: "Library") },
                navigationIcon = {
                    if(libraryState.activeFolder.value != null){
                        IconButton(onClick = {
                            libraryState.activeFolder.value = null
                        }) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        mainState.showMainMenu.value = true
                    }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "more")
                        MainMenu (
                            show = mainState.showMainMenu.value,
                            onDismissHandler = { mainState.showMainMenu.value = false },
                            onSelectionHandler = { commonSelection ->
                                mainState.showMainMenu.value = false

                                when (commonSelection) {
                                    CommonMenuSelections.APP_INFO -> {}
                                    CommonMenuSelections.THEME -> {
                                        mainState.showThemeSubMenu.value = true
                                    }
                                }
                            },
                            uniqueMenuItems = { LibraryMenuItems(
                                onSelectionHandler = { librarySelection ->
                                    when (librarySelection) {
                                        LibraryMenuSelections.SORT_BY -> {
                                            libraryState.showItemSortModeMenu.value = true
                                        }
                                    }
                                }
                            ) }
                        )
                        ThemeMenu(
                            expanded = mainState.showThemeSubMenu.value,
                            currentTheme = mainState.activeTheme.value,
                            onDismissHandler = { mainState.showThemeSubMenu.value = false },
                            onSelectionHandler = { theme ->
                                mainState.showThemeSubMenu.value = false
                                mainState.setTheme(theme)
                            }
                        )
                    }
                }
            )

            // Action bar

            if(libraryState.actionMode.value) {
                ActionBar(
                    numSelectedItems =
                        libraryState.selectedItemIds.size +
                        libraryState.selectedFolderIds.size,
                    onDismissHandler = {
                        libraryState.clearActionMode()
                    },
                    onEditHandler = {
                        libraryState.apply {
                            mainState.libraryItems.value.firstOrNull { item ->
                                selectedItemIds.firstOrNull()?.let { it == item.id } ?: false
                            }?.let { item ->
                                editableItem.value = item
                                itemDialogMode.value = DialogMode.EDIT
                                itemDialogName.value = item.name
                                itemDialogColorIndex.value = item.colorIndex
                                itemDialogFolderId.value = item.libraryFolderId
                                showItemDialog.value = true
                            } ?: mainState.libraryFolders.value.firstOrNull { folder ->
                                selectedFolderIds.firstOrNull()?.let { it == folder.id } ?: false
                            }?.let { folder ->
                                editableFolder.value = folder
                                folderDialogMode.value = DialogMode.EDIT
                                folderDialogName.value = folder.name
                                showFolderDialog.value = true
                            }
                        }
                        libraryState.clearActionMode()
                    },
                    onDeleteHandler = {
                        mainState.archiveItems(libraryState.selectedItemIds.toList())
                        mainState.deleteFolders(libraryState.selectedFolderIds.toList())
                        libraryState.clearActionMode()
                    }
                )
            }
        },
        content = { innerPadding ->
            LibraryContent(
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                ),
                activeFolder = libraryState.activeFolder.value,
                showFolderSortMenu = libraryState.showFolderSortModeMenu.value,
                folderSortMode = mainState.libraryFolderSortMode.value,
                folderSortDirection = mainState.libraryFolderSortDirection.value,
                folders = mainState.libraryFolders.collectAsState().value,
                selectedFolderIds = libraryState.selectedFolderIds,
                showItemSortMenu = libraryState.showItemSortModeMenu.value,
                itemSortMode = mainState.libraryItemSortMode.value,
                itemSortDirection = mainState.libraryItemSortDirection.value,
                items = mainState.libraryItems.collectAsState().value,
                selectedItemIds = libraryState.selectedItemIds,
                onShowFolderSortMenuChange = { libraryState.showFolderSortModeMenu.value = it },
                onFolderSortModeSelected = {
                    mainState.sortLibraryFolders(it)
                    libraryState.showFolderSortModeMenu.value = false
                },
                onShowItemSortMenuChange = { libraryState.showItemSortModeMenu.value = it },
                onItemSortModeSelected = {
                    mainState.sortLibraryItems(it)
                    libraryState.showItemSortModeMenu.value = false
                },
                onLibraryFolderShortClicked = { folder ->
                    libraryState.apply {
                        if(actionMode.value) {
                            if(selectedFolderIds.contains(folder.id)) {
                                selectedFolderIds.remove(folder.id)
                                if(selectedFolderIds.isEmpty() && selectedItemIds.isEmpty()) {
                                    actionMode.value = false
                                }
                            } else {
                                selectedFolderIds.add(folder.id)
                            }
                        } else {
                            activeFolder.value = folder
                            clearActionMode()
                        }
                    }
                },
                onLibraryFolderLongClicked = { folder ->
                    libraryState.apply {
                        if (!selectedFolderIds.contains(folder.id)) {
                            selectedFolderIds.add(folder.id)
                            actionMode.value = true
                        }
                    }
                },
                onLibraryItemShortClicked = { item ->
                    libraryState.apply {
                        if (actionMode.value) {
                            if (selectedItemIds.contains(item.id)) {
                                selectedItemIds.remove(item.id)
                                if (selectedItemIds.isEmpty() && selectedFolderIds.isEmpty()) {
                                    actionMode.value = false
                                }
                            } else {
                                selectedItemIds.add(item.id)
                            }
                        } else {
                            editableItem.value = item
                            itemDialogMode.value = DialogMode.EDIT
                            itemDialogName.value = item.name
                            itemDialogColorIndex.value = item.colorIndex
                            itemDialogFolderId.value = item.libraryFolderId
                            showItemDialog.value = true
                            clearActionMode()
                        }
                    }
                },
                onLibraryItemLongClicked = { item ->
                    libraryState.apply {
                        if (!selectedItemIds.contains(item.id)) {
                            selectedItemIds.add(item.id)
                            actionMode.value = true
                        }
                    }
                },
            )

            // Show hint if no items or folders are in the library
            if (
                mainState.libraryFolders.collectAsState().value.isEmpty() &&
                mainState.libraryItems.collectAsState().value.isEmpty()
            ) {
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

            if(libraryState.showFolderDialog.value) {
                LibraryFolderDialog(
                    mode = libraryState.folderDialogMode.value,
                    folderName = libraryState.folderDialogName.value,
                    onFolderNameChange = { libraryState.folderDialogName.value = it },
                    onDismissHandler = { create ->
                        if(create) {
                            when(libraryState.folderDialogMode.value) {
                                DialogMode.ADD -> {
                                    mainState.addLibraryFolder(
                                        LibraryFolder(
                                            name = libraryState.folderDialogName.value,
                                        )
                                    )
                                }
                                DialogMode.EDIT -> {
                                    libraryState.editableFolder.value?.apply {
                                        name = libraryState.folderDialogName.value
                                        mainState.editFolder(this)
                                    }
                                }
                            }
                        }
                        libraryState.clearFolderDialog()
                    },
                )
            }

            if(libraryState.showItemDialog.value) {
                LibraryItemDialog(
                    mode = libraryState.itemDialogMode.value,
                    folders = mainState.libraryFolders.collectAsState().value,
                    name = libraryState.itemDialogName.value,
                    colorIndex = libraryState.itemDialogColorIndex.value,
                    folderId = libraryState.itemDialogFolderId.value,
                    folderSelectorExpanded = libraryState.itemDialogFolderSelectorExpanded.value,
                    onNameChange = { libraryState.itemDialogName.value = it },
                    onColorIndexChange = { libraryState.itemDialogColorIndex.value = it },
                    onFolderIdChange = {
                        libraryState.itemDialogFolderId.value = it
                        libraryState.itemDialogFolderSelectorExpanded.value = SpinnerState.COLLAPSED
                    },
                    onFolderSelectorExpandedChange = { libraryState.itemDialogFolderSelectorExpanded.value = it },
                    onDismissHandler = { cancel ->
                        if(!cancel) {
                            when(libraryState.itemDialogMode.value) {
                                DialogMode.ADD -> {
                                    mainState.addLibraryItem(
                                        LibraryItem(
                                            name = libraryState.itemDialogName.value,
                                            colorIndex = libraryState.itemDialogColorIndex.value,
                                            libraryFolderId = libraryState.itemDialogFolderId.value
                                        )
                                    )
                                }
                                DialogMode.EDIT -> {
                                    libraryState.editableItem.value?.apply {
                                        name = libraryState.itemDialogName.value
                                        colorIndex = libraryState.itemDialogColorIndex.value
                                        libraryFolderId = libraryState.itemDialogFolderId.value
                                        mainState.editItem(this)
                                    }
                                }
                            }
                        }
                        libraryState.clearItemDialog()
                    },
                )
            }

            // Content Scrim for multiFAB
            AnimatedVisibility(
                modifier = Modifier
                    .zIndex(1f),
                visible = libraryState.multiFABState.value == MultiFABState.EXPANDED,
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
                            libraryState.multiFABState.value = MultiFABState.COLLAPSED
                            mainState.showNavBarScrim.value = false
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
) {
    DropdownMenuItem(
        text = { Text(text = "Sort by") },
        onClick = { onSelectionHandler(LibraryMenuSelections.SORT_BY) },
        trailingIcon = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryContent(
    contentPadding: PaddingValues,
    activeFolder: LibraryFolder?,
    showFolderSortMenu: Boolean,
    folderSortMode: LibraryFolderSortMode,
    folderSortDirection: SortDirection,
    folders: List<LibraryFolder>,
    selectedFolderIds: List<Long>,
    showItemSortMenu: Boolean,
    itemSortMode: LibraryItemSortMode,
    itemSortDirection: SortDirection,
    items: List<LibraryItem>,
    selectedItemIds: List<Long>,
    onShowFolderSortMenuChange: (Boolean) -> Unit,
    onFolderSortModeSelected: (LibraryFolderSortMode) -> Unit,
    onShowItemSortMenuChange: (Boolean) -> Unit,
    onItemSortModeSelected: (LibraryItemSortMode) -> Unit,
    onLibraryFolderShortClicked: (LibraryFolder) -> Unit,
    onLibraryFolderLongClicked: (LibraryFolder) -> Unit,
    onLibraryItemShortClicked: (LibraryItem) -> Unit,
    onLibraryItemLongClicked: (LibraryItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 56.dp,
        ),
    ) {
        // if active folder ist null, we are in the top level
        if(activeFolder == null) {
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
                                selected = selectedFolderIds.contains(folder.id),
                                onShortClick = { onLibraryFolderShortClicked(folder) },
                                onLongClick = { onLibraryFolderLongClicked(folder) }
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
                        selected = item.id in selectedItemIds,
                        onShortClick = { onLibraryItemShortClicked(item) },
                        onLongClick = { onLibraryItemLongClicked(item) }
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
