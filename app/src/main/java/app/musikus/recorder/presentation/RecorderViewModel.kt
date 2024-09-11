/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.recorder.presentation

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.core.domain.DateFormat
import app.musikus.core.domain.TimeProvider
import app.musikus.core.domain.musikusFormat
import app.musikus.core.presentation.utils.DurationFormat
import app.musikus.core.presentation.utils.getDurationString
import app.musikus.permissions.domain.PermissionChecker
import app.musikus.permissions.domain.usecase.PermissionsUseCases
import app.musikus.recorder.domain.usecase.RecordingsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecorderViewModel @Inject constructor(
    private val application: Application,
    private val permissionsUseCases: PermissionsUseCases,
    private val recordingsUseCases: RecordingsUseCases,
    private val timeProvider: TimeProvider
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
        var exceptionForwardingJob: Job? = null

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to SessionForegroundService, cast the Binder and get SessionService instance
            val binder = service as RecorderService.LocalBinder
            recorderServiceEventHandler = binder.getEventHandler()
            recorderServiceStateWrapper.update { binder.getServiceState() }

            exceptionForwardingJob = viewModelScope.launch {
                binder.getExceptionChannel().collect {
                    _exceptionChannel.send(RecorderException.ServiceException(it))
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            recorderServiceEventHandler = null
            recorderServiceStateWrapper.update { null }
            exceptionForwardingJob?.cancel()
        }
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
        application.bindService(
            recorderServiceBindIntent,
            recorderServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onCleared() {
        if (recorderServiceStateWrapper.value != null) {
            application.unbindService(recorderServiceConnection)
        }
        super.onCleared()
    }

    /** ------------------ Main ViewModel --------------------- */

    /** Own state flows */

    private val _exceptionChannel = Channel<RecorderException>()

    private val _currentRecordingUri = MutableStateFlow<Uri?>(null)

    private val _readPermissionsGranted = MutableStateFlow(
        // after android 10, we don't need to request read permissions
        value = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    )

    private val _showDeleteRecordingDialog = MutableStateFlow(false)

    private val _showSaveRecordingDialog = MutableStateFlow(false)
    private val _recordingName = MutableStateFlow("")

    /** Imported Flows */

    private val recordings = _readPermissionsGranted.flatMapLatest { readPermissionsGranted ->
        if (!readPermissionsGranted) {
            return@flatMapLatest flowOf(emptyList())
        }

        recordingsUseCases.get().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    private val currentRawRecording = _currentRecordingUri.flatMapLatest { currentRawRecording ->
        flow {
            emit(null)

            emit(
                currentRawRecording?.let {
                    recordingsUseCases.getRawRecording(it).getOrElse {
                        _exceptionChannel.send(RecorderException.CouldNotLoadRecording(application))
                        null
                    }
                }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /** Composing the UI state */

    private val saveRecordingDialogUiState = combine(
        _showSaveRecordingDialog,
        _recordingName
    ) { showSaveRecordingDialog, recordingName ->
        if (!showSaveRecordingDialog) return@combine null

        SaveRecordingDialogUiState(recordingName)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val dialogUiState = combine(
        _showDeleteRecordingDialog,
        saveRecordingDialogUiState
    ) { showDeleteRecordingDialog, saveRecordingDialogUiState ->
        RecorderDialogUiState(
            showDeleteRecordingDialog,
            saveRecordingDialogUiState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecorderDialogUiState(
            showDeleteRecordingDialog = false,
            saveRecordingDialogUiState = null
        )
    )

    val uiState = combine(
        recorderServiceState,
        recordings,
        currentRawRecording,
        _currentRecordingUri, // TODO check if combine cycle b.c. rawRec. depends on _curr.R.Uri
        dialogUiState
    ) { serviceState, recordings, currentRawRecording, currentUri, dialogUiState ->
        RecorderUiState(
            recorderState = serviceState?.recorderState ?: RecorderState.UNINITIALIZED,
            recordingDuration = getDurationString(
                (serviceState?.recordingDuration ?: 0.seconds),
                DurationFormat.MSC_DIGITAL
            ),
            recordings = recordings.map {
                RecordingListItemUiState(
                    title = it.title,
                    date = it.date.musikusFormat(DateFormat.DAY_MONTH_YEAR),
                    duration = getDurationString(it.duration, DurationFormat.MS_DIGITAL).toString(),
                    mediaItem = it.mediaItem,
                    contentUri = it.contentUri,
                    showPlayerUi = it.contentUri == currentUri
                )
            },
            currentPlaybackRawMedia = currentRawRecording,
            dialogUiState = dialogUiState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecorderUiState(
            recorderState = RecorderState.UNINITIALIZED,
            recordingDuration = AnnotatedString(""),
            recordings = emptyList(),
            currentPlaybackRawMedia = null,
            dialogUiState = dialogUiState.value
        )
    )

    // TODO split out subfunctions
    fun onUiEvent(event: RecorderUiEvent) {
        when (event) {
            is RecorderUiEvent.StartRecording -> { viewModelScope.launch { startRecording() } }
            is RecorderUiEvent.PauseRecording -> pauseRecording()
            is RecorderUiEvent.ResumeRecording -> {
                recorderServiceEventHandler?.invoke(RecorderServiceEvent.ResumeRecording)
                    ?: viewModelScope.launch {
                        _exceptionChannel.send(RecorderException.ServiceNotFound(application))
                    }
            }
            is RecorderUiEvent.DeleteRecording -> {
                if (recorderServiceState.value?.recorderState == RecorderState.RECORDING) {
                    pauseRecording()
                }
                _showDeleteRecordingDialog.update { true }
            }
            is RecorderUiEvent.DeleteRecordingDialogDismissed -> _showDeleteRecordingDialog.update { false }
            is RecorderUiEvent.DeleteRecordingDialogConfirmed -> {
                _showDeleteRecordingDialog.update { false }
                recorderServiceEventHandler?.invoke(RecorderServiceEvent.DeleteRecording)
                    ?: viewModelScope.launch {
                        _exceptionChannel.send(RecorderException.ServiceNotFound(application))
                    }
            }
            is RecorderUiEvent.SaveRecording -> {
                if (recorderServiceState.value?.recorderState == RecorderState.RECORDING) {
                    pauseRecording()
                }
                _showSaveRecordingDialog.update { true }
                _recordingName.update {
                    "Musikus_${
                        timeProvider.now().musikusFormat(DateFormat.RECORDING)
                    }"
                }
            }
            is RecorderUiEvent.SaveRecordingDialogConfirmed -> {
                _showSaveRecordingDialog.update { false }
                recorderServiceEventHandler?.invoke(
                    RecorderServiceEvent.SaveRecording(_recordingName.value)
                ) ?: viewModelScope.launch {
                    _exceptionChannel.send(RecorderException.ServiceNotFound(application))
                }
            }
            is RecorderUiEvent.RecordingNameChanged -> _recordingName.update { event.recordingName }
            is RecorderUiEvent.SaveRecordingDialogDismissed -> _showSaveRecordingDialog.update { false }
            is RecorderUiEvent.LoadRecording -> _currentRecordingUri.update { event.contentUri }
        }
    }

    val exceptionChannel = _exceptionChannel.receiveAsFlow()

    /**
     *  --------------- Private methods ---------------
     */

    private suspend fun checkAndRequestPermissions(): Boolean {
        var permissionList = listOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionList = permissionList.plus(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Under API 29, we need WRITE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissionList = permissionList.plus(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        val permissionsRequestResult = permissionsUseCases.request(
            permissionList
        ).exceptionOrNull()

        if (permissionsRequestResult is PermissionChecker.PermissionsDeniedException) {
            if (permissionsRequestResult.permissions.contains(Manifest.permission.RECORD_AUDIO)) {
                _exceptionChannel.send(RecorderException.NoMicrophonePermission(application))
            }
            if (permissionsRequestResult.permissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                _exceptionChannel.send(RecorderException.NoStoragePermission(application))
            }
            if (permissionsRequestResult.permissions.contains(Manifest.permission.POST_NOTIFICATIONS)) {
                _exceptionChannel.send(RecorderException.NoNotificationPermission(application))
            }
            return false
        }
        return true
    }

    private suspend fun startRecording() {
        if (!checkAndRequestPermissions()) {
            return
        }
        startRecorderService()
        recorderServiceEventHandler?.invoke(RecorderServiceEvent.StartRecording)
            ?: _exceptionChannel.send(RecorderException.ServiceNotFound(application))
    }

    private fun pauseRecording() {
        recorderServiceEventHandler?.invoke(RecorderServiceEvent.PauseRecording)
            ?: viewModelScope.launch {
                _exceptionChannel.send(RecorderException.ServiceNotFound(application))
            }
    }

    init {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            viewModelScope.launch {
                val storagePermissionResult = permissionsUseCases.request(
                    listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                )
                if (storagePermissionResult.isSuccess) {
                    _readPermissionsGranted.update { true }
                } else {
                    _exceptionChannel.send(RecorderException.NoStoragePermission(application))
                }
            }
        }

        // try to bind to Recorder and Media Player services
        bindRecorderService()
    }
}
