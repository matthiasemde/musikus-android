/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.di

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.musikus.dataStore
import app.musikus.database.MusikusDatabase
import app.musikus.repository.GoalRepository
import app.musikus.repository.GoalRepositoryImpl
import app.musikus.repository.LibraryRepository
import app.musikus.repository.LibraryRepositoryImpl
import app.musikus.repository.SessionRepository
import app.musikus.repository.SessionRepositoryImpl
import app.musikus.repository.UserPreferencesRepository
import app.musikus.repository.UserPreferencesRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(app: Application): DataStore<Preferences> {
        return app.dataStore
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        dataStore: DataStore<Preferences>
    ): UserPreferencesRepository {
        return UserPreferencesRepositoryImpl(dataStore)
    }


    /**
     * Dependency injection for the database
     * @param app Application
     * @param dbProvider Provider<MusikusDatabase>: Needed for prepopulating the database
     * */
    @Provides
    @Singleton
    fun provideMusikusDatabase(app: Application, dbProvider: Provider<MusikusDatabase>): MusikusDatabase {
        return MusikusDatabase.buildDatabase(app, dbProvider)
    }

    @Provides
    @Singleton
    fun provideLibraryRepository(
        database: MusikusDatabase
    ): LibraryRepository {
        return LibraryRepositoryImpl(
            itemDao = database.libraryItemDao,
            folderDao = database.libraryFolderDao,
        )
    }

    @Provides
    @Singleton
    fun provideGoalRepository(
        database: MusikusDatabase
    ): GoalRepository {
        return GoalRepositoryImpl(
            goalInstanceDao = database.goalInstanceDao,
            goalDescriptionDao = database.goalDescriptionDao,
        )
    }

    @Provides
    @Singleton
    fun provideSessionRepository(
        database: MusikusDatabase
    ): SessionRepository {
        return SessionRepositoryImpl(
            sessionDao = database.sessionDao,
            sectionDao = database.sectionDao,
        )
    }
}