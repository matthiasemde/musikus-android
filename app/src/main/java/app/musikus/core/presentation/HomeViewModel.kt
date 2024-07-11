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
import app.musikus.core.presentation.components.MultiFabState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class HomeUiState(
    val currentTab: Screen.HomeTab,
    val multiFabState: MultiFabState,
    val showMainMenu: Boolean,
)

typealias HomeUiEventHandler = (HomeUiEvent) -> Unit

sealed class HomeUiEvent {
    data class TabSelected(val tab: Screen.HomeTab) : HomeUiEvent()
    data object ShowMainMenu: HomeUiEvent()
    data object HideMainMenu: HomeUiEvent()
    data object ExpandMultiFab: HomeUiEvent()
    data object CollapseMultiFab: HomeUiEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
) : ViewModel() {

    /**
     * Own state flows
     */

    // Current Tab
    private val _currentTab = MutableStateFlow<Screen.HomeTab>(Screen.HomeTab.defaultTab)

    // Content Scrim over NavBar for Multi FAB etc
    private val _multiFabState = MutableStateFlow(MultiFabState.COLLAPSED)

    // Menu
    private val _showMainMenu = MutableStateFlow(false)

    /**
     * Composing the ui state
     */

    val uiState = combine(
        _currentTab,
        _showMainMenu,
        _multiFabState,
    ) { currentTab, showMainMenu, multiFabState ->
        HomeUiState(
            currentTab = currentTab,
            multiFabState = multiFabState,
            showMainMenu = showMainMenu,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(
            currentTab = _currentTab.value,
            multiFabState = _multiFabState.value,
            showMainMenu = _showMainMenu.value,
        )
    )

    fun onUiEvent(event: HomeUiEvent) {
        when(event) {
            is HomeUiEvent.TabSelected -> {
                _currentTab.update { event.tab }
            }
            is HomeUiEvent.ShowMainMenu -> {
                _showMainMenu.update { true }
            }
            is HomeUiEvent.HideMainMenu -> {
                _showMainMenu.update { false }
            }
            is HomeUiEvent.ExpandMultiFab -> {
                _multiFabState.update { MultiFabState.EXPANDED }
            }
            is HomeUiEvent.CollapseMultiFab -> {
                _multiFabState.update { MultiFabState.COLLAPSED }
            }
        }
    }
}