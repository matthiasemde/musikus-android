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
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
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


class LibraryState(
    private val coroutineScope: CoroutineScope,
) {
    var showMainMenu = mutableStateOf(false)
    var showThemeSubMenu = mutableStateOf(false)
    var showSortModeSubMenu = mutableStateOf(false)

    var showAddFolderDialog = mutableStateOf(false)
    var newFolderName = mutableStateOf("")

    var showAddItemDialog = mutableStateOf(false)
    var newItemName = mutableStateOf("")
    var newItemColorIndex = mutableStateOf(0)

    var multiFABState = mutableStateOf(MultiFABState.COLLAPSED)

    val folders = mutableStateListOf<LibraryFolder>()
    var items = mutableStateListOf<LibraryItem>()
    val selectedItems = mutableStateListOf<Long>()

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
        when (sortMode.value) {
            LibrarySortMode.DATE_ADDED -> items.sortBy { it.createdAt }
            LibrarySortMode.NAME -> items.sortBy { it.name }
            LibrarySortMode.COLOR -> items.sortBy { it.colorIndex }
            LibrarySortMode.LAST_MODIFIED -> items.sortBy { it.modifiedAt }
            LibrarySortMode.CUSTOM -> items.sortBy { it.createdAt } // TODO: Not implemented yet
        }
        if (sortDirection.value == SortDirection.DESCENDING) {
            items.reverse()
//            val tmp = items
//            items.clear()
//            items.addAll(tmp.reversed())
//            Log.d("items", items.toString())
        }
    }

    fun addFolder(newFolder: LibraryFolder) {
        coroutineScope.launch {
            PracticeTime.libraryFolderDao.insertAndGet(newFolder)?.let {
                folders.add(0, it)
            }
        }
    }

    fun addItem(newItem: LibraryItem) {
        coroutineScope.launch {
            PracticeTime.libraryItemDao.insertAndGet(newItem)?.let {
                items.add(0, it)
                sortItems()
            }
        }
    }
//
//    var editLibraryItemDialog: LibraryItemDialog? = null
//    var deleteLibraryItemDialog: AlertDialog? = null
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

    LaunchedEffect(true) {
        PracticeTime.libraryFolderDao.get().let {
            libraryState.folders.addAll(it)
            libraryState.folders.reverse()
        }
    }

    LaunchedEffect(true) {
        PracticeTime.libraryItemDao.get(activeOnly = true).let {
            // sort libraryItems depending on the current sort mode
            libraryState.items.addAll(it)
            libraryState.sortItems()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(bottom = 0.dp),
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        floatingActionButton = {
            MultiFAB(
                state = libraryState.multiFABState.value,
                onStateChange = { state ->
                    libraryState.multiFABState.value = state
                    showNavBarScrim(state == MultiFABState.EXPANDED)
                },
                miniFABs = listOf(
                    MiniFABData(
                        onClick = {
                            libraryState.showAddItemDialog.value = true
                            libraryState.multiFABState.value = MultiFABState.COLLAPSED
                            showNavBarScrim(false)
                        },
                        label = "Item",
                        icon = Icons.Rounded.MusicNote
                    ),
                    MiniFABData(
                        onClick = {
                            libraryState.showAddFolderDialog.value = true
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
                title = {
                    Text(text = "Library")
                },
                actions = {
                    IconButton(onClick = {
                        libraryState.showMainMenu.value = !libraryState.showMainMenu.value
                    }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "more")
                    }
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
                    LibrarySubMenuSortMode(
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
            )
        },
        content = { innerPadding ->
            LibraryContent(
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                ),
                folders = libraryState.folders,
                items = libraryState.items,
                selectedItems = libraryState.selectedItems,
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

            if(libraryState.showAddFolderDialog.value) {
                LibraryFolderDialog(
                    folderName = libraryState.newFolderName.value,
                    onFolderNameChange = { libraryState.newFolderName.value = it },
                    onDismissHandler = { create ->
                        libraryState.showAddFolderDialog.value = false
                        if(create) {
                            libraryState.addFolder(
                                LibraryFolder(
                                    name = libraryState.newFolderName.value,
                                )
                            )
                        }
                    },
                )
            }

            if(libraryState.showAddItemDialog.value) {
                LibraryItemDialog(
                    itemName = libraryState.newItemName.value,
                    colorIndex = libraryState.newItemColorIndex.value,
                    onItemNameChange = { libraryState.newItemName.value = it },
                    onColorIndexChange = {
                        Log.d("color", it.toString())
                        libraryState.newItemColorIndex.value = it
                     },
                    onDismissHandler = { create ->
                        libraryState.showAddItemDialog.value = false
                        if(create) {
                            libraryState.addItem(
                                LibraryItem(
                                    name = libraryState.newItemName.value,
                                    colorIndex = libraryState.newItemColorIndex.value,
                                )
                            )
                        }
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
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
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
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        // Menu Items
        DropdownMenuItem(
            text = { Text(text = "Automatic") },
            onClick = { onSelectionHandler(ThemeSelections.SYSTEM) },
            trailingIcon = {
                if(currentTheme == ThemeSelections.SYSTEM)
                    Icon(Icons.Default.Check, contentDescription = null)
            }
        )
        DropdownMenuItem(
            text = { Text(text = "Light") },
            onClick = { onSelectionHandler(ThemeSelections.DAY) },
            trailingIcon = {
                if(currentTheme == ThemeSelections.DAY)
                    Icon(Icons.Default.Check, contentDescription = null)
            }
       )
        DropdownMenuItem(
            text = { Text(text = "Dark") },
            onClick = { onSelectionHandler(ThemeSelections.NIGHT) },
            trailingIcon = {
                if(currentTheme == ThemeSelections.NIGHT)
                    Icon(Icons.Default.Check, contentDescription = null)
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
    show: Boolean,
    onDismissHandler: () -> Unit,
    sortMode: LibrarySortMode,
    sortDirection: SortDirection,
    onSelectionHandler: (LibrarySortMode) -> Unit
) {
    DropdownMenu(
        expanded = show,
        onDismissRequest = onDismissHandler,
    ) {
        // Menu Header
        Text(
            modifier = Modifier.padding(12.dp),
            text = "Sort by",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        // Menu Body
        val directionIcon: @Composable () -> Unit = {
            Icon(
                imageVector = when (sortDirection) {
                    SortDirection.ASCENDING -> Icons.Default.KeyboardArrowUp
                    SortDirection.DESCENDING -> Icons.Default.KeyboardArrowDown
                },
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null
            )
        }
        val primaryColor = MaterialTheme.colorScheme.primary
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

@Composable
fun LibraryFolderComposable(
    folder: LibraryFolder
) {
    Button(
        modifier = Modifier
            .size(150.dp),
        colors = ButtonDefaults.elevatedButtonColors(),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.elevatedButtonElevation(),
        onClick = {}
    ) {
        Text(text = folder.name, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun LibraryItemComposable(
    contentPadding: PaddingValues,
    libraryItem: LibraryItem,
    selected: Boolean
//        itemClickedCallback: (callbackListDataItem: CallbackListDataItem) -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(contentPadding)
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
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
    selectedItems: List<Long>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if(folders.isNotEmpty()) {
            item {
                Text(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .padding(horizontal = 16.dp),
                    text = "Folders", style = MaterialTheme.typography.titleLarge
                )
            }
            item {
                LazyRow(
//                    contentPadding = PaddingValues(horizontal = 16.dp),
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
                            modifier = Modifier.animateItemPlacement()
                        ) {
                            LibraryFolderComposable(folder = folder)
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
                        .padding(top = 16.dp)
                        .padding(horizontal = 16.dp),
                    text="Items",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(
                items=items,
                key = { item -> item.id }
            ) { item ->
                Row(
                    modifier = Modifier.animateItemPlacement()
                ) {
                    LibraryItemComposable(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        libraryItem = item,
                        selected = selectedItems.contains(item.id)
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(0.dp)) } // maybe solve this using content value padding instead
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryFolderDialog(
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
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp, bottom = 16.dp),
                    text = "Create folder",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column (
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
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
                        Text(text = "Create")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryItemDialog(
    itemName: String,
    colorIndex: Int,
    onItemNameChange: (String) -> Unit,
    onColorIndexChange: (Int) -> Unit,
    onDismissHandler: (Boolean) -> Unit, // true if item was created
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
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp, bottom = 16.dp),
                    text = "Create item",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column (
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .padding(horizontal = 24.dp),
                    value = itemName, onValueChange = onItemNameChange,
                    label = { Text(text = "Item name") },
                )
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
                            ColorSelectRadioButton(
                                color = Color(PracticeTime.getLibraryItemColors(LocalContext.current)[i]),
                                selected = colorIndex == i,
                                onClick = { onColorIndexChange(i) }
                            )
                            ColorSelectRadioButton(
                                color = Color(PracticeTime.getLibraryItemColors(LocalContext.current)[i+5]),
                                selected = colorIndex == i+5,
                                onClick = { onColorIndexChange(i+5) }
                            )
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
                        onClick = { onDismissHandler(false) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = "Cancel")
                    }
                    TextButton(
                        onClick = { onDismissHandler(true) },
                        enabled = itemName.isNotEmpty(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = "Create")
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


//    private fun initLibraryItemList() {
//        libraryItemAdapter = LibraryItemAdapter(
//            libraryItems,
//            selectedItems,
//            context = requireActivity(),
//            shortClickHandler = ::shortClickOnLibraryItemHandler,
//            longClickHandler = ::longClickOnLibraryItemHandler,
//        )
//
//        // load all active libraryItems from the database and notify the adapter
//        lifecycleScope.launch {
//            PracticeTime.libraryItemDao.get(activeOnly = true).let {
//                // sort libraryItems depending on the current sort mode
//                libraryItems.addAll(it)
//                Log.d("LIBRARY","$sortMode")
//                sortLibraryItemList(sortMode)
////                libraryItemAdapter?.notifyItemRangeInserted(0, it.size) // sortLibraryItemList() already notifies the adapter
//            }
//
////            requireActivity().findViewById<RecyclerView>(R.id.libraryLibraryItemList).apply {
//////                layoutManager = GridLayoutManager(context, 2)
////                layoutManager = LinearLayoutManager(context)
////                adapter = libraryItemAdapter
////                itemAnimator?.apply {
////                    addDuration = 500L
////                    moveDuration = 500L
////                    removeDuration = 200L
////                }
////            }
//
//            if (libraryItems.isEmpty()) showHint()
//        }
//    }

    // initialize the libraryItem delete dialog
//    private fun initDeleteLibraryItemDialog() {
//        deleteLibraryItemDialog = AlertDialog.Builder(requireActivity()).apply {
//            setPositiveButton(R.string.deleteDialogConfirm) { dialog, _ ->
////                deleteLibraryItemsHandler()
//                dialog.dismiss()
//            }
//            setNegativeButton(R.string.dialogCancel) { dialog, _ ->
//                dialog.cancel()
//            }
//        }.create()
//    }
//
//    private fun actionModeCallback(): ActionMode.Callback {
//        return object : ActionMode.Callback {
//            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
//                mode?.menuInflater?.inflate(R.menu.library_toolbar_menu_for_selection, menu)
//                libraryCollapsingToolbarLayout.apply {
//                    setBackgroundColor(PracticeTime.getThemeColor(R.attr.colorSurface, requireContext()))
//                    contentScrim = null
//                }
//                return true
//            }
//
//            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
//                return false
//            }
//
//            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
//                return when(item?.itemId) {
//                    R.id.topToolbarSelectionEdit -> {
//                        // editLibraryItemDialog?.show(libraryItems[selectedItems.first().toInt()])
//                        true
//                    }
//                    R.id.topToolbarSelectionDelete -> {
//                        if(PracticeTime.serviceIsRunning)
//                            Toast.makeText(context, getString(R.string.cannot_delete_error), Toast.LENGTH_SHORT).show()
//                        else {
//                            deleteLibraryItemDialog?.apply {
//                                setMessage(
//                                    context.getString(
//                                        if (selectedItems.size > 1) R.string.deleteLibraryItemsDialogMessage
//                                        else R.string.deleteLibraryItemDialogMessage
//                                    )
//                                )
//                                show()
//                            }
//                        }
//                        true
//                    }
//                    else -> false
//                }
//            }
//
//            override fun onDestroyActionMode(mode: ActionMode?) {
//                clearLibraryItemSelection()
//                val transparentSurfaceColor =
//                    ColorUtils.setAlphaComponent(PracticeTime.getThemeColor(R.attr.colorSurface, requireContext()), 0)
//                libraryCollapsingToolbarLayout.apply {
//                    val backgroundColorAnimation = ValueAnimator.ofObject(
//                        ArgbEvaluator(),
//                        PracticeTime.getThemeColor(R.attr.colorSurface, requireContext()),
//                        transparentSurfaceColor
//                    )
//                    backgroundColorAnimation.duration = 500 // milliseconds
//
//                    backgroundColorAnimation.addUpdateListener { animator ->
//                        setBackgroundColor(animator.animatedValue as Int)
//                    }
//                    backgroundColorAnimation.start()
//
//                    val scrimColorAnimation = ValueAnimator.ofObject(
//                        ArgbEvaluator(),
//                        PracticeTime.getThemeColor(R.attr.colorSurface, requireContext()),
//                        transparentSurfaceColor
//                    )
//                    scrimColorAnimation.duration = 340 // milliseconds
//
//                    scrimColorAnimation.addUpdateListener { animator ->
//                        setStatusBarScrimColor(animator.animatedValue as Int)
//                    }
//                    scrimColorAnimation.start()
//                    setContentScrimColor(PracticeTime.getThemeColor(R.attr.backgroundToolbarCollapsed, requireContext()))
//                }
//            }
//        }
//    }
//
//    private fun shortClickOnLibraryItemHandler(index: Int) {
//        // if there are already libraryItems selected,
//        // add or remove the clicked libraryItem from the selection
////        if(selectedItems.isNotEmpty()) {
////            if(selectedItems.removeAt(index)) {
////                libraryItemAdapter?.notifyItemChanged(index)
////                if(selectedItems.size == 1) {
////                    actionMode?.menu?.findItem(R.id.topToolbarSelectionEdit)?.isVisible = true
////                } else if(selectedItems.isEmpty()) {
////                    actionMode?.finish()
////                }
////            } else {
////                longClickOnLibraryItemHandler(index, vibrate = false)
////            }
////            actionMode?.title = "${selectedItems.size} selected"
////        } else {
////            editLibraryItemDialog?.show(libraryItems[index])
////        }
//    }
//
//    // the handler for dealing with long clicks on libraryItem
//    private fun longClickOnLibraryItemHandler(index: Int, vibrate: Boolean = true): Boolean {
//        // if there is no libraryItem selected already, change the toolbar
//        if(selectedItems.isEmpty()) {
//            actionMode = libraryToolbar.startActionMode(actionModeCallback())
//            actionMode?.title = "1 selected"
//        }
//
//        if(!selectedItems.contains(index.toLong())) {
//            if (vibrate && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                (requireContext().getSystemService(
//                    Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
//                        ).defaultVibrator.apply {
//                        cancel()
//                        vibrate(
//                            VibrationEffect.createOneShot(100,100)
//                        )
//                    }
//            }
//            // now add the newly selected libraryItem to the list...
//            selectedItems.add(index.toLong())
//            libraryItemAdapter?.notifyItemChanged(index)
//        }
//
//        actionMode?.menu?.findItem(R.id.topToolbarSelectionEdit)?.isVisible =
//            selectedItems.size == 1
//
//        // we consumed the event so we return true
//        return true
//    }
//
//    private fun clearLibraryItemSelection() {
//        val tmpCopy = selectedItems.toList()
//        selectedItems.clear()
//        tmpCopy.forEach { libraryItemAdapter?.notifyItemChanged(it.toInt()) }
//    }

    // init the toolbar and associated data
//    private fun initToolbar() {
//        libraryToolbar.apply {
//            menu?.clear()
//            setCommonToolbar(requireActivity(), this) {
//                when(it) {
//                    R.id.libraryToolbarSortModeDateAdded -> sortLibraryItemList(LibrarySortMode.DATE_ADDED)
//                    R.id.libraryToolbarSortModeLastModified -> sortLibraryItemList(LibrarySortMode.LAST_MODIFIED)
//                    R.id.libraryToolbarSortModeName -> sortLibraryItemList(LibrarySortMode.NAME)
//                    R.id.libraryToolbarSortModeColor -> sortLibraryItemList(LibrarySortMode.COLOR)
//                    R.id.libraryToolbarSortModeCustom -> sortLibraryItemList(LibrarySortMode.CUSTOM)
//                    else -> {}
//                }
//            }
//            inflateMenu(R.menu.library_toolbar_menu_base)
////            setNavigationIcon(R.drawable.ic_account)
//        }
//    }

//    // the handler for creating new libraryItems
//    private fun addLibraryItemHandler(newLibraryItem: LibraryItem) {
//        lifecycleScope.launch {
//            PracticeTime.libraryItemDao.insertAndGet(newLibraryItem)
//                ?.let { libraryItems.add(0, it) }
//            libraryItemAdapter?.notifyItemInserted(0)
//            if(libraryItems.isNotEmpty()) hideHint()
//        }
//    }
//
//    // the handler for editing libraryItems
//    private fun editLibraryItemHandler(libraryItem: LibraryItem) {
//        lifecycleScope.launch {
//            PracticeTime.libraryItemDao.update(libraryItem)
//            libraryItems.indexOfFirst { c -> c.id == libraryItem.id }.also { i ->
//                assert(i != -1) {
//                    Log.e("EDIT_CATEGORY", "No libraryItem with matching id found for\n$libraryItem")
//                }
//                libraryItems[i] = libraryItem
//                libraryItemAdapter?.notifyItemChanged(i)
//            }
//            actionMode?.finish()
//        }
//    }
//
//    // the handler for deleting libraryItems
//    private fun deleteLibraryItemsHandler() {
//        var failedDeleteFlag = false
//        lifecycleScope.launch {
//            selectedItems.sortedByDescending { it }.forEach { index ->
//                if(PracticeTime.libraryItemDao.archive(libraryItems[index.toInt()].id)) {
//                    libraryItems.removeAt(index.toInt())
//                    libraryItemAdapter?.notifyItemRemoved(index.toInt())
//                } else {
//                    failedDeleteFlag = true
//                }
//            }
//
//            if(failedDeleteFlag) {
//                Snackbar.make(
//                    requireView(),
//                    if(selectedItems.size > 1) R.string.deleteLibraryItemsFailSnackbar
//                    else R.string.deleteLibraryItemFailSnackbar,
//                    5000
//                ).show()
//            } else {
//                Toast.makeText(
//                    requireActivity(),
//                    if(selectedItems.size > 1) R.string.deleteLibraryItemsSuccessSnackbar
//                    else R.string.deleteLibraryItemSuccessSnackbar,
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//
//            if(libraryItems.isEmpty()) showHint()
//            actionMode?.finish()
//        }
//    }

//    private fun showHint() {
//        requireView().apply {
//            findViewById<TextView>(R.id.libraryHint).visibility = View.VISIBLE
////            findViewById<RecyclerView>(R.id.libraryLibraryItemList).visibility = View.GONE
//        }
//    }
//
//    private fun hideHint() {
//        requireView().apply {
//            findViewById<TextView>(R.id.libraryHint).visibility = View.GONE
////            findViewById<RecyclerView>(R.id.libraryLibraryItemList).visibility = View.VISIBLE
//        }
//    }
//}