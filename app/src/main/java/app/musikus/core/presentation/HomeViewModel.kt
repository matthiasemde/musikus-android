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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class HomeUiState(
    val currentTab: Screen.HomeTab,
    val showMainMenu: Boolean,
)

typealias HomeUiEventHandler = (HomeUiEvent) -> Unit

sealed class HomeUiEvent {
    data class TabSelected(val tab: Screen.HomeTab) : HomeUiEvent()
    data object ShowMainMenu : HomeUiEvent()
    data object HideMainMenu : HomeUiEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    /**
     * Own state flows
     */

    // Current Tab
    private val _currentTab = MutableStateFlow<Screen.HomeTab>(Screen.HomeTab.defaultTab)

    // Menu
    private val _showMainMenu = MutableStateFlow(false)

    /**
     * Composing the ui state
     */

    val uiState = combine(
        _currentTab,
        _showMainMenu,
    ) { currentTab, showMainMenu ->
        HomeUiState(
            currentTab = currentTab,
            showMainMenu = showMainMenu,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(
            currentTab = _currentTab.value,
            showMainMenu = _showMainMenu.value,
        )
    )

    fun onUiEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.TabSelected -> {
                _currentTab.update { event.tab }
            }
            is HomeUiEvent.ShowMainMenu -> {
                _showMainMenu.update { true }
            }
            is HomeUiEvent.HideMainMenu -> {
                _showMainMenu.update { false }
            }
        }
    }
}
