/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.core.di

import android.app.Application
import androidx.room.Room
import app.musikus.core.data.MusikusDatabase
import app.musikus.core.domain.FakeIdProvider
import app.musikus.core.domain.FakeTimeProvider
import app.musikus.core.domain.IdProvider
import app.musikus.core.domain.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Named
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [MainModule::class]
)
object TestMainModule {

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider {
        return FakeTimeProvider()
    }

    @Provides
    fun provideFakeTimeProvider(
        timeProvider: TimeProvider
    ): FakeTimeProvider {
        return timeProvider as FakeTimeProvider
    }

    @Provides
    @Singleton
    fun provideIdProvider(): IdProvider {
        return FakeIdProvider()
    }

    @Provides
    @Singleton
    @Named("test_db")
    fun provideMusikusDatabase(
        app: Application,
        timeProvider: TimeProvider,
        idProvider: IdProvider
    ): MusikusDatabase {
        return Room.inMemoryDatabaseBuilder(
            app,
            MusikusDatabase::class.java
        ).build().apply {
            this.timeProvider = timeProvider
            this.idProvider = idProvider
        }
    }

    @Provides
    fun providesMusikusDatabase(
        @Named("test_db") database: MusikusDatabase
    ): MusikusDatabase {
        return database
    }
}
