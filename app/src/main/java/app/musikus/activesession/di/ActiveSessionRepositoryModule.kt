/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.activesession.di

import app.musikus.activesession.data.ActiveSessionRepositoryImpl
import app.musikus.activesession.domain.ActiveSessionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object ActiveSessionRepositoryModule {

    @Provides
    @Singleton
    fun provideActiveSessionRepository() : ActiveSessionRepository {
        return ActiveSessionRepositoryImpl()
    }
}