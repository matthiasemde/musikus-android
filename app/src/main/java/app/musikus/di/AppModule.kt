/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.di

import android.app.Application
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.musikus.dataStore
import app.musikus.database.MusikusDatabase
import app.musikus.repository.LibraryRepository
import app.musikus.repository.LibraryRepositoryImpl
import app.musikus.repository.UserPreferencesRepository
import app.musikus.repository.UserPreferencesRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
        Log.d("Musikus", "provideUserPreferencesRepository")
        return UserPreferencesRepositoryImpl(dataStore)
    }

    @Provides
    @Singleton
    fun provideMusikusDatabase(app: Application): MusikusDatabase {
        Log.d("Musikus", "provideMusikusDatabase")
        return MusikusDatabase.buildDatabase(app)
//        return Room.databaseBuilder(
//            app,
//            MusikusDatabase::class.java,
//            MusikusDatabase.DATABASE_NAME
//        ).build()
    }

    @Provides
    @Singleton
    fun provideLibraryRepository(
        database: MusikusDatabase
    ): LibraryRepository {
        Log.d("Musikus", "provideLibraryRepository")
        return LibraryRepositoryImpl(
            itemDao = database.libraryItemDao,
            folderDao = database.libraryFolderDao,
        )
    }
}