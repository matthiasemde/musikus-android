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
import app.musikus.recorder.data.RecordingsRepository
import app.musikus.recorder.data.RecordingsRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineScope

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RecordingsRepositoryModule::class]
)
object TestRecordingsRepositoryModule {

    @Provides
    fun provideRecordingsRepository(
        application: Application,
        @IoScope ioScope: CoroutineScope
    ) : RecordingsRepository {
        return RecordingsRepositoryImpl(
            application = application,
            contentResolver = application.contentResolver,
            ioScope = ioScope
        )
    }

}