/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.activesession.di

import app.musikus.activesession.domain.ActiveSessionRepository
import app.musikus.activesession.domain.usecase.ActiveSessionUseCases
import app.musikus.activesession.domain.usecase.ComputeOngoingPauseDurationUseCase
import app.musikus.activesession.domain.usecase.ComputeRunningItemDurationUseCase
import app.musikus.activesession.domain.usecase.ComputeTotalPracticeDurationUseCase
import app.musikus.activesession.domain.usecase.DeleteSectionUseCase
import app.musikus.activesession.domain.usecase.GetActiveSessionStateUseCase
import app.musikus.activesession.domain.usecase.GetAppIntroDialogIndexUseCase
import app.musikus.activesession.domain.usecase.GetCompletedSectionsUseCase
import app.musikus.activesession.domain.usecase.GetFinalizedSessionUseCase
import app.musikus.activesession.domain.usecase.GetRunningItemUseCase
import app.musikus.activesession.domain.usecase.GetSessionStatusUseCase
import app.musikus.activesession.domain.usecase.GetStartTimeUseCase
import app.musikus.activesession.domain.usecase.IsSessionPausedUseCase
import app.musikus.activesession.domain.usecase.IsSessionRunningUseCase
import app.musikus.activesession.domain.usecase.PauseActiveSessionUseCase
import app.musikus.activesession.domain.usecase.ResetSessionUseCase
import app.musikus.activesession.domain.usecase.ResumeActiveSessionUseCase
import app.musikus.activesession.domain.usecase.SelectItemUseCase
import app.musikus.activesession.domain.usecase.SetAppIntroDialogIndexUseCase
import app.musikus.core.domain.IdProvider
import app.musikus.core.domain.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ActiveSessionUseCasesModule {

    @Provides
    @Singleton
    fun provideActiveSessionUseCases(
        activeSessionRepository: ActiveSessionRepository,
        idProvider: IdProvider,
        userPreferencesRepository: UserPreferencesRepository,
    ): ActiveSessionUseCases {
        val computeOngoingPauseDurationUseCase = ComputeOngoingPauseDurationUseCase()

        val resumeUseCase = ResumeActiveSessionUseCase(
            activeSessionRepository,
            computeOngoingPauseDurationUseCase
        )

        val computeRunningItemDurationUseCase = ComputeRunningItemDurationUseCase()

        return ActiveSessionUseCases(
            getState = GetActiveSessionStateUseCase(activeSessionRepository),
            selectItem = SelectItemUseCase(
                activeSessionRepository = activeSessionRepository,
                computeRunningItemDuration = computeRunningItemDurationUseCase,
                idProvider = idProvider
            ),
            computeTotalPracticeDuration = ComputeTotalPracticeDurationUseCase(
                computeRunningItemDuration = computeRunningItemDurationUseCase
            ),
            deleteSection = DeleteSectionUseCase(activeSessionRepository),
            pause = PauseActiveSessionUseCase(
                activeSessionRepository = activeSessionRepository,
            ),
            resume = resumeUseCase,
            computeRunningItemDuration = computeRunningItemDurationUseCase,
            getCompletedSections = GetCompletedSectionsUseCase(activeSessionRepository),
            computeOngoingPauseDuration = computeOngoingPauseDurationUseCase,
            isSessionPaused = IsSessionPausedUseCase(activeSessionRepository),
            getFinalizedSession = GetFinalizedSessionUseCase(
                activeSessionRepository = activeSessionRepository,
                computeRunningItemDuration = computeRunningItemDurationUseCase,
                idProvider = idProvider,
                computeOngoingPauseDuration = computeOngoingPauseDurationUseCase
            ),
            getStartTime = GetStartTimeUseCase(activeSessionRepository),
            reset = ResetSessionUseCase(activeSessionRepository),
            getRunningItem = GetRunningItemUseCase(activeSessionRepository),
            isSessionRunning = IsSessionRunningUseCase(activeSessionRepository),
            getSessionStatus = GetSessionStatusUseCase(activeSessionRepository),

            setAppIntroDialogIndex = SetAppIntroDialogIndexUseCase(userPreferencesRepository),
            getAppIntroDialogIndex = GetAppIntroDialogIndexUseCase(userPreferencesRepository)
        )
    }
}
