/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.library.di

import app.musikus.core.domain.UserPreferencesRepository
import app.musikus.library.domain.LibraryRepository
import app.musikus.library.domain.usecase.AddFolderUseCase
import app.musikus.library.domain.usecase.AddItemUseCase
import app.musikus.library.domain.usecase.DeleteFoldersUseCase
import app.musikus.library.domain.usecase.DeleteItemsUseCase
import app.musikus.library.domain.usecase.EditFolderUseCase
import app.musikus.library.domain.usecase.EditItemUseCase
import app.musikus.library.domain.usecase.GetAllLibraryItemsUseCase
import app.musikus.library.domain.usecase.GetFolderSortInfoUseCase
import app.musikus.library.domain.usecase.GetItemSortInfoUseCase
import app.musikus.library.domain.usecase.GetLastPracticedDateUseCase
import app.musikus.library.domain.usecase.GetSortedLibraryFoldersUseCase
import app.musikus.library.domain.usecase.GetSortedLibraryItemsUseCase
import app.musikus.library.domain.usecase.LibraryUseCases
import app.musikus.library.domain.usecase.RestoreFoldersUseCase
import app.musikus.library.domain.usecase.RestoreItemsUseCase
import app.musikus.library.domain.usecase.SelectFolderSortModeUseCase
import app.musikus.library.domain.usecase.SelectItemSortModeUseCase
import app.musikus.sessions.domain.SessionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [LibraryUseCasesModule::class]
)
object TestLibraryUseCasesModule {
    @Provides
    fun provideLibraryUseCases(
        libraryRepository: LibraryRepository,
        sessionRepository: SessionRepository,
        userPreferencesRepository: UserPreferencesRepository
    ): LibraryUseCases {
        val getItemSortInfo = GetItemSortInfoUseCase(userPreferencesRepository)
        val getFolderSortInfo = GetFolderSortInfoUseCase(userPreferencesRepository)

        return LibraryUseCases(
            getAllItems = GetAllLibraryItemsUseCase(libraryRepository),
            getSortedItems = GetSortedLibraryItemsUseCase(libraryRepository, getItemSortInfo),
            getSortedFolders = GetSortedLibraryFoldersUseCase(libraryRepository, getFolderSortInfo),
            getLastPracticedDate = GetLastPracticedDateUseCase(sessionRepository),
            addItem = AddItemUseCase(libraryRepository),
            addFolder = AddFolderUseCase(libraryRepository),
            editItem = EditItemUseCase(libraryRepository),
            editFolder = EditFolderUseCase(libraryRepository),
            deleteItems = DeleteItemsUseCase(libraryRepository),
            deleteFolders = DeleteFoldersUseCase(libraryRepository),
            restoreItems = RestoreItemsUseCase(libraryRepository),
            restoreFolders = RestoreFoldersUseCase(libraryRepository),
            getItemSortInfo = getItemSortInfo,
            getFolderSortInfo = getFolderSortInfo,
            selectItemSortMode = SelectItemSortModeUseCase(userPreferencesRepository),
            selectFolderSortMode = SelectFolderSortModeUseCase(userPreferencesRepository)
        )
    }
}