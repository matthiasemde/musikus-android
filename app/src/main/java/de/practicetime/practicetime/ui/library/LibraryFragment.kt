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

import android.os.FileObserver.CREATE
import android.view.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.LibraryFolder
import de.practicetime.practicetime.database.entities.LibraryItem
import de.practicetime.practicetime.shared.*
import de.practicetime.practicetime.ui.MainState
import de.practicetime.practicetime.ui.ThemeSelections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class LibrarySortMode {
    DATE_ADDED,
    LAST_MODIFIED,
    NAME,
    COLOR,
    CUSTOM
}


enum class LibraryMenuSelections {
    SORT_BY,
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

enum class CommonMenuSelections {
    THEME,
    APP_INFO
}

enum class DialogMode {
    ADD,
    EDIT
}

class LibraryState(
    private val coroutineScope: CoroutineScope,
) {
    init {
        loadItems()
        loadFolders()
    }

    var actionMode = mutableStateOf(false)

    var showMainMenu = mutableStateOf(false)
    var showThemeSubMenu = mutableStateOf(false)
    var showSortModeSubMenu = mutableStateOf(false)

    var showFolderDialog = mutableStateOf(false)
    var editableFolder = mutableStateOf<LibraryFolder?>(null)
    var folderDialogMode = mutableStateOf(DialogMode.ADD)
    var folderDialogName = mutableStateOf("")

    fun clearFolderDialog() {
        showFolderDialog.value = false
        editableFolder.value = null
        folderDialogName.value = ""
    }

    var showItemDialog = mutableStateOf(false)
    var editableItem = mutableStateOf<LibraryItem?>(null)
    var itemDialogMode = mutableStateOf(DialogMode.ADD)
    var itemDialogName = mutableStateOf("")
    var itemDialogColorIndex = mutableStateOf(0)
    var itemDialogFolderId = mutableStateOf<Long?>(null)
    var itemDialogFolderSelectorExpanded = mutableStateOf(SpinnerState.COLLAPSED)

    fun clearItemDialog() {
        showItemDialog.value = false
        editableItem.value = null
        itemDialogName.value = ""
        itemDialogColorIndex.value = 0
        itemDialogFolderId.value = null
        itemDialogFolderSelectorExpanded.value = SpinnerState.COLLAPSED
    }

    var multiFABState = mutableStateOf(MultiFABState.COLLAPSED)

    val folders = mutableStateListOf<LibraryFolder>()
    var items = mutableStateListOf<LibraryItem>()
    val selectedItemIds = mutableStateListOf<Long>()
    val selectedFolderIds = mutableStateListOf<Long>()
    val activeFolder = mutableStateOf<LibraryFolder?>(null)

    fun archiveSelectedItems() {
        coroutineScope.launch {
            selectedItemIds.forEach { itemId ->
                if (PracticeTime.libraryItemDao.archive(itemId)) // returns false in case item couldnt be archived
                    items.remove(items.find { it.id == itemId })
                else
                    TODO("Show error message")
            }
            selectedItemIds.clear()
            actionMode.value = false
        }
    }

    fun deleteSelectedFolders() {
        coroutineScope.launch {
            selectedFolderIds.forEach { folderId ->
                PracticeTime.libraryFolderDao.deleteAndResetItems(folderId)
                folders.remove(folders.find { it.id == folderId })
            }
            loadItems() // reload items to show those which were in a folder
            selectedFolderIds.clear()
            actionMode.value = false
        }
    }

    var sortMode = mutableStateOf(try {
        PracticeTime.prefs.getString(
            PracticeTime.PREFERENCES_KEY_LIBRARY_SORT_MODE,
            LibrarySortMode.DATE_ADDED.name
        )?.let { LibrarySortMode.valueOf(it) } ?: LibrarySortMode.DATE_ADDED
    } catch (ex: Exception) {
        LibrarySortMode.DATE_ADDED
    })

    var sortDirection = mutableStateOf(try {
        PracticeTime.prefs.getString(
            PracticeTime.PREFERENCES_KEY_LIBRARY_SORT_DIRECTION,
            SortDirection.ASCENDING.name
        )?.let { SortDirection.valueOf(it) } ?: SortDirection.ASCENDING
    } catch (ex: Exception) {
        SortDirection.ASCENDING
    })


    fun sortItems(mode: LibrarySortMode? = null) {
        if(mode != null) {
            if (mode == sortMode.value) {
                when (sortDirection.value) {
                    SortDirection.ASCENDING -> sortDirection.value = SortDirection.DESCENDING
                    SortDirection.DESCENDING -> sortDirection.value = SortDirection.ASCENDING
                }
            } else {
                sortDirection.value = SortDirection.ASCENDING
                sortMode.value = mode
                PracticeTime.prefs.edit().putString(
                    PracticeTime.PREFERENCES_KEY_LIBRARY_SORT_MODE,
                    sortMode.value.name
                ).apply()
            }
            PracticeTime.prefs.edit().putString(
                PracticeTime.PREFERENCES_KEY_LIBRARY_SORT_DIRECTION,
                sortDirection.value.name
            ).apply()
        }
        when (sortDirection.value) {
            SortDirection.ASCENDING -> {
                when (sortMode.value) {
                    LibrarySortMode.DATE_ADDED -> items.sortBy { it.createdAt }
                    LibrarySortMode.LAST_MODIFIED -> items.sortBy { it.modifiedAt }
                    LibrarySortMode.NAME -> items.sortBy { it.name }
                    LibrarySortMode.COLOR -> items.sortBy { it.colorIndex }
                    LibrarySortMode.CUSTOM -> TODO()
                }
            }
            SortDirection.DESCENDING -> {
                when (sortMode.value) {
                    LibrarySortMode.DATE_ADDED -> items.sortByDescending { it.createdAt }
                    LibrarySortMode.LAST_MODIFIED -> items.sortByDescending { it.modifiedAt }
                    LibrarySortMode.NAME -> items.sortByDescending { it.name }
                    LibrarySortMode.COLOR -> items.sortByDescending { it.colorIndex }
                    LibrarySortMode.CUSTOM -> TODO()
                }
            }
        }
    }

    fun addFolder(newFolder: LibraryFolder) {
        coroutineScope.launch {
            PracticeTime.libraryFolderDao.insertAndGet(newFolder)?.let {
                folders.add(0, it)
            }
            clearFolderDialog()
        }
    }

    fun editFolder(folder: LibraryFolder) {
        coroutineScope.launch {
            PracticeTime.libraryFolderDao.update(folder)
            folders.removeIf { it.id == folder.id }
            folders.add(folder)
            clearFolderDialog()
        }
    }

    fun clearActionMode() {
        selectedItemIds.clear()
        selectedFolderIds.clear()
        actionMode.value = false
    }

    fun loadItems() {
        coroutineScope.launch {
            items.clear()
            PracticeTime.libraryItemDao.get(activeOnly = true).forEach { item ->
                // check if the items folderId actually exists
                item.libraryFolderId?.let {
                    if(PracticeTime.libraryFolderDao.get(it) == null) {
                        item.libraryFolderId = null
                        PracticeTime.libraryItemDao.update(item)
                    }
                }
                items.add(item)
            }
            sortItems()
        }
    }

    fun loadFolders() {
        coroutineScope.launch {
            folders.clear()
            folders.addAll(PracticeTime.libraryFolderDao.getAll().reversed())
        }
    }

    fun addItem(item: LibraryItem) {
        coroutineScope.launch {
            PracticeTime.libraryItemDao.insertAndGet(item)?.let {
                items.add(0, it)
                sortItems()
                clearItemDialog()
            }
        }
    }

    fun editItem(item: LibraryItem) {
        coroutineScope.launch {
            PracticeTime.libraryItemDao.update(item)
            sortItems()
            clearItemDialog()
        }
    }
}


@Composable
fun rememberLibraryState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) = remember(coroutineScope) { LibraryState(coroutineScope) }

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
fun LibraryComposable(
    mainState: MainState,
    showNavBarScrim: (Boolean) -> Unit,
) {
    val libraryState = rememberLibraryState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        contentWindowInsets = WindowInsets(bottom = 0.dp),
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
                        showNavBarScrim(false)
                    },
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New item")
                }
            } ?: MultiFAB(
                state = libraryState.multiFABState.value,
                onStateChange = { state ->
                    libraryState.multiFABState.value = state
                    showNavBarScrim(state == MultiFABState.EXPANDED)
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
                            showNavBarScrim(false)
                        },
                        label = "Item",
                        icon = Icons.Rounded.MusicNote
                    ),
                    MiniFABData(
                        onClick = {
                            libraryState.folderDialogMode.value = DialogMode.ADD
                            libraryState.showFolderDialog.value = true
                            libraryState.multiFABState.value = MultiFABState.COLLAPSED
                            showNavBarScrim(false)
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
                    TextButton(
                        onClick = { libraryState.showSortModeSubMenu.value = true })
                    {
                        Text(
                            modifier = Modifier.padding(end = 8.dp),
                            color = colorScheme.onSurface,
                            text = when (libraryState.sortMode.value) {
                            LibrarySortMode.DATE_ADDED -> "Date added"
                            LibrarySortMode.NAME -> "Name"
                            LibrarySortMode.COLOR -> "Color"
                            LibrarySortMode.LAST_MODIFIED -> "Last modified"
                            LibrarySortMode.CUSTOM -> "Custom"
                        })
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = when (libraryState.sortDirection.value) {
                                SortDirection.ASCENDING -> Icons.Default.ArrowUpward
                                SortDirection.DESCENDING -> Icons.Default.ArrowDownward
                            },
                            tint = colorScheme.onSurface,
                            contentDescription = null
                        )
                        LibrarySubMenuSortMode(
                            offset = DpOffset((-10).dp, 10.dp),
                            show = libraryState.showSortModeSubMenu.value,
                            onDismissHandler = { libraryState.showSortModeSubMenu.value = false },
                            sortMode = libraryState.sortMode.value,
                            sortDirection = libraryState.sortDirection.value,
                            onSelectionHandler = { sortMode ->
                                libraryState.showSortModeSubMenu.value = false
                                libraryState.sortItems(sortMode)
                            }
                        )
                    }
                    IconButton(onClick = {
                        libraryState.showMainMenu.value = true
                    }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "more")
                        MainMenu(
                            show = libraryState.showMainMenu.value,
                            onDismissHandler = { libraryState.showMainMenu.value = false },
                            onSelectionHandler = { librarySelection, commonSelection ->
                                libraryState.showMainMenu.value = false
                                when (librarySelection) {
                                    LibraryMenuSelections.SORT_BY -> {
                                        libraryState.showSortModeSubMenu.value = true
                                    }
                                    null -> {}
                                }
                                when (commonSelection) {
                                    CommonMenuSelections.APP_INFO -> {}
                                    CommonMenuSelections.THEME -> {
                                        libraryState.showThemeSubMenu.value = true
                                    }
                                    null -> {}
                                }
                            }
                        )
                        ThemeSubMenu(
                            show = libraryState.showThemeSubMenu.value,
                            currentTheme = mainState.activeTheme.value,
                            onDismissHandler = { libraryState.showThemeSubMenu.value = false },
                            onSelectionHandler = { theme ->
                                libraryState.showThemeSubMenu.value = false
                                mainState.setTheme(theme)
                            }
                        )
                    }
                }
            )
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
                            items.firstOrNull { item ->
                                selectedItemIds.firstOrNull()?.let { it == item.id } ?: false
                            }?.let { item ->
                                editableItem.value = item
                                itemDialogMode.value = DialogMode.EDIT
                                itemDialogName.value = item.name
                                itemDialogColorIndex.value = item.colorIndex
                                itemDialogFolderId.value = item.libraryFolderId
                                showItemDialog.value = true
                            } ?: folders.firstOrNull { folder ->
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
                        libraryState.archiveSelectedItems()
                        libraryState.deleteSelectedFolders()
                    }
                )
            }
        },
        content = { innerPadding ->
            LibraryContent(
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                ),
                folders = if(libraryState.activeFolder.value == null) libraryState.folders else listOf(),
                items = libraryState.items.filter { it.libraryFolderId == libraryState.activeFolder.value?.id },
                selectedItemIds = libraryState.selectedItemIds,
                selectedFolderIds = libraryState.selectedFolderIds,
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
            if (libraryState.folders.isEmpty() && libraryState.items.isEmpty()) {
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
                                    libraryState.addFolder(
                                        LibraryFolder(
                                            name = libraryState.folderDialogName.value,
                                        )
                                    )
                                }
                                DialogMode.EDIT -> {
                                    libraryState.editableFolder.value?.apply {
                                        name = libraryState.folderDialogName.value
                                        libraryState.editFolder(this)
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
                    folders = libraryState.folders,
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
                                    libraryState.addItem(
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
                                        libraryState.editItem(this)
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
                            showNavBarScrim(false)
                        }
                )
            }
        }
    )
}


@Composable
fun ThemeSubMenu(
    show: Boolean,
    currentTheme: ThemeSelections,
    onDismissHandler: () -> Unit,
    onSelectionHandler: (ThemeSelections) -> Unit,
) {
    DropdownMenu(expanded = show, onDismissRequest = onDismissHandler) {
        // Menu Header
        Text(
            modifier = Modifier.padding(12.dp),
            text = "Theme",
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.onSurface.copy(alpha = 0.8f)
        )

        // Menu Items
        DropdownMenuItem(
            text = { Text(
                text = "Automatic",
                color = if (currentTheme == ThemeSelections.SYSTEM) colorScheme.primary else Color.Unspecified
            ) },
            onClick = { onSelectionHandler(ThemeSelections.SYSTEM) },
            trailingIcon = {
                if(currentTheme == ThemeSelections.SYSTEM)
                    Icon(Icons.Default.Check, contentDescription = null, tint = colorScheme.primary)
            }
        )
        DropdownMenuItem(
            text = { Text(
                text = "Light",
                color = if (currentTheme == ThemeSelections.DAY) colorScheme.primary else Color.Unspecified
            ) },
            onClick = { onSelectionHandler(ThemeSelections.DAY) },
            trailingIcon = {
                if(currentTheme == ThemeSelections.DAY)
                    Icon(Icons.Default.Check, contentDescription = null, tint = colorScheme.primary)
            }
       )
        DropdownMenuItem(
            text = { Text(
                text = "Dark",
                color = if (currentTheme == ThemeSelections.NIGHT) colorScheme.primary else Color.Unspecified
            ) },
            onClick = { onSelectionHandler(ThemeSelections.NIGHT) },
            trailingIcon = {
                if(currentTheme == ThemeSelections.NIGHT)
                    Icon(Icons.Default.Check, contentDescription = null, tint = colorScheme.primary)
            }
        )
    }
}

@Composable
fun CommonMenuItems(
    onSelectionHandler: (CommonMenuSelections) -> Unit
) {
    DropdownMenuItem(
        text = { Text(text = "Theme") },
        trailingIcon = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
        onClick = { onSelectionHandler(CommonMenuSelections.THEME) }
    )
    DropdownMenuItem(
        text = { Text(text = "App Info") },
        onClick = { onSelectionHandler(CommonMenuSelections.APP_INFO) }
    )
}

@Composable
fun LibrarySubMenuSortMode(
    offset: DpOffset,
    show: Boolean,
    onDismissHandler: () -> Unit,
    sortMode: LibrarySortMode,
    sortDirection: SortDirection,
    onSelectionHandler: (LibrarySortMode) -> Unit
) {
    DropdownMenu(
        offset = offset,
        expanded = show,
        onDismissRequest = onDismissHandler,
    ) {
        // Menu Header
        Text(
            modifier = Modifier.padding(12.dp),
            text = "Sort by",
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.onSurface.copy(alpha = 0.8f)
        )

        // Menu Body
        val directionIcon: @Composable () -> Unit = {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = when (sortDirection) {
                    SortDirection.ASCENDING -> Icons.Default.ArrowUpward
                    SortDirection.DESCENDING -> Icons.Default.ArrowDownward
                },
                tint = colorScheme.primary,
                contentDescription = null
            )
        }
        val primaryColor = colorScheme.primary
        DropdownMenuItem(
            text = { Text(
                text = "Date Added",
                color = if (sortMode == LibrarySortMode.DATE_ADDED) primaryColor else Color.Unspecified
            ) },
            onClick = { onSelectionHandler(LibrarySortMode.DATE_ADDED) },
            trailingIcon = if (sortMode == LibrarySortMode.DATE_ADDED) directionIcon else null
        )
        DropdownMenuItem(
            text = { Text(
                text = "Last modified",
                color = if (sortMode == LibrarySortMode.LAST_MODIFIED) primaryColor else Color.Unspecified
            ) },
            onClick = { onSelectionHandler(LibrarySortMode.LAST_MODIFIED) },
            trailingIcon = if (sortMode == LibrarySortMode.LAST_MODIFIED) directionIcon else null
        )
        DropdownMenuItem(
            text = { Text(
                text = "Name",
                color = (if (sortMode == LibrarySortMode.NAME) primaryColor else Color.Unspecified)
            ) },
            onClick = { onSelectionHandler(LibrarySortMode.NAME) },
            trailingIcon = if (sortMode == LibrarySortMode.NAME) directionIcon else null
        )
        DropdownMenuItem(
            text = { Text(
                text = "Color",
                color = if (sortMode == LibrarySortMode.COLOR) primaryColor else Color.Unspecified
            ) },
            onClick = { onSelectionHandler(LibrarySortMode.COLOR) },
            trailingIcon = if (sortMode == LibrarySortMode.COLOR) directionIcon else null
        )
    }
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

@Composable
fun MainMenu(
    show: Boolean,
    onDismissHandler: () -> Unit,
    onSelectionHandler: (
        librarySelection: LibraryMenuSelections?,
        commonSelection: CommonMenuSelections?
    ) -> Unit
) {
    DropdownMenu(
        expanded = show,
        onDismissRequest = onDismissHandler,
    ) {
        LibraryMenuItems(
            onSelectionHandler = { onSelectionHandler(it, null) }
        )
        CommonMenuItems(
            onSelectionHandler = { onSelectionHandler( null, it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionBar(
    numSelectedItems: Int,
    onDismissHandler: () -> Unit,
    onEditHandler: () -> Unit,
    onDeleteHandler: () -> Unit
) {
    TopAppBar(
        title = { Text(text = "$numSelectedItems selected") },
        colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = colorScheme.primaryContainer),
        navigationIcon = {
            IconButton(onClick = onDismissHandler) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Back",
                    tint = colorScheme.onPrimaryContainer
                )
            }
        },
        actions = {
            if(numSelectedItems == 1) {
                IconButton(onClick = {
                    onEditHandler()
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit",
                        tint = colorScheme.onPrimaryContainer
                    )
                }
            }
            IconButton(onClick = {
                onDeleteHandler()
            }) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = colorScheme.onPrimaryContainer
                )
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryFolderComposable(
    folder: LibraryFolder,
    selected: Boolean,
    onShortClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box {
        Surface(
            modifier = Modifier
                .size(150.dp)
                .clip(MaterialTheme.shapes.large)
                .combinedClickable (
                    onClick = onShortClick,
                    onLongClick = onLongClick
                ),
            color = colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            tonalElevation = 1.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    modifier = Modifier.padding(4.dp),
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if(selected) {
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .matchParentSize()
                    .background(color = colorScheme.onSurface.copy(alpha = 0.2f))
            )
        }
    }
}

@Composable
fun LibraryItemComposable(
    modifier: Modifier,
    libraryItem: LibraryItem,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryContent(
    contentPadding: PaddingValues,
    folders: List<LibraryFolder>,
    items: List<LibraryItem>,
    selectedItemIds: List<Long>,
    selectedFolderIds: List<Long>,
    onLibraryFolderShortClicked: (LibraryFolder) -> Unit,
    onLibraryFolderLongClicked: (LibraryFolder) -> Unit,
    onLibraryItemShortClicked: (LibraryItem) -> Unit,
    onLibraryItemLongClicked: (LibraryItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
    ) {
        if(folders.isNotEmpty()) {
            item {
                Text(
                    modifier = Modifier
                        .padding(16.dp),
                    text = "Folders", style = MaterialTheme.typography.titleLarge
                )
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
                            LibraryFolderComposable(
                                folder = folder,
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
        if(items.isNotEmpty()) {
            item {
                Text(
                    modifier = Modifier
                        .padding(16.dp),
                    text="Items",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(
                items=items,
                key = { item -> item.id }
            ) { item ->
                Box(
                    modifier = Modifier
                        .animateItemPlacement()
                        .combinedClickable(
                            onLongClick = { onLibraryItemLongClicked(item) },
                            onClick = { onLibraryItemShortClicked(item) }
                        )
                ) {
                    Row {
                        LibraryItemComposable(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                            libraryItem = item,
                        )
                    }
                    if(selectedItemIds.contains(item.id)) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(colorScheme.onSurface.copy(alpha = 0.1f))
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(40.dp)) } // maybe solve this using content value padding instead
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryFolderDialog(
    mode: DialogMode,
    folderName: String,
    onFolderNameChange: (String) -> Unit,
    onDismissHandler: (Boolean) -> Unit, // true if folder was created
) {
    Dialog(
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        onDismissRequest = { onDismissHandler(false) }
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
        ) {
            Row(
                modifier = Modifier
                    .background(colorScheme.primaryContainer)
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp, bottom = 16.dp),
                    text = when(mode) {
                        DialogMode.ADD -> "Create folder"
                        DialogMode.EDIT -> "Edit folder"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onPrimaryContainer,
                )
            }
            Column (
                modifier = Modifier
                    .background(colorScheme.surface)
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .padding(horizontal = 24.dp),
                    value = folderName, onValueChange = onFolderNameChange,
                    label = { Text(text = "Folder name") },
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
                            contentColor = colorScheme.primary
                        )
                    ) {
                        Text(text = "Cancel")
                    }
                    TextButton(
                        onClick = { onDismissHandler(true) },
                        enabled = folderName.isNotEmpty(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = colorScheme.primary
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
    Dialog(
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        onDismissRequest = { onDismissHandler(true) }
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
        ) {
            Row(
                modifier = Modifier
                    .background(colorScheme.primaryContainer)
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp, bottom = 16.dp),
                    text = when(mode) {
                        DialogMode.ADD -> "Create item"
                        DialogMode.EDIT -> "Edit item"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onPrimaryContainer,
                )
            }
            Column (
                modifier = Modifier
                    .background(colorScheme.surface)
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .padding(horizontal = 24.dp),
                    value = name,
                    onValueChange = onNameChange,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Item name",
                            tint = colorScheme.onSurface.copy(alpha = 0.6f)
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
                        .padding(top = 16.dp)
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
                            contentColor = colorScheme.primary
                        )
                    ) {
                        Text(text = "Cancel")
                    }
                    TextButton(
                        onClick = { onDismissHandler(false) },
                        enabled = name.isNotEmpty(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = colorScheme.primary
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