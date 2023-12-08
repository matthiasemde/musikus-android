/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.usecase.library

data class LibraryUseCases(
    val getItems: GetItemsUseCase,
    val getFolders: GetFoldersUseCase,
    val addItem: AddItemUseCase,
    val addFolder: AddFolderUseCase,
    val editItem: EditItemUseCase,
    val editFolder: EditFolderUseCase,
    val deleteItems: DeleteItemsUseCase,
    val deleteFolders: DeleteFoldersUseCase,
    val restoreItems: RestoreItemsUseCase,
    val restoreFolders: RestoreFoldersUseCase,

    val getItemSortInfo: GetItemSortInfoUseCase,
    val selectItemSortMode: SelectItemSortModeUseCase,
    val getFolderSortInfo: GetFolderSortInfoUseCase,
    val selectFolderSortMode: SelectFolderSortModeUseCase,
)