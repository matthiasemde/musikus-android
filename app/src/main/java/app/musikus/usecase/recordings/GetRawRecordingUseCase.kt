/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.usecase.recordings

import android.net.Uri

class GetRawRecordingUseCase(
    private val recordingsRepository: RecordingsRepository
) {

    suspend operator fun invoke(contentUri: Uri): Result<ShortArray> = recordingsRepository.getRawRecording(contentUri)
}