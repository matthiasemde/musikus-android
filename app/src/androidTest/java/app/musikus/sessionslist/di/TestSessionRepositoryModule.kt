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
import app.musikus.core.domain.TimeProvider
import app.musikus.sessionslist.data.SessionRepositoryImpl
import app.musikus.sessionslist.domain.SessionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Named


@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [SessionRepositoryModule::class]
)
object TestSessionRepositoryModule {

    @Provides
    fun provideSessionRepository(
        @Named("test_db") database: MusikusDatabase,
        timeProvider: TimeProvider
    ): SessionRepository {
        return SessionRepositoryImpl(
            timeProvider = timeProvider,
            sessionDao = database.sessionDao,
            sectionDao = database.sectionDao,
            withDatabaseTransaction = { block -> database.withTransaction(block) }
        )
    }

}