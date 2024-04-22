/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 *
 */

package app.musikus.ui.activesession

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.database.Nullable
import app.musikus.ui.theme.libraryItemColors
import app.musikus.usecase.activesession.ActiveSessionUseCases
import app.musikus.usecase.activesession.SessionTimerState
import app.musikus.usecase.library.LibraryUseCases
import app.musikus.utils.DurationFormat
import app.musikus.utils.getDurationString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.timer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


@HiltViewModel
class ActiveSessionViewModel @Inject constructor(
    application: Application,
    private val activeSessionUseCases: ActiveSessionUseCases,
    libraryUseCases: LibraryUseCases
) : AndroidViewModel(application) {

    private var _clock = MutableStateFlow(false)
    private var _timer: Timer? = null

    /** ------------------- Proxies for useCases ------------------------------------------- */

    private val completedSections = activeSessionUseCases.getCompletedSections().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val runningLibraryItem = activeSessionUseCases.getRunningItem().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val sessionTimerState = activeSessionUseCases.getTimerState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SessionTimerState.NOT_STARTED
    )

    private val libraryFoldersWithItems = libraryUseCases.getSortedFolders().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val rootItems = libraryUseCases.getSortedItems(Nullable(null)).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /** ------------------- Own StateFlows  ------------------------------------------- */

    private val _newItemSelectorVisible = MutableStateFlow(false)

    /** ------------------- Sub UI states  ------------------------------------------- */

    private val topBarUiState = sessionTimerState.map {
        Log.d("TAG", "sessionTimerState = $it")
        ActiveSessionTopBarUiState(
            visible =  it != SessionTimerState.NOT_STARTED,
            pauseButtonAppearance = if(it == SessionTimerState.PAUSED) {
                SessionPausedResumedState.PAUSED
            } else {
                SessionPausedResumedState.RUNNING
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActiveSessionTopBarUiState()
    )

    private val timerUiState = combine(
        sessionTimerState,
        _clock  // should update with clock
    ) { timerState, _ ->
        val pause = timerState == SessionTimerState.PAUSED

        val practiceDuration = try {
            activeSessionUseCases.getPracticeDuration()
        } catch (e: IllegalStateException) {
            Duration.ZERO   // Session not yet started
        }
        val pauseDurStr = getDurationString(
            activeSessionUseCases.getOngoingPauseDuration(),
            DurationFormat.MS_DIGITAL
        )
        ActiveSessionTimerUiState(
            timerText = getDurationString(practiceDuration, DurationFormat.MS_DIGITAL).toString(),
            subHeadingAppearance = if(pause) SessionPausedResumedState.PAUSED else SessionPausedResumedState.RUNNING,
            subHeadingText = if (pause) "Paused $pauseDurStr" else "Practice Time",
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActiveSessionTimerUiState()
    )

    private val currentItemUiState = combine(
        sessionTimerState,
        runningLibraryItem,
        _clock  // should update with clock
    ) { timerState, item, _ ->

        val currentItemDuration = try {
            activeSessionUseCases.getRunningItemDuration()
        } catch (e: IllegalStateException) {
            Duration.ZERO   // Session not yet started
        }
        ActiveSessionCurrentItemUiState(
            visible = timerState != SessionTimerState.NOT_STARTED,
            name = item?.name ?: "Not started",
            durationText = getDurationString(currentItemDuration, DurationFormat.MS_DIGITAL).toString(),
            color = libraryItemColors[item?.colorIndex ?: 0]
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActiveSessionCurrentItemUiState()
    )

    private val pastSectionsUiState = completedSections.map { sections ->
        ActiveSessionCompletedSectionsUiState(
            visible = sections.isNotEmpty(),
            items = sections.reversed().map {
                   CompletedSectionUiState(
                       id = it.id,
                       name = it.libraryItem.name,
                       color = libraryItemColors[it.libraryItem.colorIndex],
                       durationText = getDurationString(it.duration, DurationFormat.MS_DIGITAL).toString()
                   )
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActiveSessionCompletedSectionsUiState()
    )

    private val mainContentUiState = combine(
        timerUiState,
        currentItemUiState,
        pastSectionsUiState
    ) { timerUiState, currentItemUiState, pastSectionsUiState ->
        MainContentUiState(
            timerUiState = timerUiState,
            currentItemUiState = currentItemUiState,
            pastSectionsUiState = pastSectionsUiState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainContentUiState()
    )

    private val newItemSelectorUiState = combine(
        _newItemSelectorVisible,
        runningLibraryItem,
        libraryFoldersWithItems,
        rootItems
    ) { visible, runningItem, folders, rootItems ->
        NewItemSelectorUiState(
            visible = visible,
            runningItem = runningItem,
            foldersWithItems = folders,
            rootItems = rootItems
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NewItemSelectorUiState()
    )

    private val toolsUiState = MutableStateFlow(ActiveSessionToolsUiState())

    /** ------------------- Main UI State ------------------------------------------- */

    val uiState = combine(
        topBarUiState,
        mainContentUiState,
        newItemSelectorUiState,
        toolsUiState,
    ) { topBar, mainContent, newItem, tools ->
        ActiveSessionUiState(
            topBarUiState = topBar,
            mainContentUiState = mainContent,
            newItemSelectorUiState = newItem,
            toolsUiState = tools
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActiveSessionUiState()
    )

    /** ------------------- Event Handler ------------------------------------------- */

    fun onUiEvent(event: ActiveSessionUiEvent) {
        when(event) {
            is ActiveSessionUiEvent.ToggleNewItemSelectorVisible -> {
                _newItemSelectorVisible.update { !it }
            }
            is ActiveSessionUiEvent.SelectItem -> {
                viewModelScope.launch {
                    activeSessionUseCases.selectItem(event.item)
                }
            }
            is ActiveSessionUiEvent.ShowMetronome -> {}
            is ActiveSessionUiEvent.ShowRecorder -> {}
            is ActiveSessionUiEvent.TogglePauseState -> {
                viewModelScope.launch {
                    when (sessionTimerState.value) {
                        SessionTimerState.RUNNING -> activeSessionUseCases.pause()
                        SessionTimerState.PAUSED -> activeSessionUseCases.resume()
                        else -> {
                            Log.d(
                                "ActiveSessionViewModel",
                                "TogglePauseState: Timer state = ${sessionTimerState.value}"
                            )
                        }
                    }
                }
            }
            is ActiveSessionUiEvent.BackPressed -> {}
            is ActiveSessionUiEvent.ShowDiscardSessionDialog -> {
                Log.d("TAG", "ShowDiscardSessionDialog")
            }
            is ActiveSessionUiEvent.ShowFinishDialog -> {}
            is ActiveSessionUiEvent.DeleteSection -> {
                viewModelScope.launch {
                    activeSessionUseCases.deleteSection(event.sectionId)
                }
            }
        }
    }


    /** ---------------------------------- private methods ----------------------------------- */

    init {
        startTimer()
    }

    private fun startTimer() {
        if (_timer != null) {
            return
        }
        _timer = timer(
            name = "Timer",
            initialDelay = 0,
            period = 100.milliseconds.inWholeMilliseconds
        ) {
            _clock.update { !it }
        }
    }
}