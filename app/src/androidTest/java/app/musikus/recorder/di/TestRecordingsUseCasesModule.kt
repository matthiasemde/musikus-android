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
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RecordingsUseCasesModule::class]
)
object TestRecordingsUseCasesModule {

    @Provides
    fun provideRecordingsUseCases(
        recordingRepository: RecordingsRepository,
    ): RecordingsUseCases {
        return RecordingsUseCases(
            get = GetRecordingsUseCase(recordingRepository),
            getRawRecording = GetRawRecordingUseCase(recordingRepository),
        )
    }
}