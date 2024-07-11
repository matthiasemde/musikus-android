/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.sessionslist.di

import app.musikus.library.domain.usecase.LibraryUseCases
import app.musikus.sessionslist.data.SessionRepository
import app.musikus.sessionslist.domain.usecase.AddSessionUseCase
import app.musikus.sessionslist.domain.usecase.DeleteSessionsUseCase
import app.musikus.sessionslist.domain.usecase.EditSessionUseCase
import app.musikus.sessionslist.domain.usecase.GetSessionByIdUseCase
import app.musikus.sessionslist.domain.usecase.GetSessionsForDaysForMonthsUseCase
import app.musikus.sessionslist.domain.usecase.GetSessionsInTimeframeUseCase
import app.musikus.sessionslist.domain.usecase.RestoreSessionsUseCase
import app.musikus.sessionslist.domain.usecase.SessionsUseCases
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [SessionsUseCasesModule::class]
)
object TestSessionsUseCasesModule {

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

}