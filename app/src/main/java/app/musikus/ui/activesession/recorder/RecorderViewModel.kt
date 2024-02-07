/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui.activesession.recorder

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.services.RecorderService
import app.musikus.services.RecorderServiceEvent
import app.musikus.services.RecorderServiceState
import app.musikus.usecase.permissions.PermissionsUseCases
import app.musikus.usecase.recordings.Recording
import app.musikus.usecase.recordings.RecordingsUseCases
import app.musikus.utils.DurationFormat
import app.musikus.utils.DurationString
import app.musikus.utils.getDurationString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds


data class RecorderUiState(
    val isRecording: Boolean,
    val recordingDuration: DurationString,
    val recordings: List<Recording>,
    val currentRawRecording: FloatArray?,
)

sealed class RecorderUiEvent {
    data object ToggleRecording : RecorderUiEvent()
    data class LoadRecording(val contentUri: Uri) : RecorderUiEvent()
}

typealias RecorderUiEventHandler = (event: RecorderUiEvent) -> Unit

sealed class RecorderException(message: String) : Exception(message) {
    data object NoMicrophonePermission : RecorderException("Microphone permission required")
    data object NoStoragePermission : RecorderException("Storage permission required")

    data object CouldNotLoadRecording : RecorderException("Could not load recording")
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecorderViewModel @Inject constructor(
    private val application: Application,
    private val permissionsUseCases: PermissionsUseCases,
    private val recordingsUseCases: RecordingsUseCases
) : AndroidViewModel(application) {

    /** ------------------ Recorder Service --------------------- */

    /** state wrapper and event handler */
    private val recorderServiceStateWrapper = MutableStateFlow<Flow<RecorderServiceState>?>(null)
    private val recorderServiceState = recorderServiceStateWrapper.flatMapLatest {
        it ?: flowOf(null)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = null
    )

    private var recorderServiceEventHandler: ((RecorderServiceEvent) -> Unit)? = null

    /** binding */

    private val recorderServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to SessionForegroundService, cast the Binder and get SessionService instance
            val binder = service as RecorderService.LocalBinder
            recorderServiceEventHandler = binder.getEventHandler()
            recorderServiceStateWrapper.update { binder.getServiceState() }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            recorderServiceEventHandler = null
            recorderServiceStateWrapper.update { null }
        }
    }

    init {
        // try to bind to Recorder and Media Player services
        bindRecorderService()
    }

    private fun startRecorderService() {
        val intent = Intent(application, RecorderService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            application.startForegroundService(intent)
        } else {
            application.startService(intent)
        }
    }

    private fun bindRecorderService() {
        // Bind the recorder service
        val recorderServiceBindIntent = Intent(application, RecorderService::class.java)
        application.bindService(recorderServiceBindIntent, recorderServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onCleared() {
        if (recorderServiceStateWrapper.value != null) {
            application.unbindService(recorderServiceConnection)
        }
        super.onCleared()
    }

    /** ------------------ Main ViewModel --------------------- */

    /** Own state flows */

    val exception = MutableStateFlow<Exception?>(null)

    private val _currentRecordingUri = MutableStateFlow<Uri?>(null)

    /** Imported Flows */

    private val recordings = recordingsUseCases.get().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val currentRawRecording = _currentRecordingUri.map { currentRawRecording ->
        currentRawRecording?.let {
            Log.d("RecorderViewModel", "Loading raw recording: $it")
            recordingsUseCases.getRawRecording(it).getOrElse {
                exception.update { RecorderException.CouldNotLoadRecording }
                return@map null
            }.also {
                Log.d("RecorderViewModel", "Loaded raw recording: ${it.size} samples")
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /** Composing the UI state */
    val uiState = combine(
        recorderServiceState,
        recordings,
        currentRawRecording
    ) { serviceState, recordings, currentRawRecording ->
        RecorderUiState(
            isRecording = serviceState?.isRecording ?: false,
            recordingDuration = getDurationString(
                (serviceState?.recordingDuration ?: 0.seconds),
                DurationFormat.HMSC_DIGITAL
            ),
            recordings = recordings,
            currentRawRecording = currentRawRecording
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecorderUiState(
            isRecording = false,
            recordingDuration = AnnotatedString(""),
            recordings = emptyList(),
            currentRawRecording = null
        )
    )


    fun onUiEvent(event: RecorderUiEvent) {
        when(event) {
            is RecorderUiEvent.ToggleRecording -> {
                viewModelScope.launch {
                    val recordingPermissionResult = permissionsUseCases.request(
                        listOf(android.Manifest.permission.RECORD_AUDIO)
                    )
                    if(recordingPermissionResult.isFailure) {
                        Toast.makeText(application, "Microphone permission required", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        val writeExternalStoragePermissionResult = permissionsUseCases.request(
                            listOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        )

                        if (writeExternalStoragePermissionResult.isFailure) {
                            Toast.makeText(application, "Storage permission required", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                    }

                    startRecorderService()
                    recorderServiceEventHandler?.invoke(RecorderServiceEvent.ToggleRecording)
                }
            }
            is RecorderUiEvent.LoadRecording -> _currentRecordingUri.update { event.contentUri }
        }
    }
}