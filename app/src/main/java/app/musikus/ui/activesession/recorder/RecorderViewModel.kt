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
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.services.RecorderService
import app.musikus.services.RecorderServiceEvent
import app.musikus.services.RecorderServiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject


data class RecorderUiState(
    val isRecording: Boolean,
    val recordingTime: String,
)

sealed class RecorderUiEvent {
    data object ToggleRecording : RecorderUiEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecorderViewModel @Inject constructor(
    private val application: Application
) : AndroidViewModel(application) {

    /** ------------------ Service --------------------- */

    /** Service state wrapper and event handler */
    private val serviceStateWrapper = MutableStateFlow<Flow<RecorderServiceState>?>(null)
    private val serviceState = serviceStateWrapper.flatMapLatest {
        it ?: flowOf(null)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = null
    )

    private var serviceEventHandler: ((RecorderServiceEvent) -> Unit)? = null

    /** Service binding */

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to SessionForegroundService, cast the Binder and get SessionService instance
            val binder = service as RecorderService.LocalBinder
            serviceEventHandler = binder.getEventHandler()
            serviceStateWrapper.update { binder.getServiceState() }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            serviceEventHandler = null
            serviceStateWrapper.update { null }
        }
    }

    init {
        // try to bind to SessionService
        bindService()
    }

    private fun startService() {
        val intent = Intent(application, RecorderService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            application.startForegroundService(intent)
        } else {
            application.startService(intent)
        }
    }

    private fun bindService() {
        val intent = Intent(application, RecorderService::class.java)
        application.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onCleared() {
        if (serviceStateWrapper.value != null) {
            application.unbindService(connection)
        }
        super.onCleared()
    }

    /** ------------------ Main ViewModel --------------------- */

    /** Composing the UI state */
    val uiState = serviceState.map {
        RecorderUiState(
            isRecording = it?.isRecording ?: false,
            recordingTime = "00:00"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecorderUiState(
            isRecording = false,
            recordingTime = "00:00"
        )
    )


    fun onUiEvent(event: RecorderUiEvent) {
        when(event) {
            is RecorderUiEvent.ToggleRecording -> {
                startService()
                serviceEventHandler?.invoke(RecorderServiceEvent.ToggleRecording)
            }
        }
    }
}