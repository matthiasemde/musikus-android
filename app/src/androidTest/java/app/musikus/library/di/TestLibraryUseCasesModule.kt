/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.library.di

import app.musikus.library.domain.LibraryRepository
import app.musikus.library.domain.usecase.AddFolderUseCase
import app.musikus.library.domain.usecase.AddItemUseCase
import app.musikus.library.domain.usecase.DeleteFoldersUseCase
import app.musikus.library.domain.usecase.DeleteItemsUseCase
import app.musikus.library.domain.usecase.EditFolderUseCase
import app.musikus.library.domain.usecase.EditItemUseCase
import app.musikus.library.domain.usecase.GetAllLibraryItemsUseCase
import app.musikus.library.domain.usecase.GetLastPracticedDateUseCase
import app.musikus.library.domain.usecase.GetSortedLibraryFoldersUseCase
import app.musikus.library.domain.usecase.GetSortedLibraryItemsUseCase
import app.musikus.library.domain.usecase.LibraryUseCases
import app.musikus.library.domain.usecase.RestoreFoldersUseCase
import app.musikus.library.domain.usecase.RestoreItemsUseCase
import app.musikus.sessions.domain.SessionRepository
import app.musikus.settings.domain.usecase.UserPreferencesUseCases
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
        userPreferencesUseCases: UserPreferencesUseCases
    ): LibraryUseCases {
        return LibraryUseCases(
            getAllItems = GetAllLibraryItemsUseCase(libraryRepository),
            getSortedItems = GetSortedLibraryItemsUseCase(libraryRepository, userPreferencesUseCases.getItemSortInfo),
            getSortedFolders = GetSortedLibraryFoldersUseCase(libraryRepository, userPreferencesUseCases.getFolderSortInfo),
            getLastPracticedDate = GetLastPracticedDateUseCase(sessionRepository),
            addItem = AddItemUseCase(libraryRepository),
            addFolder = AddFolderUseCase(libraryRepository),
            editItem = EditItemUseCase(libraryRepository),
            editFolder = EditFolderUseCase(libraryRepository),
            deleteItems = DeleteItemsUseCase(libraryRepository),
            deleteFolders = DeleteFoldersUseCase(libraryRepository),
            restoreItems = RestoreItemsUseCase(libraryRepository),
            restoreFolders = RestoreFoldersUseCase(libraryRepository),
        )
    }

}