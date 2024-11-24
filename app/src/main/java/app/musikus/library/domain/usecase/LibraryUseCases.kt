/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde
 */

package app.musikus.library.domain.usecase

data class LibraryUseCases(
    val getAllItems: GetAllLibraryItemsUseCase,
    val getSortedItems: GetSortedLibraryItemsUseCase,
    val getSortedFolders: GetSortedLibraryFoldersUseCase,
    val getLastPracticedDate: GetLastPracticedDateUseCase,
    val addItem: AddItemUseCase,
    val addFolder: AddFolderUseCase,
    val editItem: EditItemUseCase,
    val editFolder: EditFolderUseCase,
    val deleteItems: DeleteItemsUseCase,
    val deleteFolders: DeleteFoldersUseCase,
    val restoreItems: RestoreItemsUseCase,
    val restoreFolders: RestoreFoldersUseCase,
    val getFolderSortInfo: GetFolderSortInfoUseCase,
    val getItemSortInfo: GetItemSortInfoUseCase,
    val selectFolderSortMode: SelectFolderSortModeUseCase,
    val selectItemSortMode: SelectItemSortModeUseCase,
)
