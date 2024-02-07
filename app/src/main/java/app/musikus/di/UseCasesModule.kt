/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.di

import app.musikus.repository.GoalRepository
import app.musikus.repository.LibraryRepository
import app.musikus.repository.SessionRepository
import app.musikus.repository.UserPreferencesRepository
import app.musikus.usecase.goals.AddGoalUseCase
import app.musikus.usecase.goals.ArchiveGoalsUseCase
import app.musikus.usecase.goals.CalculateGoalProgressUseCase
import app.musikus.usecase.goals.CleanFutureGoalInstancesUseCase
import app.musikus.usecase.goals.DeleteGoalsUseCase
import app.musikus.usecase.goals.EditGoalUseCase
import app.musikus.usecase.goals.GetAllGoalsUseCase
import app.musikus.usecase.goals.GetCurrentGoalsUseCase
import app.musikus.usecase.goals.GetLastFiveCompletedGoalsUseCase
import app.musikus.usecase.goals.GetLastNBeforeInstanceUseCase
import app.musikus.usecase.goals.GetNextNAfterInstanceUseCase
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
import app.musikus.usecase.library.GetAllLibraryItemsUseCase
import app.musikus.usecase.library.GetSortedLibraryFoldersUseCase
import app.musikus.usecase.library.GetSortedLibraryItemsUseCase
import app.musikus.usecase.library.LibraryUseCases
import app.musikus.usecase.library.RestoreFoldersUseCase
import app.musikus.usecase.library.RestoreItemsUseCase
import app.musikus.usecase.recordings.GetRawRecordingUseCase
import app.musikus.usecase.recordings.GetRecordingsUseCase
import app.musikus.usecase.recordings.RecordingsRepository
import app.musikus.usecase.recordings.RecordingsUseCases
import app.musikus.usecase.sessions.AddSessionUseCase
import app.musikus.usecase.sessions.DeleteSessionsUseCase
import app.musikus.usecase.sessions.EditSessionUseCase
import app.musikus.usecase.sessions.GetSessionByIdUseCase
import app.musikus.usecase.sessions.GetSessionsForDaysForMonthsUseCase
import app.musikus.usecase.sessions.GetSessionsInTimeframeUseCase
import app.musikus.usecase.sessions.RestoreSessionsUseCase
import app.musikus.usecase.sessions.SessionsUseCases
import app.musikus.usecase.userpreferences.ChangeMetronomeSettingsUseCase
import app.musikus.usecase.userpreferences.GetFolderSortInfoUseCase
import app.musikus.usecase.userpreferences.GetGoalSortInfoUseCase
import app.musikus.usecase.userpreferences.GetItemSortInfoUseCase
import app.musikus.usecase.userpreferences.GetMetronomeSettingsUseCase
import app.musikus.usecase.userpreferences.GetThemeUseCase
import app.musikus.usecase.userpreferences.SelectFolderSortModeUseCase
import app.musikus.usecase.userpreferences.SelectGoalsSortModeUseCase
import app.musikus.usecase.userpreferences.SelectItemSortModeUseCase
import app.musikus.usecase.userpreferences.SelectThemeUseCase
import app.musikus.usecase.userpreferences.UserPreferencesUseCases
import app.musikus.utils.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCasesModule {

    @Provides
    @Singleton
    fun provideLibraryUseCases(
        libraryRepository: LibraryRepository,
        userPreferencesUseCases: UserPreferencesUseCases
    ): LibraryUseCases {

        return LibraryUseCases(
            getAllItems = GetAllLibraryItemsUseCase(libraryRepository),
            getSortedItems = GetSortedLibraryItemsUseCase(libraryRepository, userPreferencesUseCases.getItemSortInfo),
            getSortedFolders = GetSortedLibraryFoldersUseCase(libraryRepository, userPreferencesUseCases.getFolderSortInfo),
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
    @Singleton
    fun providesGoalUseCases(
        goalRepository: GoalRepository,
        libraryUseCases: LibraryUseCases,
        sessionsUseCases: SessionsUseCases,
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

        val calculateGoalProgressUseCase = CalculateGoalProgressUseCase(
        getSessionsInTimeframe = sessionsUseCases.getInTimeframe,
        timeProvider = timeProvider
        )

        return GoalsUseCases(
            calculateProgress = calculateGoalProgressUseCase,
            getAll = GetAllGoalsUseCase(
                goalRepository = goalRepository,
                sortGoals = sortGoalsUseCase,
            ),
            getCurrent = GetCurrentGoalsUseCase(
                goalRepository = goalRepository,
                sortGoals = sortGoalsUseCase,
                calculateProgress = calculateGoalProgressUseCase
            ),
            getLastFiveCompleted = GetLastFiveCompletedGoalsUseCase(
                goalRepository = goalRepository,
                calculateProgress = calculateGoalProgressUseCase
            ),
            getLastNBeforeInstance = GetLastNBeforeInstanceUseCase(
                goalRepository = goalRepository,
                calculateProgress = calculateGoalProgressUseCase
            ),
            getNextNAfterInstance = GetNextNAfterInstanceUseCase(
                goalRepository = goalRepository,
                calculateProgress = calculateGoalProgressUseCase
            ),
            add = AddGoalUseCase(goalRepository, libraryUseCases.getAllItems, timeProvider),
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
    @Singleton
    fun provideSessionsUseCases(
        sessionRepository: SessionRepository,
        libraryUseCases: LibraryUseCases
    ): SessionsUseCases {
        return SessionsUseCases(
            getSessionsForDaysForMonths = GetSessionsForDaysForMonthsUseCase(sessionRepository),
            getInTimeframe = GetSessionsInTimeframeUseCase(sessionRepository),
            getById = GetSessionByIdUseCase(sessionRepository),
            add = AddSessionUseCase(sessionRepository, libraryUseCases.getAllItems),
            edit = EditSessionUseCase(sessionRepository),
            delete = DeleteSessionsUseCase(sessionRepository),
            restore = RestoreSessionsUseCase(sessionRepository),
        )
    }

    @Provides
    @Singleton
    fun provideUserPreferencesUseCases(
        userPreferencesRepository: UserPreferencesRepository
    ): UserPreferencesUseCases {
        return UserPreferencesUseCases(
            getTheme = GetThemeUseCase(userPreferencesRepository),
            getItemSortInfo = GetItemSortInfoUseCase(userPreferencesRepository),
            getFolderSortInfo = GetFolderSortInfoUseCase(userPreferencesRepository),
            getGoalSortInfo = GetGoalSortInfoUseCase(userPreferencesRepository),
            selectTheme = SelectThemeUseCase(userPreferencesRepository),
            selectItemSortMode = SelectItemSortModeUseCase(userPreferencesRepository),
            selectFolderSortMode = SelectFolderSortModeUseCase(userPreferencesRepository),
            selectGoalSortMode = SelectGoalsSortModeUseCase(userPreferencesRepository),
            getMetronomeSettings = GetMetronomeSettingsUseCase(userPreferencesRepository),
            changeMetronomeSettings = ChangeMetronomeSettingsUseCase(userPreferencesRepository),
        )
    }

    @Provides
    @Singleton
    fun provideRecordingsUseCases(
        recordingsRepository: RecordingsRepository
    ): RecordingsUseCases {
        return RecordingsUseCases(
            get = GetRecordingsUseCase(recordingsRepository),
            getRawRecording = GetRawRecordingUseCase(recordingsRepository)
        )
    }
}