/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.recorder.di

import app.musikus.recorder.data.RecordingsRepository
import app.musikus.recorder.domain.usecase.GetRawRecordingUseCase
import app.musikus.recorder.domain.usecase.GetRecordingsUseCase
import app.musikus.recorder.domain.usecase.RecordingsUseCases
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object RecordingsUseCasesModule {

    @Provides
    @Singleton
    fun provideRecordingsUseCases(
        recordingsRepository: RecordingsRepository
    ): RecordingsUseCases {
        return RecordingsUseCases(
            get = GetRecordingsUseCase(recordingsRepository),
            getRawRecording = GetRawRecordingUseCase(recordingsRepository)
        )
    }
}