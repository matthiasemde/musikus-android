package app.musikus.ui.activesession.recorder

import android.net.Uri
import app.musikus.services.RecorderServiceException
import app.musikus.usecase.recordings.Recording
import app.musikus.utils.DurationString
import app.musikus.utils.RecorderState

data class RecorderUiState(
    val recorderState: RecorderState,
    val recordingDuration: DurationString,
    val recordings: List<Recording>,
    val currentRawRecording: ShortArray?,
    val dialogUiState: RecorderDialogUiState
)

data class RecorderDialogUiState(
    val showDeleteRecordingDialog: Boolean,
    val saveRecordingDialogUiState: SaveRecordingDialogUiState?
)

data class SaveRecordingDialogUiState(
    val recordingName: String
)

sealed class RecorderUiEvent {
    data object StartRecording : RecorderUiEvent()

    data object PauseRecording : RecorderUiEvent()

    data object ResumeRecording : RecorderUiEvent()
    data object DeleteRecording : RecorderUiEvent()
    data object SaveRecording : RecorderUiEvent()
    data class LoadRecording(val contentUri: Uri) : RecorderUiEvent()

    data class RecordingNameChanged(val recordingName: String) : RecorderUiEvent()
    data object SaveRecordingDialogDismissed : RecorderUiEvent()
    data object DeleteRecordingDialogDismissed : RecorderUiEvent()
    data object SaveRecordingDialogConfirmed : RecorderUiEvent()
    data object DeleteRecordingDialogConfirmed : RecorderUiEvent()

}

typealias RecorderUiEventHandler = (event: RecorderUiEvent) -> Unit

sealed class RecorderException(message: String) : Exception(message) {
    data object NoMicrophonePermission : RecorderException("Microphone permission required")
    data object NoStoragePermission : RecorderException("Storage permission required")

    data object CouldNotLoadRecording : RecorderException("Could not load recording")

    data object ServiceNotFound : RecorderException("Cannot find recorder service")
    data class ServiceException(val exception: RecorderServiceException) : RecorderException(exception.message)
}