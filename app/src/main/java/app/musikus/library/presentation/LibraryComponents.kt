/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-2025 Matthias Emde
 */

package app.musikus.library.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import app.musikus.R
import app.musikus.core.data.UUIDConverter
import app.musikus.core.domain.DateFormat
import app.musikus.core.domain.musikusFormat
import app.musikus.core.presentation.MainUiEvent
import app.musikus.core.presentation.MainUiEventHandler
import app.musikus.core.presentation.components.DeleteConfirmationBottomSheet
import app.musikus.core.presentation.components.Selectable
import app.musikus.core.presentation.components.SortMenu
import app.musikus.core.presentation.theme.MusikusColorSchemeProvider
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.core.presentation.theme.libraryItemColors
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.UiIcon
import app.musikus.core.presentation.utils.UiText
import app.musikus.library.data.LibraryItemSortMode
import app.musikus.library.data.daos.LibraryFolder
import app.musikus.library.data.daos.LibraryItem
import app.musikus.menu.domain.ColorSchemeSelections
import java.time.ZonedDateTime

@Composable
fun LibraryFolderComponent(
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
        ElevatedCard(Modifier.size(150.dp)) {
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
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
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
fun LibraryItemComponent(
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
                .padding(
                    vertical = MaterialTheme.spacing.small,
                    horizontal = MaterialTheme.spacing.extraLarge
                )
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .alpha(if (!enabled) 0.5f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .width(10.dp)
                    .fillMaxHeight()
                    .padding(vertical = 2.dp),
                shape = MaterialTheme.shapes.small,
                color = libraryItemColors[item.colorIndex]
            ) { }
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.libraryItemsComponent(
    uiState: LibraryItemsUiState,
    libraryCoreEventHandler: LibraryCoreUiEventHandler,
    showHeader: Boolean = true,
) {
    if (showHeader) {
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

                val sortMenuUiState = uiState.sortMenuUiState
                SortMenu(
                    show = sortMenuUiState.show,
                    sortModes = LibraryItemSortMode.entries,
                    currentSortMode = sortMenuUiState.mode,
                    currentSortDirection = sortMenuUiState.direction,
                    sortItemDescription = stringResource(id = R.string.library_content_items_sort_menu_description),
                    onShowMenuChanged = { libraryCoreEventHandler(LibraryCoreUiEvent.ItemSortMenuPressed) },
                    onSelectionHandler = {
                        libraryCoreEventHandler(LibraryCoreUiEvent.ItemSortModeSelected(it as LibraryItemSortMode))
                    }
                )
            }
        }
    }
    items(
        items = uiState.itemsWithLastPracticedDate,
        key = { (item, _) -> item.id }
    ) { (item, lastPracticedDate) ->
        Box(
            modifier = Modifier.animateItemPlacement()
        ) {
            LibraryItemComponent(
                item = item,
                lastPracticedDate = lastPracticedDate,
                selected = item.id in uiState.selectedItemIds,
                onShortClick = { libraryCoreEventHandler(LibraryCoreUiEvent.ItemPressed(item, longClick = false)) },
                onLongClick = { libraryCoreEventHandler(LibraryCoreUiEvent.ItemPressed(item, longClick = true)) }
            )
        }
    }
}

@Composable
fun LibraryDeleteDialog(
    uiState: LibraryDeleteDialogUiState,
    libraryCoreUiEventHandler: LibraryCoreUiEventHandler,
    mainEventHandler: MainUiEventHandler,
) {
    val snackbarMessage = stringResource(id = R.string.library_screen_snackbar_deleted)

    val foldersSelected = uiState.numberOfSelectedFolders > 0
    val itemsSelected = uiState.numberOfSelectedItems > 0

    val totalSelections =
        uiState.numberOfSelectedFolders + uiState.numberOfSelectedItems

    DeleteConfirmationBottomSheet(
        explanation =
        if (foldersSelected && itemsSelected) {
            UiText.StringResource(
                R.string.library_screen_deletion_dialog_explanation_both,
                uiState.numberOfSelectedFolders,
                pluralStringResource(
                    id = R.plurals.library_folder,
                    uiState.numberOfSelectedFolders
                ),
                uiState.numberOfSelectedItems,
                pluralStringResource(
                    id = R.plurals.library_item,
                    uiState.numberOfSelectedItems
                )
            )
        } else {
            UiText.PluralResource(
                R.plurals.library_screen_deletion_dialog_explanation,
                totalSelections,
                totalSelections,
                if (foldersSelected) {
                    pluralStringResource(
                        id = R.plurals.library_folder,
                        uiState.numberOfSelectedFolders
                    )
                } else {
                    pluralStringResource(
                        id = R.plurals.library_item,
                        uiState.numberOfSelectedItems
                    )
                }
            )
        },
        confirmationIcon = UiIcon.DynamicIcon(Icons.Default.Delete),
        confirmationText = UiText.StringResource(
            R.string.library_screen_deletion_dialog_confirm,
            totalSelections
        ),
        onDismiss = { libraryCoreUiEventHandler(LibraryCoreUiEvent.DeleteDialogDismissed) },
        onConfirm = {
            libraryCoreUiEventHandler(LibraryCoreUiEvent.DeleteDialogConfirmed)
            mainEventHandler(
                MainUiEvent.ShowSnackbar(
                    message = snackbarMessage,
                    onUndo = { libraryCoreUiEventHandler(LibraryCoreUiEvent.RestoreButtonPressed) }
                )
            )
        }
    )
}

@Composable
fun LibraryDialogs(
    uiState: LibraryDialogsUiState,
    libraryCoreEventHandler: LibraryCoreUiEventHandler,
    mainEventHandler: MainUiEventHandler,
) {
    uiState.folderDialogUiState?.let {
        LibraryFolderDialog(
            uiState = it,
            eventHandler = { libraryCoreEventHandler(LibraryCoreUiEvent.FolderDialogUiEvent(it)) }
        )
    }

    uiState.itemDialogUiState?.let {
        LibraryItemDialog(
            uiState = it,
            eventHandler = { libraryCoreEventHandler(LibraryCoreUiEvent.ItemDialogUiEvent(it)) }
        )
    }

    uiState.deleteDialogUiState?.let {
        LibraryDeleteDialog(
            uiState = it,
            libraryCoreUiEventHandler = libraryCoreEventHandler,
            mainEventHandler = mainEventHandler
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewLibraryItem(
    @PreviewParameter(MusikusColorSchemeProvider::class) colorScheme: ColorSchemeSelections
) {
    MusikusThemedPreview(colorScheme) {
        LibraryItemComponent(
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
