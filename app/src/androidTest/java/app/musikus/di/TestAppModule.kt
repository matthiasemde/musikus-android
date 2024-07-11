/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde
 */
@file:Suppress("DEPRECATION")

package app.musikus.di

import android.app.Application
import android.os.AsyncTask
import androidx.room.Room
import androidx.room.withTransaction
import app.musikus.core.data.MusikusDatabase
import app.musikus.activesession.data.ActiveSessionRepositoryImpl
import app.musikus.repository.FakeUserPreferencesRepository
import app.musikus.goals.data.GoalRepository
import app.musikus.goals.data.GoalRepositoryImpl
import app.musikus.library.data.LibraryRepository
import app.musikus.library.data.LibraryRepositoryImpl
import app.musikus.recorder.data.RecordingsRepositoryImpl
import app.musikus.sessionslist.data.SessionRepositoryImpl
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
import app.musikus.utils.FakeIdProvider
import app.musikus.utils.FakeTimeProvider
import app.musikus.utils.IdProvider
import app.musikus.utils.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import javax.inject.Named
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [MainModule::class, RepositoriesModule::class, UseCasesModule::class, CoroutinesDispatchersModule::class]
)
object TestAppModule {

    @DefaultDispatcher
    @Provides
    fun providesDefaultDispatcher(): CoroutineDispatcher =
        AsyncTask.THREAD_POOL_EXECUTOR.asCoroutineDispatcher()

    @IoDispatcher
    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher {
        return AsyncTask.THREAD_POOL_EXECUTOR.asCoroutineDispatcher()
    }

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
    fun providesMusikusDatabase(
        @Named("test_db") database: MusikusDatabase
    ): MusikusDatabase {
        return database
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
    fun provideRecordingsRepository(
        application: Application,
        @IoScope ioScope: CoroutineScope
    ) : RecordingsRepository {
        return RecordingsRepositoryImpl(
            application = application,
            contentResolver = application.contentResolver,
            ioScope = ioScope
        )
    }

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

    @Provides
    fun provideGoalsUseCases(
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
                calculateProgress = calculateGoalProgressUseCase,
            ),
            getLastFiveCompleted = GetLastFiveCompletedGoalsUseCase(
                goalRepository = goalRepository,
                calculateProgress = calculateGoalProgressUseCase,
            ),
            getLastNBeforeInstance = GetLastNBeforeInstanceUseCase(
                goalRepository = goalRepository,
                calculateProgress = calculateGoalProgressUseCase,
            ),
            getNextNAfterInstance = GetNextNAfterInstanceUseCase(
                goalRepository = goalRepository,
                calculateProgress = calculateGoalProgressUseCase,
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
    fun provideActiveSessionRepository() : ActiveSessionRepository {
        return ActiveSessionRepositoryImpl()
    }


    @Provides
    fun provideSessionUseCases(
        sessionRepository: SessionRepository,
        libraryUseCases: LibraryUseCases,
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
    fun provideUserPreferencesUseCases(
        userPreferencesRepository: UserPreferencesRepository
    ): UserPreferencesUseCases {
        return UserPreferencesUseCases(
            getTheme = GetThemeUseCase(userPreferencesRepository),
            getColorScheme = GetColorSchemeUseCase(userPreferencesRepository),
            getFolderSortInfo = GetFolderSortInfoUseCase(userPreferencesRepository),
            getItemSortInfo = GetItemSortInfoUseCase(userPreferencesRepository),
            getGoalSortInfo = GetGoalSortInfoUseCase(userPreferencesRepository),
            getMetronomeSettings = GetMetronomeSettingsUseCase(userPreferencesRepository),
            selectTheme = SelectThemeUseCase(userPreferencesRepository),
            selectColorScheme = SelectColorSchemeUseCase(userPreferencesRepository),
            selectFolderSortMode = SelectFolderSortModeUseCase(userPreferencesRepository),
            selectItemSortMode = SelectItemSortModeUseCase(userPreferencesRepository),
            selectGoalSortMode = SelectGoalsSortModeUseCase(userPreferencesRepository),
            changeMetronomeSettings = ChangeMetronomeSettingsUseCase(userPreferencesRepository),
        )
    }

    @Provides
    fun provideRecordingsUseCases(
        recordingRepository: RecordingsRepository,
    ): RecordingsUseCases {
        return RecordingsUseCases(
            get = GetRecordingsUseCase(recordingRepository),
            getRawRecording = GetRawRecordingUseCase(recordingRepository),
        )
    }
}