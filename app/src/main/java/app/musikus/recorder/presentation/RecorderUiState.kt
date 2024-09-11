/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.recorder.presentation

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.media3.common.MediaItem
import app.musikus.R
import app.musikus.core.presentation.utils.DurationString

@Stable
data class RecorderUiState(
    val recorderState: RecorderState,
    val recordingDuration: DurationString,
    val recordings: List<RecordingListItemUiState>,
    val currentPlaybackRawMedia: ShortArray?, // currently played media file as raw array
    val dialogUiState: RecorderDialogUiState
)

@Stable
data class RecordingListItemUiState(
    val title: String,
    val date: String,
    val duration: String,
    val showPlayerUi: Boolean,
    val mediaItem: MediaItem,
    val contentUri: Uri,
)

@Stable
data class RecorderDialogUiState(
    val showDeleteRecordingDialog: Boolean,
    val saveRecordingDialogUiState: SaveRecordingDialogUiState?
)

@Stable
data class SaveRecordingDialogUiState(
    val recordingName: String
)

sealed class RecorderUiEvent {
    data object StartRecording : RecorderUiEvent()

    data object PauseRecording : RecorderUiEvent()

    data object ResumeRecording : RecorderUiEvent()
    data object DeleteRecording : RecorderUiEvent()
    data object SaveRecording : RecorderUiEvent()
    data class LoadRecording(val contentUri: Uri?) : RecorderUiEvent()

    data class RecordingNameChanged(val recordingName: String) : RecorderUiEvent()
    data object SaveRecordingDialogDismissed : RecorderUiEvent()
    data object DeleteRecordingDialogDismissed : RecorderUiEvent()
    data object SaveRecordingDialogConfirmed : RecorderUiEvent()
    data object DeleteRecordingDialogConfirmed : RecorderUiEvent()
}

typealias RecorderUiEventHandler = (event: RecorderUiEvent) -> Unit

sealed class RecorderException(message: String) : Exception(message) {
    class NoMicrophonePermission(context: Context) : RecorderException(
        context.getString(R.string.recorder_exception_no_microphone_permission)
    )
    class NoStoragePermission(context: Context) : RecorderException(
        context.getString(R.string.recorder_exception_no_storage_permission)
    )
    class NoNotificationPermission(context: Context) : RecorderException(
        context.getString(R.string.recorder_exception_no_notification_permission)
    )

    class CouldNotLoadRecording(context: Context) : RecorderException(
        context.getString(R.string.recorder_exception_no_could_not_load)
    )

    class MediaStoreInsertFailed(context: Context) : RecorderException(
        context.getString(R.string.recorder_exception_media_store_failed_insert)
    )
    class MediaStoreUpdateFailed(context: Context) : RecorderException(
        context.getString(R.string.recorder_exception_media_store_failed_update)
    )
    class SaveWithEmptyName(context: Context) : RecorderException(
        context.getString(R.string.recorder_exception_save_with_empty_name)
    )

    class ServiceNotFound(context: Context) : RecorderException(
        context.getString(R.string.recorder_exception_no_service_not_found)
    )
    data class ServiceException(val exception: RecorderServiceException) : RecorderException(exception.message)
}
