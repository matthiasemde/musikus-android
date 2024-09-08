/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.recorder.di

import android.app.Application
import app.musikus.core.di.IoScope
import app.musikus.recorder.data.RecordingsRepositoryImpl
import app.musikus.recorder.domain.RecordingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RecordingsRepositoryModule {

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
