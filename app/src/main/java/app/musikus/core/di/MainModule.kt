/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.core.di

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import app.musikus.core.data.MusikusDatabase
import app.musikus.core.domain.IdProvider
import app.musikus.core.domain.IdProviderImpl
import app.musikus.core.domain.TimeProvider
import app.musikus.core.domain.TimeProviderImpl
import app.musikus.core.domain.USER_PREFERENCES_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.SupervisorJob
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MainModule {

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider {
        return TimeProviderImpl()
    }

    @Provides
    @Singleton
    fun provideIdProvider(): IdProvider {
        return IdProviderImpl()
    }

    @Provides
    @Singleton
    fun provideDataStore(app: Application): DataStore<Preferences> {
        // source: https://medium.com/androiddevelopers/datastore-and-dependency-injection-ea32b95704e3
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler {
                emptyPreferences()
            },
            scope = CoroutineScope(IO + SupervisorJob()),
            produceFile = { app.preferencesDataStoreFile(USER_PREFERENCES_NAME) }
        )
    }

    /**
     * Dependency injection for the database
     * @param app Application
     * @param databaseProvider Provider<MusikusDatabase>: Needed for prepopulating the database
     * */
    @Provides
    @Singleton
    fun provideMusikusDatabase(
        app: Application,
        databaseProvider: Provider<MusikusDatabase>,
        timeProvider: TimeProvider,
        idProvider: IdProvider,
    ): MusikusDatabase {
        return MusikusDatabase.buildDatabase(
            context = app,
            databaseProvider = databaseProvider,
        ).apply {
            this.timeProvider = timeProvider
            this.idProvider = idProvider
            this.databaseFile = app.getDatabasePath(MusikusDatabase.DATABASE_NAME)
        }
    }
}
