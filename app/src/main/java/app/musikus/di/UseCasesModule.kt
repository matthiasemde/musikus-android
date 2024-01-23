/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.di

import android.util.Log
import app.musikus.repository.GoalRepository
import app.musikus.repository.LibraryRepository
import app.musikus.repository.SessionRepository
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
import app.musikus.usecase.goals.SelectGoalsSortModeUseCase
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
import app.musikus.usecase.library.GetFolderSortInfoUseCase
import app.musikus.usecase.library.GetFoldersUseCase
import app.musikus.usecase.library.GetItemSortInfoUseCase
import app.musikus.usecase.library.GetItemsUseCase
import app.musikus.usecase.library.LibraryUseCases
import app.musikus.usecase.library.RestoreFoldersUseCase
import app.musikus.usecase.library.RestoreItemsUseCase
import app.musikus.usecase.library.SelectFolderSortModeUseCase
import app.musikus.usecase.library.SelectItemSortModeUseCase
import app.musikus.usecase.sessions.AddSessionUseCase
import app.musikus.usecase.sessions.DeleteSessionsUseCase
import app.musikus.usecase.sessions.EditSessionUseCase
import app.musikus.usecase.sessions.GetAllSessionsUseCase
import app.musikus.usecase.sessions.GetSessionsInTimeframeUseCase
import app.musikus.usecase.sessions.RestoreSessionsUseCase
import app.musikus.usecase.sessions.SessionsUseCases
import app.musikus.utils.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object UseCasesModule {

    @Provides
    @ViewModelScoped
    fun provideLibraryUseCases(
        libraryRepository: LibraryRepository,
        userPreferencesRepository: UserPreferencesRepository
    ): LibraryUseCases {
        Log.d("UseCasesModule", "providesLibraryUseCases")

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
    @ViewModelScoped
    fun providesGoalUseCases(
        goalRepository: GoalRepository,
        libraryRepository: LibraryRepository,
        userPreferencesRepository: UserPreferencesRepository,
        timeProvider: TimeProvider
    ): GoalsUseCases {

        Log.d("UseCasesModule", "providesGoalUseCases")
        val sortGoalsUseCase = SortGoalsUseCase(userPreferencesRepository)
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
            add = AddGoalUseCase(goalRepository, libraryRepository, timeProvider),
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
            selectSortMode = SelectGoalsSortModeUseCase(userPreferencesRepository),
        )
    }

    @Provides
    @ViewModelScoped
    fun provideSessionsUseCases(
        sessionRepository: SessionRepository,
        libraryRepository: LibraryRepository
    ): SessionsUseCases {
        return SessionsUseCases(
            getAll = GetAllSessionsUseCase(sessionRepository),
            getInTimeframe = GetSessionsInTimeframeUseCase(sessionRepository),
            add = AddSessionUseCase(sessionRepository, libraryRepository),
            edit = EditSessionUseCase(sessionRepository),
            delete = DeleteSessionsUseCase(sessionRepository),
            restore = RestoreSessionsUseCase(sessionRepository),
        )
    }
}