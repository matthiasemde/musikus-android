/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.recorder.data

import android.net.Uri
import app.musikus.recorder.domain.Recording
import kotlinx.coroutines.flow.Flow

interface RecordingsRepository {
    val recordings: Flow<List<Recording>>

    suspend fun getRawRecording(contentUri: Uri): Result<ShortArray>
}