/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.settings.di

import app.musikus.repository.FakeUserPreferencesRepository
import app.musikus.settings.domain.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [UserPreferencesRepositoryModule::class]
)
object TestUserPreferencesRepositoryModule {

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(): UserPreferencesRepository {
        return FakeUserPreferencesRepository()
    }

}