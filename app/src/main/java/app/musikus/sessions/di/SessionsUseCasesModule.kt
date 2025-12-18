/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.sessions.di

import app.musikus.core.domain.UserPreferencesRepository
import app.musikus.library.domain.usecase.LibraryUseCases
import app.musikus.sessions.domain.SessionRepository
import app.musikus.sessions.domain.usecase.AddSessionUseCase
import app.musikus.sessions.domain.usecase.DeleteSessionsUseCase
import app.musikus.sessions.domain.usecase.EditSessionUseCase
import app.musikus.sessions.domain.usecase.GetAllSessionsUseCase
import app.musikus.sessions.domain.usecase.GetAppIntroDialogIndexUseCase
import app.musikus.sessions.domain.usecase.GetSessionByIdUseCase
import app.musikus.sessions.domain.usecase.GetSessionsForDaysForMonthsUseCase
import app.musikus.sessions.domain.usecase.GetSessionsInTimeframeUseCase
import app.musikus.sessions.domain.usecase.RestoreSessionsUseCase
import app.musikus.sessions.domain.usecase.SessionsUseCases
import app.musikus.sessions.domain.usecase.SetAppIntroDialogIndexUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SessionsUseCasesModule {

    @Provides
    @Singleton
    fun provideSessionsUseCases(
        sessionRepository: SessionRepository,
        libraryUseCases: LibraryUseCases,
        userPreferencesRepository: UserPreferencesRepository,
    ): SessionsUseCases {
        return SessionsUseCases(
            getAll = GetAllSessionsUseCase(sessionRepository),
            getSessionsForDaysForMonths = GetSessionsForDaysForMonthsUseCase(sessionRepository),
            getInTimeframe = GetSessionsInTimeframeUseCase(sessionRepository),
            getById = GetSessionByIdUseCase(sessionRepository),
            getAppIntroDialogIndex = GetAppIntroDialogIndexUseCase(userPreferencesRepository),
            add = AddSessionUseCase(sessionRepository, libraryUseCases.getAllItems),
            edit = EditSessionUseCase(sessionRepository),
            delete = DeleteSessionsUseCase(sessionRepository),
            restore = RestoreSessionsUseCase(sessionRepository),
            setAppIntroDialogIndex = SetAppIntroDialogIndexUseCase(userPreferencesRepository),
        )
    }
}
