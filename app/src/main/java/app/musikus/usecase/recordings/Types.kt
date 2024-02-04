/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.usecase.recordings

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import kotlin.time.Duration

data class Recording(
    val id: Long,
    val title: String,
    val duration: Duration,
    val date: ZonedDateTime,
    val contentUri: Uri
)

interface RecordingsRepository {
    val recordings: Flow<List<Recording>>
}