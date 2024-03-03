/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.datastore.ColorSchemeSelections
import app.musikus.datastore.ThemeSelections
import app.musikus.services.SessionService
import app.musikus.services.SessionServiceState
import app.musikus.usecase.userpreferences.UserPreferencesUseCases
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


typealias MainUiEventHandler = (MainUiEvent) -> Unit

sealed class MainUiEvent {
    data class ShowSnackbar(val message: String, val onUndo: (() -> Unit)? = null): MainUiEvent()
}

data class MainUiState(
    val activeTheme: ThemeSelections?,
    val activeColorScheme: ColorSchemeSelections?,
    var snackbarHost: SnackbarHostState,
    val isSessionActive: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application,
    userPreferencesUseCases: UserPreferencesUseCases,
) : ViewModel() {


    /**
     * Private state variables
     */

    /** Snackbar */
    private val _snackbarHost = MutableStateFlow(SnackbarHostState())

    /** Theme */
    private val _activeTheme = userPreferencesUseCases.getTheme().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _activeColorScheme = userPreferencesUseCases.getColorScheme().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /**
     * Imported flows
     */

    private val activeSessionServiceStateWrapper = MutableStateFlow<Flow<SessionServiceState>?>(null)
    private val activeSessionState = activeSessionServiceStateWrapper.flatMapLatest {
        it ?: flowOf(null)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /**
     * Transforming and combining imported and own flows
     */

    private val isSessionActive = activeSessionState.map { sessionState ->
        sessionState?.sections?.isNotEmpty() == true
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    /**
     * Composing the ui state
     */

    val uiState = combine(
        _activeTheme,
        _activeColorScheme,
        _snackbarHost,
        isSessionActive
    ) { activeTheme, activeColorScheme, snackbarHost, isSessionActive ->
        MainUiState(
            activeTheme = activeTheme,
            activeColorScheme = activeColorScheme,
            snackbarHost = snackbarHost,
            isSessionActive = isSessionActive
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(
            activeTheme = _activeTheme.value,
            activeColorScheme = _activeColorScheme.value,
            snackbarHost = _snackbarHost.value,
            isSessionActive = isSessionActive.value
        )
    )


    fun onUiEvent(event: MainUiEvent) {
        when(event) {
            is MainUiEvent.ShowSnackbar -> {
                showSnackbar(event.message, event.onUndo)
            }
        }
    }

    /**
     * Private state mutators
     */

    private fun showSnackbar(message: String, onUndo: (() -> Unit)? = null) {
        viewModelScope.launch {
            val result = _snackbarHost.value.showSnackbar(
                message,
                actionLabel = if(onUndo != null) "Undo" else null,
                duration = SnackbarDuration.Long
            )
            when(result) {
                SnackbarResult.ActionPerformed -> {
                    onUndo?.invoke()
                }
                SnackbarResult.Dismissed -> {
                    // do nothing
                }
            }
        }
    }


    /**
     * Active session service binding
     */

    private val connection = object : ServiceConnection {
        /** called by service when we have connection to the service => we have mService reference */
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to SessionForegroundService, cast the Binder and get SessionService instance
            val binder = service as SessionService.LocalBinder
            activeSessionServiceStateWrapper.update { binder.getServiceState() }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
        }
    }

    private fun bindService() {
        val intent = Intent(application, SessionService::class.java) // Build the intent for the service
        application.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onCleared() {
        if (activeSessionState.value != null) {
            application.unbindService(connection)
        }
        super.onCleared()
    }

    init {
//        bindService()  // TODO: this breaks sessionservice after finishing a session
    }
}