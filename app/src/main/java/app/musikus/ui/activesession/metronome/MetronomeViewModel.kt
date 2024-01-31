/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui.activesession.metronome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.usecase.userpreferences.UserPreferencesUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    }
}

data class MetronomeUiState(
    val settings: MetronomeSettings,
    val isPlaying: Boolean,
)

sealed class MetronomeUiEvent {
    data object ToggleIsPlaying : MetronomeUiEvent()
    data class ChangeBpm(val bpm: Int) : MetronomeUiEvent()
    data class ChangeBeatsPerBar(val beatsPerBar: Int) : MetronomeUiEvent()
    data class ChangeClicksPerBeat(val clicksPerBeat: Int) : MetronomeUiEvent()
}

@HiltViewModel
class MetronomeViewModel @Inject constructor(
    private val userPreferencesUseCases: UserPreferencesUseCases
) : ViewModel() {

    /** Imported flows */
    private val metronomeSettings = userPreferencesUseCases.getMetronomeSettings().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MetronomeSettings.DEFAULT
    )

    /** Own flows */
    private val _isPlaying = MutableStateFlow(false)

    /** Composing the Ui state */

    val uiState = combine(
        metronomeSettings,
        _isPlaying
    ) { settings, isPlaying ->
        MetronomeUiState(
            settings = settings,
            isPlaying = isPlaying
        )
    }

    /** Mutators */

    fun onUiEvent(event: MetronomeUiEvent) {
        when (event) {
            is MetronomeUiEvent.ToggleIsPlaying -> toggleIsPlaying()
            is MetronomeUiEvent.ChangeBpm -> changeBpm(event.bpm)
            is MetronomeUiEvent.ChangeBeatsPerBar -> changeBeatsPerBar(event.beatsPerBar)
            is MetronomeUiEvent.ChangeClicksPerBeat -> changeClicksPerBeat(event.clicksPerBeat)
        }
    }

    private fun toggleIsPlaying() {
        _isPlaying.value = !_isPlaying.value
    }

    private fun changeBpm(bpm: Int) {
        viewModelScope.launch {
            userPreferencesUseCases.changeMetronomeSettings(
                metronomeSettings.value.copy(bpm = bpm)
            )
        }
    }

    private fun changeBeatsPerBar(beatsPerBar: Int) {
        viewModelScope.launch {
            userPreferencesUseCases.changeMetronomeSettings(
                metronomeSettings.value.copy(beatsPerBar = beatsPerBar)
            )
        }
    }

    private fun changeClicksPerBeat(clicksPerBeat: Int) {
        viewModelScope.launch {
            userPreferencesUseCases.changeMetronomeSettings(
                metronomeSettings.value.copy(clicksPerBeat = clicksPerBeat)
            )
        }
    }
}