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
import androidx.room.withTransaction
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
import app.musikus.usecase.goals.GetCurrentGoalsUseCase
import app.musikus.usecase.goals.GetLastFiveCompletedGoalsUseCase
import app.musikus.usecase.goals.GoalsUseCases
import app.musikus.usecase.goals.PauseGoalsUseCase
import app.musikus.usecase.goals.RestoreGoalsUseCase
import app.musikus.usecase.goals.SortGoalsUseCase
import app.musikus.usecase.goals.UnarchiveGoalsUseCase
import app.musikus.usecase.goals.UnpauseGoalsUseCase
import app.musikus.usecase.goals.UpdateGoalsUseCase
import app.musikus.usecase.library.AddFolderUseCase
import app.musikus.usecase.library.AddItemUseCase
import app.musikus.usecase.library.DeleteFoldersUseCase
import app.musikus.usecase.library.DeleteItemsUseCase
import app.musikus.usecase.library.EditFolderUseCase
import app.musikus.usecase.library.EditItemUseCase
import app.musikus.usecase.library.GetFoldersUseCase
import app.musikus.usecase.library.GetItemsUseCase
import app.musikus.usecase.library.LibraryUseCases
import app.musikus.usecase.library.RestoreFoldersUseCase
import app.musikus.usecase.library.RestoreItemsUseCase
import app.musikus.usecase.sessions.AddSessionUseCase
import app.musikus.usecase.sessions.DeleteSessionsUseCase
import app.musikus.usecase.sessions.EditSessionUseCase
import app.musikus.usecase.sessions.GetAllSessionsUseCase
import app.musikus.usecase.sessions.GetSessionsInTimeframeUseCase
import app.musikus.usecase.sessions.RestoreSessionsUseCase
import app.musikus.usecase.sessions.SessionsUseCases
import app.musikus.usecase.userpreferences.GetFolderSortInfoUseCase
import app.musikus.usecase.userpreferences.GetGoalSortInfoUseCase
import app.musikus.usecase.userpreferences.GetItemSortInfoUseCase
import app.musikus.usecase.userpreferences.SelectFolderSortModeUseCase
import app.musikus.usecase.userpreferences.SelectGoalsSortModeUseCase
import app.musikus.usecase.userpreferences.SelectItemSortModeUseCase
import app.musikus.usecase.userpreferences.UserPreferencesUseCases
import app.musikus.utils.FakeIdProvider
import app.musikus.utils.FakeTimeProvider
import app.musikus.utils.IdProvider
import app.musikus.utils.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Named
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [MainModule::class, RepositoriesModule::class, UseCasesModule::class]
)
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
            withDatabaseTransaction = { block -> database.withTransaction(block) }
        )
    }

    @Provides
    fun provideLibraryUseCases(
        libraryRepository: LibraryRepository,
        userPreferencesUseCases: UserPreferencesUseCases
    ): LibraryUseCases {
        return LibraryUseCases(
            getItems = GetItemsUseCase(libraryRepository, userPreferencesUseCases.getItemSortInfo),
            getFolders = GetFoldersUseCase(libraryRepository, userPreferencesUseCases.getFolderSortInfo),
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

    @Provides
    fun provideGoalsUseCases(
        goalRepository: GoalRepository,
        libraryUseCases: LibraryUseCases,
        userPreferencesUseCases: UserPreferencesUseCases,
        timeProvider: TimeProvider
    ): GoalsUseCases {
        val sortGoalsUseCase = SortGoalsUseCase(userPreferencesUseCases.getGoalSortInfo)
        val cleanFutureGoalInstancesUseCase = CleanFutureGoalInstancesUseCase(
            goalRepository = goalRepository,
            timeProvider = timeProvider
        )
        val archiveGoalsUseCase = ArchiveGoalsUseCase(
            goalRepository = goalRepository,
            cleanFutureGoalInstances = cleanFutureGoalInstancesUseCase,
        )

        return GoalsUseCases(
            getAll = GetAllGoalsUseCase(goalRepository, sortGoalsUseCase),
            getCurrent = GetCurrentGoalsUseCase(goalRepository, sortGoalsUseCase),
            getLastFiveCompleted = GetLastFiveCompletedGoalsUseCase(goalRepository),
            add = AddGoalUseCase(goalRepository, libraryUseCases.getItems, timeProvider),
            pause = PauseGoalsUseCase(goalRepository, cleanFutureGoalInstancesUseCase),
            unpause = UnpauseGoalsUseCase(goalRepository),
            archive = archiveGoalsUseCase,
            unarchive = UnarchiveGoalsUseCase(goalRepository, timeProvider),
            update = UpdateGoalsUseCase(
                goalRepository = goalRepository,
                archiveGoals = archiveGoalsUseCase,
                timeProvider = timeProvider,
            ),
            edit = EditGoalUseCase(goalRepository, cleanFutureGoalInstancesUseCase),
            delete = DeleteGoalsUseCase(goalRepository),
            restore = RestoreGoalsUseCase(goalRepository),
        )
    }

    @Provides
    fun provideSessionUseCases(
        sessionRepository: SessionRepository,
        libraryUseCases: LibraryUseCases,
    ): SessionsUseCases {
        return SessionsUseCases(
            getAll = GetAllSessionsUseCase(sessionRepository),
            getInTimeframe = GetSessionsInTimeframeUseCase(sessionRepository),
            add = AddSessionUseCase(sessionRepository, libraryUseCases.getItems),
            edit = EditSessionUseCase(sessionRepository),
            delete = DeleteSessionsUseCase(sessionRepository),
            restore = RestoreSessionsUseCase(sessionRepository),
        )
    }

    @Provides
    fun providesUserPreferencesUseCases(
        userPreferencesRepository: UserPreferencesRepository
    ): UserPreferencesUseCases {
        return UserPreferencesUseCases(
            getFolderSortInfo = GetFolderSortInfoUseCase(userPreferencesRepository),
            getItemSortInfo = GetItemSortInfoUseCase(userPreferencesRepository),
            getGoalSortInfo = GetGoalSortInfoUseCase(userPreferencesRepository),
            selectFolderSortMode = SelectFolderSortModeUseCase(userPreferencesRepository),
            selectItemSortMode = SelectItemSortModeUseCase(userPreferencesRepository),
            selectGoalSortMode = SelectGoalsSortModeUseCase(userPreferencesRepository),
        )
    }
}