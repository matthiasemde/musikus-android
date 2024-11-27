/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.core.di

import app.musikus.core.domain.UserPreferencesRepository
import app.musikus.core.domain.usecase.ConfirmAnnouncementMessageUseCase
import app.musikus.core.domain.usecase.CoreUseCases
import app.musikus.core.domain.usecase.GetIdOfLastSeenAnnouncementSeenUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreUseCasesModule {

    @Provides
    @Singleton
    fun provideCoreUseCases(
        userPreferencesRepository: UserPreferencesRepository,
    ): CoreUseCases {
        return CoreUseCases(
            getIdOfLastSeenAnnouncementSeen = GetIdOfLastSeenAnnouncementSeenUseCase(
                userPreferencesRepository
            ),
            confirmAnnouncementMessage = ConfirmAnnouncementMessageUseCase(
                userPreferencesRepository
            ),
        )
    }
}
