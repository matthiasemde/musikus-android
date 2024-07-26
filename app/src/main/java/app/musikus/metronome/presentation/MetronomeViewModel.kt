/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.metronome.presentation

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.R
import app.musikus.core.presentation.utils.UiText
import app.musikus.permissions.domain.usecase.PermissionsUseCases
import app.musikus.settings.domain.usecase.UserPreferencesUseCases
import app.musikus.permissions.domain.PermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.round
import kotlin.time.Duration.Companion.minutes

data class MetronomeSettings(
    val bpm: Int,
    val beatsPerBar: Int,
    val clicksPerBeat: Int
) {
    companion object {
        val DEFAULT = MetronomeSettings(
            bpm = 120,
            beatsPerBar = 4,
            clicksPerBeat = 1
        )

        val BPM_RANGE = 40..240
        val BEATS_PER_BAR_RANGE = 1..12
        val CLICKS_PER_BEAT_RANGE = 1..20
    }
}

data class MetronomeUiState(
    val settings: MetronomeSettings,
    val sliderValue: Float,
    val tempoDescription: UiText,
    val isPlaying: Boolean,
)

sealed class MetronomeUiEvent {
    data object ToggleIsPlaying : MetronomeUiEvent()

    data class UpdateSliderValue(val value: Float) : MetronomeUiEvent()
    data class IncrementBpm(val increment: Int) : MetronomeUiEvent()
    data object IncrementBeatsPerBar : MetronomeUiEvent()
    data object DecrementBeatsPerBar : MetronomeUiEvent()
    data object IncrementClicksPerBeat : MetronomeUiEvent()
    data object DecrementClicksPerBeat : MetronomeUiEvent()

    data object TabTempo : MetronomeUiEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MetronomeViewModel @Inject constructor(
    private val userPreferencesUseCases: UserPreferencesUseCases,
    private val permissionsUseCases: PermissionsUseCases,
    private val application: Application
) : AndroidViewModel(application) {

    /** ------------------ Service --------------------- */

    /** Service state wrapper and event handler */
    private val serviceStateWrapper = MutableStateFlow<Flow<MetronomeServiceState>?>(null)
    private val serviceState = serviceStateWrapper.flatMapLatest {
        it ?: flowOf(null)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = null
    )

    private var serviceEventHandler: ((MetronomeServiceEvent) -> Unit)? = null

    /** Service binding */

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to SessionForegroundService, cast the Binder and get SessionService instance
            val binder = service as MetronomeService.LocalBinder
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
        val intent = Intent(application, MetronomeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            application.startForegroundService(intent)
        } else {
            application.startService(intent)
        }
    }

    private fun bindService() {
        val intent = Intent(application, MetronomeService::class.java)
        application.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onCleared() {
        if (serviceStateWrapper.value != null) {
            application.unbindService(connection)
        }
        super.onCleared()
    }

    /** ------------------ Main ViewModel --------------------- */

    private fun tempoToUiText(tempo: Int) = UiText.StringResource(resId = when(tempo) {
        in 20 until 40 -> R.string.metronome_tempo_grave
        in 40 until 55 -> R.string.metronome_tempo_largo
        in 55 until 66 -> R.string.metronome_tempo_lento
        in 66 until 76 -> R.string.metronome_tempo_adagio
        in 76 until 92 -> R.string.metronome_tempo_andante
        in 92 until 108 -> R.string.metronome_tempo_andante_moderato
        in 108 until 116 -> R.string.metronome_tempo_moderato
        in 116 until 120 -> R.string.metronome_tempo_allegro_moderato
        in 120 until 156 -> R.string.metronome_tempo_allegro
        in 156 until 172 -> R.string.metronome_tempo_vivace
        in 172 until 200 -> R.string.metronome_tempo_presto
        else -> R.string.metronome_tempo_prestissimo
    })

    /** Imported flows */
    private val metronomeSettings = userPreferencesUseCases.getMetronomeSettings().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MetronomeSettings.DEFAULT
    )

    /** Own flows */
    private val _sliderValue = MutableStateFlow(metronomeSettings.value.bpm.toFloat())
    private val _exceptionChannel = Channel<MetronomeException>()
    val exceptionChannel = _exceptionChannel.receiveAsFlow()

    /** Composing the Ui state */

    val uiState = combine(
        metronomeSettings,
        serviceState,
        _sliderValue
    ) { settings, serviceState, sliderValue ->
        MetronomeUiState(
            settings = settings,
            tempoDescription = tempoToUiText(settings.bpm),
            isPlaying = serviceState?.isPlaying ?: false,
            sliderValue = if(sliderValue.toInt() != settings.bpm) {
                settings.bpm.toFloat()
            } else {
                sliderValue
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MetronomeUiState(
            settings = metronomeSettings.value,
            tempoDescription = tempoToUiText(metronomeSettings.value.bpm),
            isPlaying = serviceState.value?.isPlaying ?: false,
            sliderValue = _sliderValue.value
        )
    )

    /** Mutators */

    fun onUiEvent(event: MetronomeUiEvent) {
        when (event) {
            is MetronomeUiEvent.ToggleIsPlaying -> {
                viewModelScope.launch {
                    if (serviceState.value?.isPlaying != true) {
                        if (!checkPermissions()) {
                            _exceptionChannel.send(MetronomeException.NoNotificationPermission)
                            return@launch
                        }
                        startService()
                    }
                    serviceEventHandler?.invoke(
                        MetronomeServiceEvent.TogglePlaying
                    )
                }
            }
            is MetronomeUiEvent.UpdateSliderValue -> updateSliderValue(event.value)
            is MetronomeUiEvent.IncrementBpm -> changeBpm(
                metronomeSettings.value.bpm + event.increment
            )
            is MetronomeUiEvent.IncrementBeatsPerBar -> incrementBeatsPerBar(1)
            is MetronomeUiEvent.DecrementBeatsPerBar -> incrementBeatsPerBar(-1)

            is MetronomeUiEvent.IncrementClicksPerBeat -> incrementClicksPerBeat(1)
            is MetronomeUiEvent.DecrementClicksPerBeat -> incrementClicksPerBeat(-1)

            is MetronomeUiEvent.TabTempo -> tabTempo()
        }
    }

    private suspend fun checkPermissions(): Boolean {
        // Only check for POST_NOTIFICATIONS permission on Android 12 and above
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        val result = permissionsUseCases.request(
            listOf(android.Manifest.permission.POST_NOTIFICATIONS)
        ).exceptionOrNull()
        return result !is PermissionChecker.PermissionsDeniedException
    }

    private fun updateSliderValue(value: Float) {
        _sliderValue.update { value }
        if(value.toInt() != metronomeSettings.value.bpm) {
            changeBpm(value.toInt())
        }
    }

    private fun changeBpm(bpm: Int) {
        viewModelScope.launch {
            userPreferencesUseCases.changeMetronomeSettings(
                metronomeSettings.value.copy(
                    bpm = bpm.coerceIn(MetronomeSettings.BPM_RANGE)
                )
            )
        }
    }

    private fun incrementBeatsPerBar(increment: Int) {
        viewModelScope.launch {
            userPreferencesUseCases.changeMetronomeSettings(
                metronomeSettings.value.copy(
                    beatsPerBar = (
                        metronomeSettings.value.beatsPerBar + increment
                    ).coerceIn(MetronomeSettings.BEATS_PER_BAR_RANGE)
                )
            )
        }
    }

    private fun incrementClicksPerBeat(increment: Int) {
        viewModelScope.launch {
            userPreferencesUseCases.changeMetronomeSettings(
                metronomeSettings.value.copy(
                    clicksPerBeat = (
                        metronomeSettings.value.clicksPerBeat + increment
                    ).coerceIn(MetronomeSettings.CLICKS_PER_BEAT_RANGE)
                )
            )
        }
    }

    private var lastTab = System.currentTimeMillis()

    private fun tabTempo() {
        val currentBpm = metronomeSettings.value.bpm

        val now = System.currentTimeMillis()
        val bpmSinceLastTab = 1.minutes.inWholeMilliseconds.toFloat() / (now - lastTab)
        val alpha = when(abs(currentBpm - bpmSinceLastTab)) {
            in 0f..10f -> 0.1f
            in 10f..20f -> 0.2f
            in 20f..50f -> 0.3f
            else -> 1f
        }
        val runningAverageBpm = (1 - alpha) * currentBpm + alpha * bpmSinceLastTab

        changeBpm(round(runningAverageBpm).toInt())

        lastTab = now
    }
}

sealed class MetronomeException(message: String) : Exception(message) {
    object NoNotificationPermission : MetronomeException("Notification permission required")
}