/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.goals.di

import app.musikus.core.domain.TimeProvider
import app.musikus.goals.data.GoalRepository
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
import app.musikus.library.domain.usecase.LibraryUseCases
import app.musikus.sessions.domain.usecase.SessionsUseCases
import app.musikus.settings.domain.usecase.UserPreferencesUseCases
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object GoalsUseCasesModule {

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

}