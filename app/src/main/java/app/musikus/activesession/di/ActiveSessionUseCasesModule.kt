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
import app.musikus.activesession.domain.usecase.DeleteSectionUseCase
import app.musikus.activesession.domain.usecase.GetCompletedSectionsUseCase
import app.musikus.activesession.domain.usecase.GetFinalizedSessionUseCase
import app.musikus.activesession.domain.usecase.GetOngoingPauseDurationUseCase
import app.musikus.activesession.domain.usecase.GetRunningItemDurationUseCase
import app.musikus.activesession.domain.usecase.GetRunningItemUseCase
import app.musikus.activesession.domain.usecase.GetSessionStatusUseCase
import app.musikus.activesession.domain.usecase.GetStartTimeUseCase
import app.musikus.activesession.domain.usecase.GetTotalPracticeDurationUseCase
import app.musikus.activesession.domain.usecase.IsSessionPausedUseCase
import app.musikus.activesession.domain.usecase.IsSessionRunningUseCase
import app.musikus.activesession.domain.usecase.PauseActiveSessionUseCase
import app.musikus.activesession.domain.usecase.ResetSessionUseCase
import app.musikus.activesession.domain.usecase.ResumeActiveSessionUseCase
import app.musikus.activesession.domain.usecase.SelectItemUseCase
import app.musikus.core.domain.IdProvider
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
        idProvider: IdProvider
    ): ActiveSessionUseCases {
        val getOngoingPauseDurationUseCase = GetOngoingPauseDurationUseCase(activeSessionRepository)

        val resumeUseCase = ResumeActiveSessionUseCase(
            activeSessionRepository,
            getOngoingPauseDurationUseCase
        )

        val getRunningItemDurationUseCase = GetRunningItemDurationUseCase(activeSessionRepository)

        return ActiveSessionUseCases(
            selectItem = SelectItemUseCase(
                activeSessionRepository = activeSessionRepository,
                getRunningItemDuration = getRunningItemDurationUseCase,
                idProvider = idProvider
            ),
            getTotalPracticeDuration = GetTotalPracticeDurationUseCase(
                activeSessionRepository = activeSessionRepository,
                getRunningItemDuration = getRunningItemDurationUseCase
            ),
            deleteSection = DeleteSectionUseCase(activeSessionRepository),
            pause = PauseActiveSessionUseCase(
                activeSessionRepository = activeSessionRepository,
            ),
            resume = resumeUseCase,
            getRunningItemDuration = getRunningItemDurationUseCase,
            getCompletedSections = GetCompletedSectionsUseCase(activeSessionRepository),
            getOngoingPauseDuration = getOngoingPauseDurationUseCase,
            isSessionPaused = IsSessionPausedUseCase(activeSessionRepository),
            getFinalizedSession = GetFinalizedSessionUseCase(
                activeSessionRepository = activeSessionRepository,
                getRunningItemDuration = getRunningItemDurationUseCase,
                idProvider = idProvider,
                getOngoingPauseDuration = getOngoingPauseDurationUseCase
            ),
            getStartTime = GetStartTimeUseCase(activeSessionRepository),
            reset = ResetSessionUseCase(activeSessionRepository),
            getRunningItem = GetRunningItemUseCase(activeSessionRepository),
            isSessionRunning = IsSessionRunningUseCase(activeSessionRepository),
            getSessionStatus = GetSessionStatusUseCase(activeSessionRepository)
        )
    }
}
