/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.recorder.domain.usecase

import app.musikus.recorder.data.RecordingsRepository


class GetRecordingsUseCase(
    private val recordingsRepository: RecordingsRepository
) {

    operator fun invoke() = recordingsRepository.recordings
}