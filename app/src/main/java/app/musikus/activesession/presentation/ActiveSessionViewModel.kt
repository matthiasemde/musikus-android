/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger, Matthias Emde
 */

package app.musikus.activesession.presentation

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.R
import app.musikus.activesession.domain.usecase.ActiveSessionUseCases
import app.musikus.activesession.domain.usecase.SessionStatus
import app.musikus.core.data.Nullable
import app.musikus.core.di.ApplicationScope
import app.musikus.core.domain.SortDirection
import app.musikus.core.domain.SortInfo
import app.musikus.core.domain.TimeProvider
import app.musikus.core.presentation.theme.libraryItemColors
import app.musikus.core.presentation.utils.DurationFormat
import app.musikus.core.presentation.utils.UiText
import app.musikus.core.presentation.utils.getDurationString
import app.musikus.library.data.LibraryFolderSortMode
import app.musikus.library.data.LibraryItemSortMode
import app.musikus.library.data.daos.LibraryItem
import app.musikus.library.domain.usecase.LibraryUseCases
import app.musikus.library.presentation.LibraryCoreUiEvent
import app.musikus.library.presentation.LibraryFoldersSortMenuUiState
import app.musikus.library.presentation.LibraryFoldersUiState
import app.musikus.library.presentation.LibraryItemsSortMenuUiState
import app.musikus.library.presentation.LibraryItemsUiState
import app.musikus.library.presentation.LibraryUiEvent
import app.musikus.permissions.domain.PermissionChecker
import app.musikus.permissions.domain.usecase.PermissionsUseCases
import app.musikus.sessions.data.entities.SectionCreationAttributes
import app.musikus.sessions.data.entities.SessionCreationAttributes
import app.musikus.sessions.domain.usecase.SessionsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
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
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class ActiveSessionViewModel @Inject constructor(
    application: Application,
    private val libraryUseCases: LibraryUseCases,
    private val activeSessionUseCases: ActiveSessionUseCases,
    private val sessionUseCases: SessionsUseCases,
    private val permissionsUseCases: PermissionsUseCases,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val timeProvider: TimeProvider
) : AndroidViewModel(application) {

    /** ---------- Proxies for Flows from UseCases, turned into StateFlows  -------------------- */

    private val state = activeSessionUseCases.getState().stateIn(
        scope = viewModelScope,
        started = Eagerly,
        initialValue = null
    )

    private val completedSections = activeSessionUseCases.getCompletedSections().stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val runningLibraryItem = activeSessionUseCases.getRunningItem().stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
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
        started = WhileSubscribed(5000),
        initialValue = ActiveSessionState.UNKNOWN
    )

    private val libraryFolders = libraryUseCases.getSortedFolders().stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val itemSortInfo = libraryUseCases.getItemSortInfo().stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = SortInfo(
            mode = LibraryItemSortMode.DEFAULT,
            direction = SortDirection.DEFAULT,
        )
    )

    private val folderSortInfo = libraryUseCases.getFolderSortInfo().stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = SortInfo(
            mode = LibraryFolderSortMode.DEFAULT,
            direction = SortDirection.DEFAULT,
        )
    )

    /** ------------------- Own StateFlow UI state ------------------------------------------- */

    private val _endDialogComment = MutableStateFlow("")
    private val _endDialogRating = MutableStateFlow(3)
    private val _endDialogVisible = MutableStateFlow(false)
    private val _discardDialogVisible = MutableStateFlow(false)
    private val _newItemSelectorVisible = MutableStateFlow(false)
    private val _displayedFolder = MutableStateFlow<UUID?>(null)

    private val _exceptionChannel = Channel<ActiveSessionException>()
    val exceptionChannel = _exceptionChannel.receiveAsFlow()

    private val _eventChannel = Channel<ActiveSessionEvent>()
    val eventChannel = _eventChannel.receiveAsFlow()

    /** ------------------- Combined flows ---------------------------- */

    private val totalPracticeDuration = timeProvider.clock.map { now ->
        // we intentionally do not collect events from the flow here in order to avoid race conditions
        state.value?.let {
            activeSessionUseCases.computeTotalPracticeDuration(it, now)
        } ?: Duration.ZERO
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = Duration.ZERO
    )

    /** Items from selected folder */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val items = _displayedFolder.flatMapLatest { folderId ->
        libraryUseCases.getSortedItems(Nullable(folderId))
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /** Last practiced dates for items in the selected folder */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val lastPracticedDates = items.flatMapLatest { items ->
        libraryUseCases.getLastPracticedDate(items)
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    private val ongoingPauseDuration = timeProvider.clock.map { now ->
        // we intentionally do not collect events from the flow here in order to avoid race conditions
        state.value?.let {
            activeSessionUseCases.computeOngoingPauseDuration(it, now)
        } ?: Duration.ZERO
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = Duration.ZERO
    )

    private val runningItemDuration = timeProvider.clock.map { now ->
        // we intentionally do not collect events from the flow here in order to avoid race conditions
        state.value?.let {
            activeSessionUseCases.computeRunningItemDuration(it, now)
        } ?: Duration.ZERO
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = Duration.ZERO
    )

    val isFinishButtonEnabled = totalPracticeDuration.map {
        it >= 1.seconds
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = false
    )

    /** ------------------- Sub UI states  ------------------------------------------- */

    private val timerUiState = combine(
        sessionState,
        totalPracticeDuration,
        ongoingPauseDuration,
    ) { sessionState, totalPracticeDuration, ongoingPauseDuration ->
        val pause = sessionState == ActiveSessionState.PAUSED

        val pauseDurStr = getDurationString(
            ongoingPauseDuration,
            DurationFormat.MS_DIGITAL
        )
        ActiveSessionTimerUiState(
            timerText = getFormattedTimerText(totalPracticeDuration),
            subHeadingText =
            if (pause) {
                UiText.StringResource(R.string.active_session_timer_subheading_paused, pauseDurStr)
            } else {
                UiText.StringResource(R.string.active_session_timer_subheading)
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = ActiveSessionTimerUiState(
            timerText = getFormattedTimerText(Duration.ZERO),
            subHeadingText = UiText.StringResource(R.string.active_session_timer_subheading)
        )
    )

    private fun getFormattedTimerText(duration: Duration) = getDurationString(
        duration,
        DurationFormat.MS_DIGITAL
    ).toString()

    private val currentItemUiState: StateFlow<ActiveSessionCurrentItemUiState?> = combine(
        sessionState,
        runningLibraryItem,
        runningItemDuration
    ) { sessionState, item, runningItemDuration ->
        if (sessionState == ActiveSessionState.NOT_STARTED || item == null) return@combine null

        ActiveSessionCurrentItemUiState(
            name = item.name,
            durationText = getDurationString(
                runningItemDuration,
                DurationFormat.MS_DIGITAL
            ).toString(),
            color = libraryItemColors[item.colorIndex]
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
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
        started = WhileSubscribed(5000),
        initialValue = null
    )

    private val libraryItemsUiState = combine(
        itemSortInfo,
        items,
        lastPracticedDates,
    ) { sortInfo, items, dates ->
        LibraryItemsUiState(
            itemsWithLastPracticedDate = items.map { item ->
                item to dates[item.id]
            },
            selectedItemIds = emptySet(),
            sortMenuUiState = LibraryItemsSortMenuUiState(
                mode = sortInfo.mode,
                direction = sortInfo.direction,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = LibraryItemsUiState(
            itemsWithLastPracticedDate = emptyList(),
            selectedItemIds = emptySet(),
            sortMenuUiState = LibraryItemsSortMenuUiState(
                mode = LibraryItemSortMode.DEFAULT,
                direction = SortDirection.DEFAULT,
            ),
        )
    )

    private val libraryFoldersUiState = combine(
        folderSortInfo,
        libraryFolders,
    ) { sortInfo, folders ->
        LibraryFoldersUiState(
            foldersWithItems = folders,
            selectedFolderIds = emptySet(),
            sortMenuUiState = LibraryFoldersSortMenuUiState(
                mode = sortInfo.mode,
                direction = sortInfo.direction,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = LibraryFoldersUiState(
            foldersWithItems = emptyList(),
            selectedFolderIds = emptySet(),
            sortMenuUiState = LibraryFoldersSortMenuUiState(
                mode = LibraryFolderSortMode.DEFAULT,
                direction = SortDirection.DEFAULT,
            ),
        )
    )

    private val newItemSelectorUiState = combine(
        _newItemSelectorVisible,
        runningLibraryItem,
        libraryItemsUiState,
        libraryFoldersUiState,
    ) { visible, runningItem, libraryItemsUiState, libraryFoldersUiState ->
        if (!visible) return@combine null

        NewItemSelectorUiState(
            runningItem = runningItem,
            libraryItemsUiState = libraryItemsUiState,
            libraryFoldersUiState = libraryFoldersUiState
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
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
        started = WhileSubscribed(5000),
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
        started = WhileSubscribed(5000),
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
            dialogUiState = dialogsUiStates,
            isFinishButtonEnabled = isFinishButtonEnabled
        )
    ).asStateFlow()

    init {
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
//                    _eventChannel.send(ActiveSessionEvent.HideTools)
//                }
            }
        }
    }

    /**
     * Handles UI events from the Active Session screen.
     * Returns true if the event was consumed, false otherwise.
     */
    fun onUiEvent(event: ActiveSessionUiEvent): Boolean {
        when (event) {
            is ActiveSessionUiEvent.SelectItem -> viewModelScope.launch { selectItem(event.item) }
            is ActiveSessionUiEvent.TogglePauseState -> viewModelScope.launch { togglePauseState() }
            is ActiveSessionUiEvent.DeleteSection -> viewModelScope.launch { deleteSection(event.sectionId) }
            is ActiveSessionUiEvent.EndDialogUiEvent -> onEndDialogUiEvent(event.dialogEvent)
            is ActiveSessionUiEvent.BackPressed -> { /* TODO */ }
            ActiveSessionUiEvent.DiscardSessionDialogConfirmed -> activeSessionUseCases.reset()
            ActiveSessionUiEvent.ToggleDiscardDialog -> _discardDialogVisible.update { !it }
            ActiveSessionUiEvent.ToggleFinishDialog -> _endDialogVisible.update { !it }
            ActiveSessionUiEvent.ToggleNewItemSelector -> viewModelScope.launch {
                if (!hasNotificationPermission()) {
                    _exceptionChannel.send(ActiveSessionException.NoNotificationPermission)
                    return@launch
                }
                _newItemSelectorVisible.update { !it }
            }
            is ActiveSessionUiEvent.NewItemSelectorEvent -> {
                // redirect events to NewItemSelector event handler
                return onNewItemSelectorEvent(event.libraryEvent)
            }
        }

        // events are consumed by default
        return true
    }

    /**
     * Handles events from the New Item Selector Bottom Sheet.
     * Returns true if the event was consumed, false otherwise.
     */
    private fun onNewItemSelectorEvent(event: LibraryUiEvent): Boolean {
        when (event) {
            is LibraryUiEvent.CoreUiEvent -> {
                // handle core events like navigation
                when (event.coreEvent) {
                    is LibraryCoreUiEvent.ItemPressed -> viewModelScope.launch {
                        if (event.coreEvent.longClick) return@launch // ignore long clicks
                        selectItem(event.coreEvent.item)
                    }
                    is LibraryCoreUiEvent.ItemSortModeSelected -> viewModelScope.launch {
                        libraryUseCases.selectItemSortMode(event.coreEvent.mode)
                    }
                    else -> return false // event not handled
                }
            }
            is LibraryUiEvent.FolderPressed -> {
                // select folder in new item selector
                _displayedFolder.update { event.folderId }
            }
            is LibraryUiEvent.FolderSortModeSelected -> {
                viewModelScope.launch {
                    libraryUseCases.selectFolderSortMode(event.mode)
                }
            }
            else -> return false // event not handled
        }
        return true
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
            activeSessionUseCases.resume(timeProvider.now())
        }

        activeSessionUseCases.selectItem(
            item = item,
            at = timeProvider.now()
        )
    }

    private suspend fun deleteSection(sectionId: UUID) {
        activeSessionUseCases.deleteSection(sectionId)
    }

    private suspend fun togglePauseState() {
        when (sessionState.value) {
            ActiveSessionState.RUNNING -> activeSessionUseCases.pause(timeProvider.now())
            ActiveSessionState.PAUSED -> activeSessionUseCases.resume(timeProvider.now())
            else -> {}
        }
    }

    private suspend fun stopSession() {
        // TODO this logic should be moved to the use case
        // complete running section
        val savableState = activeSessionUseCases.getFinalizedSession(timeProvider.now())
        // store in database
        sessionUseCases.add(
            sessionCreationAttributes = SessionCreationAttributes(
                // add up all pause durations
                breakDuration = savableState.completedSections.fold(0.seconds) { acc, section ->
                    acc + section.pauseDuration
                },
                comment = _endDialogComment.value,
                rating = _endDialogRating.value
            ),
            sectionCreationAttributes = savableState.completedSections.map { section ->
                SectionCreationAttributes(
                    libraryItemId = section.libraryItem.id,
                    duration = section.duration,
                    startTimestamp = section.startTimestamp
                )
            }
        )
        activeSessionUseCases.reset() // reset the active session state
    }
}

sealed interface ActiveSessionEvent {
    object HideTools : ActiveSessionEvent
}
