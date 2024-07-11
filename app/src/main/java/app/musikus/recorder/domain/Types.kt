/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.recorder.domain

import android.net.Uri
import androidx.media3.common.MediaItem
import java.time.ZonedDateTime
import kotlin.time.Duration

data class Recording(
    val mediaItem: MediaItem,
    val title: String,
    val duration: Duration,
    val date: ZonedDateTime,
    val contentUri: Uri
)