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
import app.musikus.core.domain.IdProvider
import app.musikus.core.domain.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [ActiveSessionUseCasesModule::class]
)
object TestActiveSessionUseCasesModule {

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

}