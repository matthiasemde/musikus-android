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
import kotlinx.coroutines.flow.update
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

        val BPM_RANGE = 40..240
        val BEATS_PER_BAR_RANGE = 1..12
        val CLICKS_PER_BEAT_RANGE = 1..4
    }
}

data class MetronomeUiState(
    val settings: MetronomeSettings,
    val sliderValue: Float,
    val tempoDescription: String,
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
    private val _sliderValue = MutableStateFlow(metronomeSettings.value.bpm.toFloat())

    /** Composing the Ui state */

    val uiState = combine(
        metronomeSettings,
        _isPlaying,
        _sliderValue
    ) { settings, isPlaying, sliderValue ->
        val tempoDescription = when(settings.bpm) {
            in 20 until 40 -> "Grave"
            in 40 until 55 -> "Largo"
            in 55 until 66 -> "Lento"
            in 66 until 76 -> "Adagio"
            in 76 until 92 -> "Andante"
            in 92 until 108 -> "Andante moderato"
            in 108 until 116 -> "Moderato"
            in 116 until 120 -> "Allegro moderato"
            in 120 until 156 -> "Allegro"
            in 156 until 172 -> "Vivace"
            in 172 until 200 -> "Presto"
            else -> "Prestissimo"
        }

        MetronomeUiState(
            settings = settings,
            tempoDescription = tempoDescription,
            isPlaying = isPlaying,
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
            tempoDescription = "Allegro",
            isPlaying = _isPlaying.value,
            sliderValue = _sliderValue.value
        )
    )

    /** Mutators */

    fun onUiEvent(event: MetronomeUiEvent) {
        when (event) {
            is MetronomeUiEvent.ToggleIsPlaying -> toggleIsPlaying()
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

    private fun toggleIsPlaying() {
        _isPlaying.value = !_isPlaying.value
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

    private fun tabTempo() {

    }
}