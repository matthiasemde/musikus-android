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
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.musikus.R
import app.musikus.core.data.UUIDConverter
import app.musikus.core.domain.SortDirection
import app.musikus.core.presentation.components.DialogActions
import app.musikus.core.presentation.components.DialogHeader
import app.musikus.core.presentation.components.SortMenu
import app.musikus.core.presentation.theme.MusikusColorSchemeProvider
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.UiIcon
import app.musikus.library.data.LibraryFolderSortMode
import app.musikus.library.data.LibraryItemSortMode
import app.musikus.library.presentation.LibraryCoreUiEvent
import app.musikus.library.presentation.LibraryFoldersSortMenuUiState
import app.musikus.library.presentation.LibraryFoldersSwipeRow
import app.musikus.library.presentation.LibraryFoldersUiState
import app.musikus.library.presentation.LibraryItemsSortMenuUiState
import app.musikus.library.presentation.LibraryItemsUiState
import app.musikus.library.presentation.LibraryUiEvent
import app.musikus.library.presentation.LibraryUiEventHandler
import app.musikus.library.presentation.dummyFolders
import app.musikus.library.presentation.dummyLibraryItems
import app.musikus.library.presentation.libraryItemsComponent
import app.musikus.menu.domain.ColorSchemeSelections
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
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

    var selectedFolder: UUID? by rememberSaveable { mutableStateOf(_uiState.runningItem?.libraryFolderId) }

    var sortDialogShown: Boolean by rememberSaveable { mutableStateOf(false) }

    // selectedFolder has to be tied to ViewModel state to be consistent with displayed items
    LaunchedEffect(selectedFolder) {
        eventHandler(LibraryUiEvent.FolderPressed(selectedFolder, longClick = false))
    }

    // show sorting dialog when requested
    if (sortDialogShown) {
        LibrarySortingDialog(
            onDismiss = { sortDialogShown = false },
            eventHandler = eventHandler,
            libraryFoldersSortMenuUiState = _uiState.libraryFoldersUiState.sortMenuUiState,
            libraryItemsSortMenuUiState = _uiState.libraryItemsUiState.sortMenuUiState
        )
    }

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

            OutlinedButton(
                onClick = { sortDialogShown = true },
            ) {
                Text(
                    text = stringResource(R.string.active_session_new_item_selector_sort_menu_button),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

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
                    { folderId ->
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
                showHeader = false,
                uiState = _uiState.libraryItemsUiState,
                libraryCoreEventHandler = {
                    if (it is LibraryCoreUiEvent.ItemPressed && !it.longClick) {
                        onClose() // close bottom sheet when an item is selected
                    }
                    eventHandler(LibraryUiEvent.CoreUiEvent(it))
                },
            )
        }

    }
}

@Composable
private fun LibrarySortingDialog(
    onDismiss: () -> Unit,
    eventHandler: LibraryUiEventHandler,
    libraryItemsSortMenuUiState: LibraryItemsSortMenuUiState,
    libraryFoldersSortMenuUiState: LibraryFoldersSortMenuUiState
) {

    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column {
                DialogHeader(
                    title = stringResource(id = R.string.active_session_new_item_selector_sort_menu_title),
                    icon = UiIcon.DynamicIcon(Icons.Default.SwapVert)
                )

                Spacer(Modifier.height(MaterialTheme.spacing.medium))
                DialogSortMenuRow(stringResource(R.string.active_session_new_item_selector_sort_menu_folders)) {
                    SortMenu(
                        sortItemDescription = stringResource(R.string.active_session_new_item_selector_sort_menu_folders),
                        sortModes = LibraryFolderSortMode.entries,
                        currentSortMode = libraryFoldersSortMenuUiState.mode,
                        currentSortDirection = libraryFoldersSortMenuUiState.direction,
                        onSelectionHandler = {
                            eventHandler(LibraryUiEvent.FolderSortModeSelected(it as LibraryFolderSortMode))
                        }
                    )
                }

                Spacer(Modifier.height(MaterialTheme.spacing.large))
                DialogSortMenuRow(stringResource(R.string.active_session_new_item_selector_sort_menu_items)) {
                    SortMenu(
                        sortItemDescription = stringResource(R.string.active_session_new_item_selector_sort_menu_items),
                        sortModes = LibraryItemSortMode.entries,
                        currentSortMode = libraryItemsSortMenuUiState.mode,
                        currentSortDirection = libraryItemsSortMenuUiState.direction,
                        onSelectionHandler = {
                            eventHandler(
                                LibraryUiEvent.CoreUiEvent(LibraryCoreUiEvent.ItemSortModeSelected(it as LibraryItemSortMode))
                            )
                        }
                    )
                }

                DialogActions(
                    dismissButtonText = stringResource(id = R.string.core_dialog_close),
                    onDismissHandler = onDismiss,
                    onConfirmHandler = {},
                    confirmButtonVisible = false

                )
            }
        }
    }
}

@Composable
private fun DialogSortMenuRow(
    text: String,
    content: @Composable () -> Unit,
) {
    Row (Modifier.padding(horizontal = MaterialTheme.spacing.medium)) {
        Text(
            text,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .fillMaxWidth(0.4f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.width(MaterialTheme.spacing.medium))

        Surface(
            Modifier,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            content()
        }
    }
}

/** Previews */

@PreviewLightDark
@Composable
private fun PreviewLibrarySortingDialog(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections
) {
    MusikusThemedPreview(theme = theme) {
        LibrarySortingDialog(
            onDismiss = {},
            eventHandler = {false},
            libraryFoldersSortMenuUiState = LibraryFoldersSortMenuUiState(
                mode = LibraryFolderSortMode.DEFAULT,
                direction = SortDirection.DEFAULT
            ),
            libraryItemsSortMenuUiState = LibraryItemsSortMenuUiState(
                mode = LibraryItemSortMode.DEFAULT,
                direction = SortDirection.DEFAULT
            )
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewNewItemSelector(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme) {
        Surface (color = MaterialTheme.colorScheme.surfaceContainer) {
            NewItemSelectorLayout(
                uiState = remember {
                    mutableStateOf(
                        NewItemSelectorUiState(
                            runningItem = dummyLibraryItems.first().copy(
                                libraryFolderId = UUIDConverter.fromInt(1)
                            ),
                            libraryFoldersUiState = LibraryFoldersUiState(
                                foldersWithItems = dummyFolders.toImmutableList(),
                                sortMenuUiState = LibraryFoldersSortMenuUiState(
                                    mode = LibraryFolderSortMode.DEFAULT,
                                    direction = SortDirection.DEFAULT
                                ),
                                selectedFolderIds = setOf(null),
                            ),
                            libraryItemsUiState = LibraryItemsUiState(
                                itemsWithLastPracticedDate = dummyLibraryItems.map {
                                    it to ZonedDateTime.now()
                                }.toImmutableList(),
                                selectedItemIds = emptySet(),
                                sortMenuUiState = LibraryItemsSortMenuUiState(
                                    mode = LibraryItemSortMode.DEFAULT,
                                    direction = SortDirection.DEFAULT
                                )
                            )
                        )
                    )
                },
                eventHandler = { false }
            )
        }
    }
}



//@PreviewLightDark
//@Composable
//private fun PreviewNewItemSelectorNoFolders(
//    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
//) {
//    MusikusThemedPreview(theme) {
//        Surface (color = MaterialTheme.colorScheme.surfaceContainer) {
//            NewItemSelectorLayout(
//                uiState = remember {
//                    mutableStateOf(
//                        NewItemSelectorUiState(
//                            runningItem = dummyLibraryItems.first().copy(
//                                libraryFolderId = UUIDConverter.fromInt(1)
//                            ),
//                            libraryFoldersUiState = L(null),
//                                sortMenuUiState = LibraryFoldersSortMenuUiState(
//                                    mode = LibraryFolderSortMode.DEFAULT,
//                                    direction = SortDirection.DEFAULT
//                                ),
//                                selectedFolderIds = setOf(null),
//                            ),
//                            libraryItemsUiState = LibraryItemsUiState(
//                                itemsWithLastPracticedDate = dummyLibraryItems.map {
//                                    it to ZonedDateTime.now()
//                                }.toImmutableList(),
//                                selectedItemIds = emptySet(),
//                                sortMenuUiState = LibraryItemsSortMenuUiState(
//                                    mode = LibraryItemSortMode.DEFAULT,
//                                    direction = SortDirection.DEFAULT
//                                )
//                            )
//                        )
//                    )
//                },
//                eventHandler = { false }
//            )
//        }
//    }
//}
//
//
//@PreviewLightDark
//@Composable
//private fun PreviewNewItemSelectorOneFolder() {
//    MusikusThemedPreview {
//        Column {
//            NewItemSelector(
//                uiState = remember {
//                    mutableStateOf(
//                        NewItemSelectorUiState(
//                            foldersWithItems = dummyFolders.take(1).map {
//                                LibraryFolderWithItems(it, dummyLibraryItems.toList())
//                            }.toList(),
//                            runningItem = dummyLibraryItems.first(),
//                            rootItems = dummyLibraryItems.toList(),
//                            lastPracticedDates = emptyMap(),
//                        )
//                    )
//                },
//                onItemSelected = { }
//            )
//        }
//    }
//}
//
//


