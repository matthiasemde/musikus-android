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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
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

    // TODO: this is actually a 1:1 mapping still, maybe think about either re-using state
    //       from useCase or extend UI State functionality
    private val sessionState = activeSessionUseCases.getTimerState().map { state ->
        when(state) {
            SessionTimerState.NOT_STARTED -> ActiveSessionState.NOT_STARTED
            SessionTimerState.RUNNING -> ActiveSessionState.RUNNING
            SessionTimerState.PAUSED -> ActiveSessionState.PAUSED
            SessionTimerState.UNKNOWN -> ActiveSessionState.UNKNOWN // TODO evaluate what to do
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActiveSessionState.UNKNOWN
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

    private val allLibraryItems = combine(
        libraryFoldersWithItems,
        rootItems
    ) { folders, rootItems ->
        folders.flatMap { it.items } + rootItems
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val lastPracticedDates = allLibraryItems.flatMapLatest {
        libraryUseCases.getLastPracticedDate(it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )


    /** ------------------- Own StateFlow UI states  ------------------------------------------- */

    private val _endDialogUiState = MutableStateFlow(ActiveSessionEndDialogUiState())
    private val _dialogVisibilities = MutableStateFlow(ActiveSessionDialogsUiState())


    /** ------------------- Sub UI states  ------------------------------------------- */

    private val timerUiState = combine(
        sessionState,
        _clock  // should update with clock
    ) { timerState, _ ->
        val pause = timerState == ActiveSessionState.PAUSED

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
            subHeadingText = if (pause) "Paused $pauseDurStr" else "Practice Time",
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActiveSessionTimerUiState()
    )

    private val currentItemUiState: StateFlow<ActiveSessionCurrentItemUiState?> = combine(
        sessionState,
        runningLibraryItem,
        _clock  // should update with clock
    ) { sessionState, item, _ ->
        if (sessionState == ActiveSessionState.NOT_STARTED) {
            return@combine null
        }

        val currentItemDuration = try {
            activeSessionUseCases.getRunningItemDuration()
        } catch (e: IllegalStateException) {
            Duration.ZERO   // Session not yet started
        }

        ActiveSessionCurrentItemUiState(
            name = item?.name ?: "Not started",
            durationText = getDurationString(currentItemDuration, DurationFormat.MS_DIGITAL).toString(),
            color = libraryItemColors[item?.colorIndex ?: 0]
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val pastSectionsUiState  = completedSections.map {sections ->
        if (sections.isEmpty()) {
            return@map null
        }
        ActiveSessionCompletedSectionsUiState(
            items = sections.reversed().map {
               CompletedSectionUiState(
                   id = it.id,
                   name = it.libraryItem.name,
                   color = libraryItemColors[it.libraryItem.colorIndex],
                   durationText = getDurationString(it.duration, DurationFormat.MS_DIGITAL).toString()
               )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )


    private val mainContentUiState = MutableStateFlow(
        ActiveSessionContentUiState(
            timerUiState = timerUiState,
            currentItemUiState = currentItemUiState,
            pastSectionsUiState = pastSectionsUiState,
            endDialogUiState = _endDialogUiState
        )
    )

    private val newItemSelectorUiState = combine(
        runningLibraryItem,
        libraryFoldersWithItems,
        lastPracticedDates,
        rootItems
    ) { runningItem, folders, lastPracticedDates, rootItems ->
        NewItemSelectorUiState(
            runningItem = runningItem,
            foldersWithItems = folders,
            lastPracticedDates = lastPracticedDates,
            rootItems = rootItems
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NewItemSelectorUiState()
    )

    private val _toolsUiState = MutableStateFlow(ActiveSessionToolsUiState())

    /** ------------------- Main UI State ------------------------------------------- */


    val uiState = MutableStateFlow(
        ActiveSessionUiState(
            sessionState = sessionState,
            dialogVisibilities = _dialogVisibilities,
            mainContentUiState = mainContentUiState,
            newItemSelectorUiState = newItemSelectorUiState,
            toolsUiState = _toolsUiState,
        )
    ).asStateFlow()

    private val navigationChannel = Channel<NavigationEvent>()
    val navigationEventsChannelFlow = navigationChannel.receiveAsFlow()

    /** ------------------- Event Handler ------------------------------------------- */

    fun onUiEvent(event: ActiveSessionUiEvent) {
        when(event) {
            is ActiveSessionUiEvent.SelectItem -> {
                viewModelScope.launch {
                    // resume session if paused
                    if (sessionState.value == ActiveSessionState.PAUSED) {
                        activeSessionUseCases.resume()
                    }

                    // wait until the current item has been running for at least 1 second
                    if (sessionState.value != ActiveSessionState.NOT_STARTED
                        && activeSessionUseCases.getRunningItemDuration() < 1.seconds)
                    {
                        delay(1000)
                    }

                    activeSessionUseCases.selectItem(event.item)
                }
            }
            is ActiveSessionUiEvent.TogglePauseState -> {
                viewModelScope.launch {
                    when (sessionState.value) {
                        ActiveSessionState.RUNNING -> activeSessionUseCases.pause()
                        ActiveSessionState.PAUSED -> activeSessionUseCases.resume()
                        else -> {
                            Log.d(
                                "ActiveSessionViewModel",
                                "TogglePauseState: Timer state = ${sessionState.value}"
                            )
                        }
                    }
                }
            }
            is ActiveSessionUiEvent.BackPressed -> {}
            is ActiveSessionUiEvent.DeleteSection -> {
                viewModelScope.launch {
                    if (sessionState.value == ActiveSessionState.PAUSED) {
                        activeSessionUseCases.resume()
                    }
                    activeSessionUseCases.deleteSection(event.sectionId)
                }
            }
            is ActiveSessionUiEvent.EndDialogUiEvent -> {
                onEndDialogUiEvent(event.dialogEvent)
            }

            ActiveSessionUiEvent.DiscardSessionDialogConfirmed -> {
                activeSessionUseCases.reset()
                viewModelScope.launch {
                    navigationChannel.send(NavigationEvent.NavigateUp)
                }
            }

            ActiveSessionUiEvent.ToggleDiscardDialog -> _dialogVisibilities.update {
                it.copy(discardDialogVisible = !it.discardDialogVisible)
            }
            ActiveSessionUiEvent.ToggleFinishDialog -> _dialogVisibilities.update {
                it.copy(finishDialogVisible = !it.finishDialogVisible)
            }
            ActiveSessionUiEvent.ToggleNewItemSelector -> _dialogVisibilities.update {
                it.copy(newItemSelectorVisible = !it.newItemSelectorVisible)
            }
            ActiveSessionUiEvent.ToggleCreateFolderDialog -> _dialogVisibilities.update {
                it.copy(createFolderDialogVisible = !it.createFolderDialogVisible)
            }
            ActiveSessionUiEvent.ToggleCreateItemDialog -> _dialogVisibilities.update {
                it.copy(createItemDialogVisible = !it.createItemDialogVisible)
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

        /** Show item selector on startup */
        /*
        runBlocking (context = Dispatchers.IO) {
            viewModelScope.launch {
                // wait until session data has initialized
                while (sessionState.value == ActiveSessionState.UNKNOWN) {
                    delay(100)
                }
                if (sessionState.value == ActiveSessionState.NOT_STARTED) {
                    viewModelScope.launch {
                        _dialogVisibilities.update { it.copy(newItemSelectorVisible = true) }
                    }
                }
            }
        }
        */
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
            viewModelScope.launch {
                navigationChannel.send(NavigationEvent.NavigateUp)
            }
        }
    }
}

sealed interface NavigationEvent {
    object NavigateUp: NavigationEvent
}