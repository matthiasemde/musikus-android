/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.di

import app.musikus.goals.data.GoalRepository
import app.musikus.library.data.LibraryRepository
import app.musikus.settings.data.UserPreferencesRepository
import app.musikus.activesession.domain.ActiveSessionRepository
import app.musikus.activesession.domain.usecase.ActiveSessionUseCases
import app.musikus.activesession.domain.usecase.DeleteSectionUseCase
import app.musikus.activesession.domain.usecase.GetCompletedSectionsUseCase
import app.musikus.activesession.domain.usecase.GetFinalizedSessionUseCase
import app.musikus.activesession.domain.usecase.GetOngoingPauseDurationUseCase
import app.musikus.activesession.domain.usecase.GetPausedStateUseCase
import app.musikus.activesession.domain.usecase.GetRunningItemDurationUseCase
import app.musikus.activesession.domain.usecase.GetRunningItemUseCase
import app.musikus.activesession.domain.usecase.GetSessionStatusUseCase
import app.musikus.activesession.domain.usecase.GetStartTimeUseCase
import app.musikus.activesession.domain.usecase.GetTotalPracticeDurationUseCase
import app.musikus.activesession.domain.usecase.IsSessionRunningUseCase
import app.musikus.activesession.domain.usecase.PauseActiveSessionUseCase
import app.musikus.activesession.domain.usecase.ResetSessionUseCase
import app.musikus.activesession.domain.usecase.ResumeActiveSessionUseCase
import app.musikus.activesession.domain.usecase.SelectItemUseCase
import app.musikus.goals.domain.usecase.AddGoalUseCase
import app.musikus.goals.domain.usecase.ArchiveGoalsUseCase
import app.musikus.goals.domain.usecase.CalculateGoalProgressUseCase
import app.musikus.goals.domain.usecase.CleanFutureGoalInstancesUseCase
import app.musikus.goals.domain.usecase.DeleteGoalsUseCase
import app.musikus.goals.domain.usecase.EditGoalUseCase
import app.musikus.goals.domain.usecase.GetAllGoalsUseCase
import app.musikus.goals.domain.usecase.GetCurrentGoalsUseCase
import app.musikus.goals.domain.usecase.GetLastFiveCompletedGoalsUseCase
import app.musikus.goals.domain.usecase.GetLastNBeforeInstanceUseCase
import app.musikus.goals.domain.usecase.GetNextNAfterInstanceUseCase
import app.musikus.goals.domain.usecase.GoalsUseCases
import app.musikus.goals.domain.usecase.PauseGoalsUseCase
import app.musikus.goals.domain.usecase.RestoreGoalsUseCase
import app.musikus.goals.domain.usecase.SortGoalsUseCase
import app.musikus.goals.domain.usecase.UnarchiveGoalsUseCase
import app.musikus.goals.domain.usecase.UnpauseGoalsUseCase
import app.musikus.goals.domain.usecase.UpdateGoalsUseCase
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
import app.musikus.recorder.domain.usecase.GetRawRecordingUseCase
import app.musikus.recorder.domain.usecase.GetRecordingsUseCase
import app.musikus.recorder.domain.RecordingsRepository
import app.musikus.recorder.domain.usecase.RecordingsUseCases
import app.musikus.sessionslist.domain.usecase.AddSessionUseCase
import app.musikus.sessionslist.domain.usecase.DeleteSessionsUseCase
import app.musikus.sessionslist.domain.usecase.EditSessionUseCase
import app.musikus.sessionslist.domain.usecase.GetSessionByIdUseCase
import app.musikus.sessionslist.domain.usecase.GetSessionsForDaysForMonthsUseCase
import app.musikus.sessionslist.domain.usecase.GetSessionsInTimeframeUseCase
import app.musikus.sessionslist.domain.usecase.RestoreSessionsUseCase
import app.musikus.sessionslist.domain.SessionRepository
import app.musikus.sessionslist.domain.usecase.SessionsUseCases
import app.musikus.settings.domain.usecase.ChangeMetronomeSettingsUseCase
import app.musikus.settings.domain.usecase.GetColorSchemeUseCase
import app.musikus.settings.domain.usecase.GetFolderSortInfoUseCase
import app.musikus.settings.domain.usecase.GetGoalSortInfoUseCase
import app.musikus.settings.domain.usecase.GetItemSortInfoUseCase
import app.musikus.settings.domain.usecase.GetMetronomeSettingsUseCase
import app.musikus.settings.domain.usecase.GetThemeUseCase
import app.musikus.settings.domain.usecase.SelectColorSchemeUseCase
import app.musikus.settings.domain.usecase.SelectFolderSortModeUseCase
import app.musikus.settings.domain.usecase.SelectGoalsSortModeUseCase
import app.musikus.settings.domain.usecase.SelectItemSortModeUseCase
import app.musikus.settings.domain.usecase.SelectThemeUseCase
import app.musikus.settings.domain.usecase.UserPreferencesUseCases
import app.musikus.core.domain.IdProvider
import app.musikus.core.domain.TimeProvider
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
    fun provideActiveSessionUseCases(
        activeSessionRepository: ActiveSessionRepository,
        timeProvider: TimeProvider,
        idProvider: IdProvider
    ) : ActiveSessionUseCases {

        val getOngoingPauseDurationUseCase = GetOngoingPauseDurationUseCase(
            activeSessionRepository,
            timeProvider
        )

        val resumeUseCase = ResumeActiveSessionUseCase(
            activeSessionRepository,
            getOngoingPauseDurationUseCase
        )

        val getRunningItemDurationUseCase = GetRunningItemDurationUseCase(
            activeSessionRepository,
            timeProvider
        )

        return ActiveSessionUseCases(
            selectItem = SelectItemUseCase(
                activeSessionRepository = activeSessionRepository,
                resumeUseCase = resumeUseCase,
                getRunningItemDurationUseCase = getRunningItemDurationUseCase,
                timeProvider = timeProvider,
                idProvider = idProvider
            ),
            getPracticeDuration = GetTotalPracticeDurationUseCase(
                activeSessionRepository = activeSessionRepository,
                runningItemDurationUseCase = getRunningItemDurationUseCase
            ),
            deleteSection = DeleteSectionUseCase(activeSessionRepository),
            pause = PauseActiveSessionUseCase(
                activeSessionRepository = activeSessionRepository,
                getRunningItemDurationUseCase = getRunningItemDurationUseCase,
                timeProvider = timeProvider
            ),
            resume = resumeUseCase,
            getRunningItemDuration = getRunningItemDurationUseCase,
            getCompletedSections = GetCompletedSectionsUseCase(activeSessionRepository),
            getOngoingPauseDuration = GetOngoingPauseDurationUseCase(activeSessionRepository, timeProvider),
            getPausedState = GetPausedStateUseCase(activeSessionRepository),
            getFinalizedSession = GetFinalizedSessionUseCase(
                activeSessionRepository = activeSessionRepository,
                getRunningItemDurationUseCase = getRunningItemDurationUseCase,
                resumeUseCase = resumeUseCase,
                idProvider = idProvider
            ),
            getStartTime = GetStartTimeUseCase(activeSessionRepository),
            reset = ResetSessionUseCase(activeSessionRepository),
            getRunningItem = GetRunningItemUseCase(activeSessionRepository),
            isSessionRunning = IsSessionRunningUseCase(activeSessionRepository),
            getSessionStatus = GetSessionStatusUseCase(activeSessionRepository)
        )
    }

    @Provides
    @Singleton
    fun provideUserPreferencesUseCases(
        userPreferencesRepository: UserPreferencesRepository
    ): UserPreferencesUseCases {
        return UserPreferencesUseCases(
            getTheme = GetThemeUseCase(userPreferencesRepository),
            getColorScheme = GetColorSchemeUseCase(userPreferencesRepository),
            getItemSortInfo = GetItemSortInfoUseCase(userPreferencesRepository),
            getFolderSortInfo = GetFolderSortInfoUseCase(userPreferencesRepository),
            getGoalSortInfo = GetGoalSortInfoUseCase(userPreferencesRepository),
            selectTheme = SelectThemeUseCase(userPreferencesRepository),
            selectColorScheme = SelectColorSchemeUseCase(userPreferencesRepository),
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