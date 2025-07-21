/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-2025 Matthias Emde, Michael Prommersberger
 */

package app.musikus.library.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import app.musikus.R
import app.musikus.core.data.LibraryFolderWithItems
import app.musikus.core.data.UUIDConverter
import app.musikus.core.domain.DateFormat
import app.musikus.core.domain.TimeProvider
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.random.Random

@Composable
fun LibraryFolderComponentLarge(
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
        ElevatedCard(
            Modifier.size(150.dp),
            elevation = CardDefaults.cardElevation(0.dp),
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

@Composable
fun LibraryFoldersSwipeRow(
    modifier: Modifier = Modifier,
    folders: ImmutableList<LibraryFolderWithItems>,
    showBadge: Boolean = true,
    highlightedFolderId: UUID?,
    folderWithBadge: UUID?,
    onFolderSelected: (UUID?) -> Unit,
    includeRootFolder: Boolean = true,
) {
    // translate highlightedFolderId to selectedTabIndex
    val selectedTabIndex = remember(highlightedFolderId) {
        // since the first tab is the "no folder" tab, add +1 (note: indexOfFirst returns -1 if not found)
        folders.indexOfFirst { it.folder.id == highlightedFolderId } + if (includeRootFolder) 1 else 0
    }

    ScrollableTabRow(
        modifier = modifier.fillMaxWidth(),
        selectedTabIndex = selectedTabIndex,
        containerColor = colorScheme.surfaceContainerLow, // match color of ModalBottomSheet
        divider = { }
    ) {
        if (includeRootFolder) {
            LibraryFolderComponentSmall(
                folder = null,
                onClick = { onFolderSelected(null) },
                isSelected = highlightedFolderId == null,
                showBadge = showBadge && folderWithBadge == null
            )
        }

        folders.forEach { folder ->
            LibraryFolderComponentSmall(
                folder = folder.folder,
                onClick = { onFolderSelected(folder.folder.id) },
                isSelected = folder.folder.id == highlightedFolderId,
                showBadge = showBadge && folder.folder.id == folderWithBadge
            )
        }
    }
}

@Composable
fun LibraryFolderComponentSmall(
    folder: LibraryFolder?,
    onClick: (LibraryFolder?) -> Unit,
    isSelected: Boolean,
    showBadge: Boolean = false,
) {
    val textColor = if (isSelected) {
        colorScheme.primary
    } else {
        colorScheme.onSurfaceVariant
    }

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) {
            colorScheme.primary
        } else {
            colorScheme.onSurfaceVariant
        },
        label = "color",
        animationSpec = tween(200)
    )

    Tab(
        modifier = Modifier.size(70.dp),
        selected = isSelected,
        onClick = { onClick(folder) },
    ) {
        BadgedBox(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small),
            badge = { if (showBadge) Badge() }
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Filled.Folder else Icons.Outlined.Folder,
                tint = iconColor,
                contentDescription = null
            )
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
        Text(
            modifier = Modifier
                .padding(horizontal = MaterialTheme.spacing.small)
                .basicMarquee(),
            text = folder?.name ?: stringResource(id = R.string.active_session_library_folder_element_default),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
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
    disabledItems: Set<UUID> = emptySet(),
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
                    sortModes = LibraryItemSortMode.entries,
                    currentSortMode = sortMenuUiState.mode,
                    currentSortDirection = sortMenuUiState.direction,
                    sortItemDescription = stringResource(id = R.string.library_content_items_sort_menu_description),
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
            modifier = Modifier.animateItem()
        ) {
            LibraryItemComponent(
                item = item,
                lastPracticedDate = lastPracticedDate,
                selected = item.id in uiState.selectedItemIds,
                onShortClick = { libraryCoreEventHandler(LibraryCoreUiEvent.ItemPressed(item, longClick = false)) },
                onLongClick = { libraryCoreEventHandler(LibraryCoreUiEvent.ItemPressed(item, longClick = true)) },
                enabled = item.id !in disabledItems
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

/** --------------------------------- Previews --------------------------------- */

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

@PreviewLightDark
@Composable
private fun PreviewLibraryRow(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme = theme) {
        LibraryFoldersSwipeRow(
            folders = dummyFolders.toImmutableList(),
            highlightedFolderId = dummyFolders.first().folder.id,
            folderWithBadge = dummyFolders.toList()[2].folder.id,
            onFolderSelected = {}
        )
    }
}


val dummyFolders = (0..10).asSequence().map {
    LibraryFolderWithItems(
        folder = LibraryFolder(
            id = UUIDConverter.fromInt(it),
            customOrder = null,
            name = LoremIpsum(Random.nextInt(1, 5)).values.first(),
            modifiedAt = TimeProvider.uninitializedDateTime,
            createdAt = TimeProvider.uninitializedDateTime
        ),
        items = emptyList()
    )
}

val dummyLibraryItems = (1..20).asSequence().map {
    LibraryItem(
        id = UUIDConverter.fromInt(it),
        createdAt = TimeProvider.uninitializedDateTime,
        modifiedAt = TimeProvider.uninitializedDateTime,
        name = LoremIpsum(Random.nextInt(1, 10)).values.first(),
        colorIndex = it % libraryItemColors.size,
        customOrder = null,
        libraryFolderId = null
    )
}
