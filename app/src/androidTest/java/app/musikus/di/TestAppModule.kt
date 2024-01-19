/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.di

import android.app.Application
import androidx.room.Room
import app.musikus.database.MusikusDatabase
import app.musikus.repository.FakeUserPreferencesRepository
import app.musikus.repository.GoalRepository
import app.musikus.repository.GoalRepositoryImpl
import app.musikus.repository.LibraryRepository
import app.musikus.repository.LibraryRepositoryImpl
import app.musikus.repository.SessionRepository
import app.musikus.repository.SessionRepositoryImpl
import app.musikus.repository.UserPreferencesRepository
import app.musikus.usecase.goals.AddGoalUseCase
import app.musikus.usecase.goals.ArchiveGoalsUseCase
import app.musikus.usecase.goals.CleanFutureGoalInstancesUseCase
import app.musikus.usecase.goals.DeleteGoalsUseCase
import app.musikus.usecase.goals.EditGoalUseCase
import app.musikus.usecase.goals.GetAllGoalsUseCase
import app.musikus.usecase.goals.GoalsUseCases
import app.musikus.usecase.goals.PauseGoalsUseCase
import app.musikus.usecase.goals.RestoreGoalsUseCase
import app.musikus.usecase.goals.SelectGoalSortModeUseCase
import app.musikus.usecase.goals.SortGoalsUseCase
import app.musikus.usecase.goals.UnpauseGoalsUseCase
import app.musikus.usecase.goals.UpdateGoalsUseCase
import app.musikus.usecase.library.AddFolderUseCase
import app.musikus.usecase.library.AddItemUseCase
import app.musikus.usecase.library.DeleteFoldersUseCase
import app.musikus.usecase.library.DeleteItemsUseCase
import app.musikus.usecase.library.EditFolderUseCase
import app.musikus.usecase.library.EditItemUseCase
import app.musikus.usecase.library.GetFolderSortInfoUseCase
import app.musikus.usecase.library.GetFoldersUseCase
import app.musikus.usecase.library.GetItemSortInfoUseCase
import app.musikus.usecase.library.GetItemsUseCase
import app.musikus.usecase.library.LibraryUseCases
import app.musikus.usecase.library.RestoreFoldersUseCase
import app.musikus.usecase.library.RestoreItemsUseCase
import app.musikus.usecase.library.SelectFolderSortModeUseCase
import app.musikus.usecase.library.SelectItemSortModeUseCase
import app.musikus.utils.FakeIdProvider
import app.musikus.utils.FakeTimeProvider
import app.musikus.utils.IdProvider
import app.musikus.utils.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TestAppModule {

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider {
        return FakeTimeProvider()
    }

    @Provides
    fun provideFakeTimeProvider(
        timeProvider: TimeProvider
    ) : FakeTimeProvider {
        return timeProvider as FakeTimeProvider
    }

    @Provides
    @Singleton
    fun provideIdProvider(): IdProvider {
        return FakeIdProvider()
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(): UserPreferencesRepository {
        return FakeUserPreferencesRepository()
    }


    @Provides
    @Singleton
    @Named("test_db")
    fun provideMusikusDatabase(
        app: Application,
        timeProvider: TimeProvider,
        idProvider: IdProvider
    ): MusikusDatabase {
        return Room.inMemoryDatabaseBuilder(
            app,
            MusikusDatabase::class.java
        ).build().apply {
            this.timeProvider = timeProvider
            this.idProvider = idProvider
        }
    }

    @Provides
    fun provideLibraryRepository(
        @Named("test_db") database: MusikusDatabase
    ): LibraryRepository {
        return LibraryRepositoryImpl(
            itemDao = database.libraryItemDao,
            folderDao = database.libraryFolderDao,
        )
    }

    @Provides
    fun provideGoalRepository(
        @Named("test_db") database: MusikusDatabase
    ): GoalRepository {
        return GoalRepositoryImpl(database)
    }

    @Provides
    fun provideSessionRepository(
        @Named("test_db") database: MusikusDatabase,
        timeProvider: TimeProvider
    ): SessionRepository {
        return SessionRepositoryImpl(
            timeProvider = timeProvider,
            sessionDao = database.sessionDao,
            sectionDao = database.sectionDao,
        )
    }

    @Provides
    fun provideLibraryUseCases(
        libraryRepository: LibraryRepository,
        userPreferencesRepository: UserPreferencesRepository
    ): LibraryUseCases {
        return LibraryUseCases(
            getItems = GetItemsUseCase(libraryRepository, userPreferencesRepository),
            getFolders = GetFoldersUseCase(libraryRepository, userPreferencesRepository),
            addItem = AddItemUseCase(libraryRepository),
            addFolder = AddFolderUseCase(libraryRepository),
            editItem = EditItemUseCase(libraryRepository),
            editFolder = EditFolderUseCase(libraryRepository),
            deleteItems = DeleteItemsUseCase(libraryRepository),
            deleteFolders = DeleteFoldersUseCase(libraryRepository),
            restoreItems = RestoreItemsUseCase(libraryRepository),
            restoreFolders = RestoreFoldersUseCase(libraryRepository),

            getItemSortInfo = GetItemSortInfoUseCase(userPreferencesRepository),
            selectItemSortMode = SelectItemSortModeUseCase(userPreferencesRepository),
            getFolderSortInfo = GetFolderSortInfoUseCase(userPreferencesRepository),
            selectFolderSortMode = SelectFolderSortModeUseCase(userPreferencesRepository),
        )
    }

    @Provides
    fun provideGoalsUseCases(
        goalRepository: GoalRepository,
        libraryRepository: LibraryRepository,
        userPreferencesRepository: UserPreferencesRepository,
        timeProvider: TimeProvider
    ): GoalsUseCases {
        val cleanFutureGoalInstancesUseCase = CleanFutureGoalInstancesUseCase(goalRepository, timeProvider)
        val archiveGoalsUseCase = ArchiveGoalsUseCase(goalRepository)
        val sortGoalsUseCase = SortGoalsUseCase(userPreferencesRepository)

        return GoalsUseCases(
            getAll = GetAllGoalsUseCase(goalRepository, sortGoalsUseCase),
            add = AddGoalUseCase(goalRepository, libraryRepository, timeProvider),
            pause = PauseGoalsUseCase(goalRepository, cleanFutureGoalInstancesUseCase),
            unpause = UnpauseGoalsUseCase(goalRepository),
            archive = archiveGoalsUseCase,
            update = UpdateGoalsUseCase(
                goalRepository = goalRepository,
                archiveGoals = archiveGoalsUseCase,
                timeProvider = timeProvider,
            ),
            edit = EditGoalUseCase(goalRepository, cleanFutureGoalInstancesUseCase),
            delete = DeleteGoalsUseCase(goalRepository),
            restore = RestoreGoalsUseCase(goalRepository),
            selectSortMode = SelectGoalSortModeUseCase(userPreferencesRepository),
        )
    }
}