/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 *
 */

package app.musikus.activesession.presentation

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.core.data.Nullable
import app.musikus.library.data.daos.LibraryItem
import app.musikus.sessions.data.entities.SectionCreationAttributes
import app.musikus.sessions.data.entities.SessionCreationAttributes
import app.musikus.core.di.ApplicationScope
import app.musikus.core.presentation.theme.libraryItemColors
import app.musikus.activesession.domain.usecase.ActiveSessionUseCases
import app.musikus.activesession.domain.usecase.SessionStatus
import app.musikus.library.domain.usecase.LibraryUseCases
import app.musikus.permissions.domain.usecase.PermissionsUseCases
import app.musikus.sessions.domain.usecase.SessionsUseCases
import app.musikus.core.presentation.utils.DurationFormat
import app.musikus.permissions.domain.PermissionChecker
import app.musikus.core.presentation.utils.getDurationString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.runBlocking
import java.util.Timer
import java.util.UUID
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
    private val permissionsUseCases: PermissionsUseCases,
    @ApplicationScope private val applicationScope: CoroutineScope
) : AndroidViewModel(application) {

    private var _clock = MutableStateFlow(false)
    private var _timer: Timer? = null

    /** ---------- Proxies for Flows from UseCases, turned into StateFlows  -------------------- */

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

    private val sessionState = activeSessionUseCases.getSessionStatus().map { state ->
        when (state) {
            SessionStatus.NOT_STARTED -> ActiveSessionState.NOT_STARTED
            SessionStatus.RUNNING -> ActiveSessionState.RUNNING
            SessionStatus.PAUSED -> ActiveSessionState.PAUSED
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

    /** ------------------- Own StateFlow UI state ------------------------------------------- */

    private val _endDialogComment = MutableStateFlow("")
    private val _endDialogRating = MutableStateFlow(3)
    private val _endDialogVisible = MutableStateFlow(false)
    private val _discardDialogVisible = MutableStateFlow(false)
    private val _newItemSelectorVisible = MutableStateFlow(false)
    private val _exceptionChannel = Channel<ActiveSessionException>()
    val exceptionChannel = _exceptionChannel.receiveAsFlow()

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
            timerText = getFormattedTimerText(practiceDuration),
            subHeadingText = if (pause) "Paused $pauseDurStr" else "Practice Time",
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActiveSessionTimerUiState(
            timerText = getFormattedTimerText(Duration.ZERO),
            subHeadingText = "Practice Time"
        )
    )

    private fun getFormattedTimerText(duration: Duration) = getDurationString(
        duration,
        DurationFormat.MS_DIGITAL
    ).toString()

    private val currentItemUiState: StateFlow<ActiveSessionCurrentItemUiState?> = combine(
        sessionState,
        runningLibraryItem,
        _clock  // should update with clock
    ) { sessionState, item, _ ->
        if (sessionState == ActiveSessionState.NOT_STARTED) return@combine null

        val currentItemDuration = try {
            activeSessionUseCases.getRunningItemDuration()
        } catch (e: IllegalStateException) {
            Duration.ZERO   // Session not yet started
        }
        ActiveSessionCurrentItemUiState(
            name = item?.name ?: "Not started",
            durationText = getDurationString(
                currentItemDuration,
                DurationFormat.MS_DIGITAL
            ).toString(),
            color = libraryItemColors[item?.colorIndex ?: 0]
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val pastSectionsUiState = completedSections.map { sections ->
        if (sections.isEmpty()) {
            return@map null
        }
        ActiveSessionCompletedSectionsUiState(
            items = sections.reversed().map {
                CompletedSectionUiState(
                    id = it.id,
                    name = it.libraryItem.name,
                    color = libraryItemColors[it.libraryItem.colorIndex],
                    durationText = getDurationString(
                        it.duration,
                        DurationFormat.MS_DIGITAL
                    ).toString()
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )


    private val newItemSelectorUiState = combine(
        _newItemSelectorVisible,
        runningLibraryItem,
        libraryFoldersWithItems,
        lastPracticedDates,
        rootItems,
    ) { visible, runningItem, folders, lastPracticedDates, rootItems ->
        if (!visible) return@combine null
        NewItemSelectorUiState(
            runningItem = runningItem,
            foldersWithItems = folders,
            lastPracticedDates = lastPracticedDates,
            rootItems = rootItems
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _endDialogUiState = combine(
        _endDialogVisible,
        _endDialogComment,
        _endDialogRating
    ) { visible, comment, rating ->
        if (!visible) return@combine null
        ActiveSessionEndDialogUiState(
            comment = comment,
            rating = rating
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val dialogsUiStates = combine(
        _endDialogUiState,
        _discardDialogVisible
    ) { endDialog, discardDialogVisible ->
        ActiveSessionDialogsUiState(
            endDialogUiState = endDialog,
            discardDialogVisible = discardDialogVisible
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActiveSessionDialogsUiState(
            endDialogUiState = _endDialogUiState.value,
            discardDialogVisible = _discardDialogVisible.value
        )
    )

    val uiState = MutableStateFlow(
        ActiveSessionUiState(
            sessionState = sessionState,
            mainContentUiState = MutableStateFlow(
                ActiveSessionContentUiState(
                    timerUiState = timerUiState,
                    currentItemUiState = currentItemUiState,
                    pastSectionsUiState = pastSectionsUiState,
                )
            ),
            newItemSelectorUiState = newItemSelectorUiState,
            dialogUiState = dialogsUiStates
        )
    ).asStateFlow()

    private val navigationChannel = Channel<NavigationEvent>()
    val navigationEventsChannelFlow = navigationChannel.receiveAsFlow()


    init {
        startTimer()
        /** Hide the Tools Bottom Sheet on Startup */
        runBlocking(context = Dispatchers.IO) {
            viewModelScope.launch {
                // wait until session data has initialized
                while (sessionState.value == ActiveSessionState.UNKNOWN) {
                    delay(100)
                }
                // TODO: hide tools tab but only on session start and when it was originally hidden
                //  also take into account whether recording is in progress or metronome is running
                //  and open the respective tab in case
//                viewModelScope.launch {
//                    navigationChannel.send(NavigationEvent.HideTools)
//                }

            }
        }
    }

    fun onUiEvent(event: ActiveSessionUiEvent) {
        when (event) {
            is ActiveSessionUiEvent.SelectItem -> viewModelScope.launch { selectItem(event.item) }
            is ActiveSessionUiEvent.TogglePauseState -> viewModelScope.launch { togglePauseState() }
            is ActiveSessionUiEvent.DeleteSection -> viewModelScope.launch { deleteSection(event.sectionId) }
            is ActiveSessionUiEvent.EndDialogUiEvent -> onEndDialogUiEvent(event.dialogEvent)
            is ActiveSessionUiEvent.BackPressed -> { /* TODO */ }
            ActiveSessionUiEvent.DiscardSessionDialogConfirmed -> discardSession()
            ActiveSessionUiEvent.ToggleDiscardDialog -> _discardDialogVisible.update { !it }
            ActiveSessionUiEvent.ToggleFinishDialog -> _endDialogVisible.update { !it }
            ActiveSessionUiEvent.ToggleNewItemSelector -> viewModelScope.launch {
                if (!hasNotificationPermission()) {
                    _exceptionChannel.send(ActiveSessionException.NoNotificationPermission)
                    return@launch
                }
                _newItemSelectorVisible.update { !it }
            }
        }
    }

    private fun onEndDialogUiEvent(event: ActiveSessionEndDialogUiEvent) {
        when (event) {
            is ActiveSessionEndDialogUiEvent.CommentChanged -> _endDialogComment.update { event.comment }
            is ActiveSessionEndDialogUiEvent.RatingChanged -> _endDialogRating.update { event.rating }
            ActiveSessionEndDialogUiEvent.Confirmed -> applicationScope.launch {
                // launch stopSession in applicationScope because it is a long running operation and
                // may outlive the ViewModel because it gets destroyed.
                stopSession()
            }
        }
    }

    private suspend fun hasNotificationPermission(): Boolean {
        // Only check for POST_NOTIFICATIONS permission on Android 12 and above
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        val result = permissionsUseCases.request(
            listOf(android.Manifest.permission.POST_NOTIFICATIONS)
        ).exceptionOrNull()
        return result !is PermissionChecker.PermissionsDeniedException
    }

    private suspend fun selectItem(item: LibraryItem) {
        // resume session if paused
        if (sessionState.value == ActiveSessionState.PAUSED) {
            activeSessionUseCases.resume()
        }
        // wait until the current item has been running for at least 1 second
        if (sessionState.value != ActiveSessionState.NOT_STARTED
            && activeSessionUseCases.getRunningItemDuration() < 1.seconds
        ) {
            delay(1000)
        }
        activeSessionUseCases.selectItem(item)
    }

    private suspend fun deleteSection(sectionId: UUID) {
        if (sessionState.value == ActiveSessionState.PAUSED) {
            activeSessionUseCases.resume()
        }
        activeSessionUseCases.deleteSection(sectionId)
    }

    private fun discardSession() {
        activeSessionUseCases.reset()
        viewModelScope.launch {
            navigationChannel.send(NavigationEvent.NavigateUp)
        }
    }

    private suspend fun togglePauseState() {
        when (sessionState.value) {
            ActiveSessionState.RUNNING -> activeSessionUseCases.pause()
            ActiveSessionState.PAUSED -> activeSessionUseCases.resume()
            else -> {}
        }
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

    private suspend fun stopSession() {
        // complete running section
        val savableState = activeSessionUseCases.getFinalizedSession()
        // ignore empty sections (e.g. when paused and then stopped immediately))
        val sections = savableState.completedSections.filter { it.duration > 0.seconds }
        // store in database
        sessionUseCases.add(
            sessionCreationAttributes = SessionCreationAttributes(
                // add up all pause durations
                breakDuration = sections.fold(0.seconds) { acc, section ->
                    acc + section.pauseDuration
                },
                comment = _endDialogComment.value,
                rating = _endDialogRating.value
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

sealed interface NavigationEvent {
    object NavigateUp : NavigationEvent
    object HideTools : NavigationEvent
}