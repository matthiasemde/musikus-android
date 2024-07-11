/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.sessionslist.di

import androidx.room.withTransaction
import app.musikus.core.data.MusikusDatabase
import app.musikus.core.di.IoScope
import app.musikus.core.domain.TimeProvider
import app.musikus.sessionslist.data.SessionRepository
import app.musikus.sessionslist.data.SessionRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object SessionRepositoryModule {

    @Provides
    @Singleton
    fun provideSessionRepository(
        database: MusikusDatabase,
        timeProvider: TimeProvider,
        @IoScope ioScope: CoroutineScope
    ): SessionRepository {
        return SessionRepositoryImpl(
            timeProvider = timeProvider,
            sessionDao = database.sessionDao,
            sectionDao = database.sectionDao,
            withDatabaseTransaction = { block -> database.withTransaction(block) }
        ).apply {
            ioScope.launch {
                clean()
            }
        }
    }
}