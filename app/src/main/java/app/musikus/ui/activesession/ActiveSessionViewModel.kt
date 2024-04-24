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
import app.musikus.database.entities.SectionCreationAttributes
import app.musikus.database.entities.SessionCreationAttributes
import app.musikus.di.ApplicationScope
import app.musikus.ui.theme.libraryItemColors
import app.musikus.usecase.activesession.ActiveSessionUseCases
import app.musikus.usecase.activesession.SessionTimerState
import app.musikus.usecase.library.LibraryUseCases
import app.musikus.usecase.sessions.SessionsUseCases
import app.musikus.utils.DurationFormat
import app.musikus.utils.getDurationString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
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
import kotlin.time.Duration.Companion.seconds


@HiltViewModel
class ActiveSessionViewModel @Inject constructor(
    application: Application,
    libraryUseCases: LibraryUseCases,
    private val activeSessionUseCases: ActiveSessionUseCases,
    private val sessionUseCases: SessionsUseCases,
    @ApplicationScope private val applicationScope: CoroutineScope
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
    private val _endDialogUiState = MutableStateFlow(ActiveSessionEndDialogUiState())


    /** ------------------- Sub UI states  ------------------------------------------- */

    private val topBarUiState = sessionTimerState.map {
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
        pastSectionsUiState,
        _endDialogUiState
    ) { timerUiState, currentItemUiState, pastSectionsUiState, endDialog ->
        MainContentUiState(
            timerUiState = timerUiState,
            currentItemUiState = currentItemUiState,
            pastSectionsUiState = pastSectionsUiState,
            endDialogUiState = endDialog
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

    val eventStates = MutableStateFlow(ActiveSessionEventStates())

    /** ------------------- Event Handler ------------------------------------------- */

    fun onUiEvent(event: ActiveSessionUiEvent) {
        when(event) {
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
            is ActiveSessionUiEvent.DeleteSection -> {
                viewModelScope.launch {
                    activeSessionUseCases.deleteSection(event.sectionId)
                }
            }
            is ActiveSessionUiEvent.EndDialogUiEvent -> {
                onEndDialogUiEvent(event.dialogEvent)
            }

            ActiveSessionUiEvent.DiscardSessionDialogConfirmed -> {
                activeSessionUseCases.reset()
                eventStates.update { it.copy(sessionDiscarded = true) }
            }
        }
    }

    private fun onEndDialogUiEvent(event: ActiveSessionEndDialogUiEvent) {
        when(event) {
            is ActiveSessionEndDialogUiEvent.CommentChanged -> {
                _endDialogUiState.update { it.copy(comment = event.comment) }
            }
            ActiveSessionEndDialogUiEvent.Confirmed -> {
                stopSession()
            }
            is ActiveSessionEndDialogUiEvent.RatingChanged -> {
                _endDialogUiState.update { it.copy(rating = event.rating) }
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

    private fun stopSession() {
        applicationScope.launch {
            val savableState = activeSessionUseCases.getFinalizedSession()   // complete running section

            // ignore empty sections (e.g. when paused and then stopped immediately))
            val sections = savableState.completedSections.filter { it.duration > 0.seconds }

            // store in database
            sessionUseCases.add(
                sessionCreationAttributes = SessionCreationAttributes(
                    // add up all pause durations
                    breakDuration = sections.fold(0.seconds) { acc, section ->
                        acc + section.pauseDuration
                    },
                    comment = _endDialogUiState.value.comment,
                    rating = _endDialogUiState.value.rating
                ),
                sectionCreationAttributes = sections.map { section ->
                    SectionCreationAttributes(
                        libraryItemId = section.libraryItem.id,
                        duration = section.duration,
                        startTimestamp = section.startTimestamp
                    )
                }
            )
            activeSessionUseCases.reset()   // reset the active session state
            eventStates.update { it.copy(sessionSaved = true) }
        }
    }
}