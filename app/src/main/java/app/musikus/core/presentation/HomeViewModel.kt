/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.core.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class HomeUiState(
    val showMainMenu: Boolean,
)

typealias HomeUiEventHandler = (HomeUiEvent) -> Boolean

sealed class HomeUiEvent {
    data object ShowMainMenu : HomeUiEvent()
    data object HideMainMenu : HomeUiEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    /**
     * Own state flows
     */

    // Menu
    private val _showMainMenu = MutableStateFlow(false)

    /**
     * Composing the ui state
     */

    val uiState = _showMainMenu.map { showMainMenu ->
        HomeUiState(
            showMainMenu = showMainMenu,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(
            showMainMenu = _showMainMenu.value,
        )
    )

    fun onUiEvent(event: HomeUiEvent) : Boolean {
        when (event) {
            is HomeUiEvent.ShowMainMenu -> {
                _showMainMenu.update { true }
            }
            is HomeUiEvent.HideMainMenu -> {
                _showMainMenu.update { false }
            }
        }

        // events are consumed by default
        return true
    }
}
