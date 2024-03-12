/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.di

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.withTransaction
import app.musikus.database.MusikusDatabase
import app.musikus.repository.ActiveSessionRepositoryImpl
import app.musikus.repository.GoalRepository
import app.musikus.repository.GoalRepositoryImpl
import app.musikus.repository.LibraryRepository
import app.musikus.repository.LibraryRepositoryImpl
import app.musikus.repository.RecordingsRepositoryImpl
import app.musikus.repository.SessionRepository
import app.musikus.repository.SessionRepositoryImpl
import app.musikus.repository.UserPreferencesRepository
import app.musikus.repository.UserPreferencesRepositoryImpl
import app.musikus.usecase.activesession.ActiveSessionRepository
import app.musikus.usecase.recordings.RecordingsRepository
import app.musikus.utils.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoriesModule {

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        dataStore: DataStore<Preferences>
    ): UserPreferencesRepository {
        return UserPreferencesRepositoryImpl(dataStore)
    }

    @Provides
    @Singleton
    fun provideLibraryRepository(
        database: MusikusDatabase,
        @IoScope ioScope: CoroutineScope
    ): LibraryRepository {
        return LibraryRepositoryImpl(
            itemDao = database.libraryItemDao,
            folderDao = database.libraryFolderDao,
        ).apply {
            ioScope.launch {
                clean()
            }
        }
    }

    @Provides
    @Singleton
    fun provideGoalRepository(
        database: MusikusDatabase,
        @IoScope ioScope: CoroutineScope
    ): GoalRepository {
        return GoalRepositoryImpl(
            database = database
        ).apply {
            ioScope.launch {
                clean()
            }
        }
    }

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

    @Provides
    @Singleton
    fun provideActiveSessionRepository() : ActiveSessionRepository {
        return ActiveSessionRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideRecordingsRepository(
        application: Application,
        @IoScope ioScope: CoroutineScope
    ): RecordingsRepository {
        return RecordingsRepositoryImpl(
            application = application,
            contentResolver = application.contentResolver,
            ioScope = ioScope
        )
    }
}