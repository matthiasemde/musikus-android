/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger, Matthias Emde
 *
 */

package app.musikus.ui.activesession

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.database.Nullable
import app.musikus.database.UUIDConverter
import app.musikus.database.daos.LibraryItem
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.database.entities.SectionCreationAttributes
import app.musikus.database.entities.SessionCreationAttributes
import app.musikus.di.ApplicationScope
import app.musikus.services.SessionService
import app.musikus.services.SessionServiceEvent
import app.musikus.ui.library.LibraryItemDialogUiEvent
import app.musikus.ui.library.LibraryItemEditData
import app.musikus.usecase.activesession.ActiveSessionUseCases
import app.musikus.usecase.activesession.PracticeSection
import app.musikus.usecase.library.LibraryUseCases
import app.musikus.usecase.permissions.PermissionsUseCases
import app.musikus.usecase.sessions.SessionsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.util.Timer
import java.util.UUID
import javax.inject.Inject
import kotlin.concurrent.timer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class EndDialogData(
    val rating: Int,
    val comment: String
)

data class SessionViewModelState(
    val startTimestamp: ZonedDateTime,  // only needed for storing the session in the database
    val sessionDuration: Duration,
    val sections: List<PracticeSection>,
    val activeSection: Pair<LibraryItem, Duration>,
    val ongoingPauseDuration: Duration,
    val isPaused: Boolean
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ActiveSessionViewModel @Inject constructor(
    private val libraryUseCases: LibraryUseCases,
    private val sessionUseCases: SessionsUseCases,
    private val permissionsUseCases: PermissionsUseCases,
    private val activeSessionUseCases: ActiveSessionUseCases,
    private val application: Application,
    @ApplicationScope private val applicationScope: CoroutineScope
) : AndroidViewModel(application) {

    /**
     *  --------------------- Private properties ---------------------
     */

    private val _recorderUiState = ActiveSessionDraggableCardUiState.RecorderCardUiState(
        title = "Recorder",
        isExpandable = true,
        hasFab = false,
        headerUiState = ActiveSessionDraggableCardHeaderUiState.RecorderCardHeaderUiState,
        bodyUiState = ActiveSessionDraggableCardBodyUiState.RecorderCardBodyUiState
    )

    private val _metronomeUiState = ActiveSessionDraggableCardUiState.MetronomeCardUiState(
        title = "Metronome",
        isExpandable = false,
        hasFab = false,
        headerUiState = ActiveSessionDraggableCardHeaderUiState.MetronomeCardHeaderUiState,
        bodyUiState = ActiveSessionDraggableCardBodyUiState.MetronomeCardBodyUiState
    )

    /** stateFlows */

    private val _notificationPermissionsGranted = MutableStateFlow(false)
    private val _selectedFolderId = MutableStateFlow<UUID?>(null)
    private val _addLibraryItemData = MutableStateFlow<LibraryItemEditData?>(null)
    private val _endDialogData = MutableStateFlow<EndDialogData?>(null)
    private val _showDiscardSessionDialog = MutableStateFlow(false)
    private var _clock = MutableStateFlow(false)

    /** Service */

    private var sessionServiceEventHandler: ((SessionServiceEvent) -> Unit)? = null
    private var bound = false


    /** Variables */

    private var _timer: Timer? = null
    private val connection = object : ServiceConnection {
        /** called by service when we have connection to the service => we have mService reference */
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to SessionForegroundService, cast the Binder and get SessionService instance
            val binder = service as SessionService.LocalBinder
            sessionServiceEventHandler = binder.getOnEvent()
            bound = true
            startTimer()
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
        }
    }

    init {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            viewModelScope.launch {
                val notificationPermissionResult = permissionsUseCases.request(
                    listOf(android.Manifest.permission.POST_NOTIFICATIONS)
                )
                if (notificationPermissionResult.isSuccess) {
                    _notificationPermissionsGranted.update { true }
                } else {
                    // TODO _exceptionChannel.send(ActiveSessionException.NoNotificationPermission)
                }
            }
        }

        // try to bind to SessionService
        bindService()
    }

    // ####################### Library #######################

    private val itemsInSelectedFolder = _selectedFolderId.flatMapLatest { selectedFolderId ->
        libraryUseCases.getSortedItems(Nullable(selectedFolderId))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf()
    )

    private val foldersWithItems = libraryUseCases.getSortedFolders().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf()
    )

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


    /**
     *  ---------------- Composing the Ui state --------------------
     */


    private val sessionState = combine(
        completedSections,
        runningLibraryItem,
        _clock
    ) { completedSections, runningItem, _ ->
        if (runningItem == null) return@combine null    // no active session try-catch does the same
        try {
            SessionViewModelState(
                sessionDuration = activeSessionUseCases.getPracticeDuration(),
                sections = completedSections,
                activeSection = Pair(runningItem, activeSessionUseCases.getRunningItemDuration()),
                ongoingPauseDuration = activeSessionUseCases.getOngoingPauseDuration(),
                isPaused = activeSessionUseCases.getPausedState(),
                startTimestamp = activeSessionUseCases.getStartTime()
            )
        } catch (e: IllegalStateException) {
            null
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val libraryCardHeaderUiState = combine(
        foldersWithItems,
        _selectedFolderId,
        runningLibraryItem
    ) { foldersWithItems, selectedFolderId, runningLibraryItem ->
        ActiveSessionDraggableCardHeaderUiState.LibraryCardHeaderUiState(
            folders = listOf(null) + foldersWithItems.map { it.folder },
            selectedFolderId = selectedFolderId,
            activeFolderId = runningLibraryItem?.let { Nullable(it.libraryFolderId) }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActiveSessionDraggableCardHeaderUiState.LibraryCardHeaderUiState(
            folders = emptyList(),
            selectedFolderId = null,
            activeFolderId = null
        )
    )

    private val libraryCardBodyUiState = combine(
        itemsInSelectedFolder,
        runningLibraryItem
    ) { items, runningLibraryItem ->
        ActiveSessionDraggableCardBodyUiState.LibraryCardBodyUiState(
            items = items,
            activeItemId = runningLibraryItem?.id
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActiveSessionDraggableCardBodyUiState.LibraryCardBodyUiState(
            items = itemsInSelectedFolder.value,
            activeItemId = null
        )
    )

    private val libraryCardUiState = combine(
        libraryCardHeaderUiState,
        libraryCardBodyUiState,
    ) { headUiState, bodyUiState ->
        ActiveSessionDraggableCardUiState.LibraryCardUiState(
            title = "Library",
            isExpandable = true,
            hasFab = true,
            headerUiState = headUiState,
            bodyUiState = bodyUiState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActiveSessionDraggableCardUiState.LibraryCardUiState(
            title = "Library",
            isExpandable = true,
            hasFab = true,
            headerUiState = libraryCardHeaderUiState.value,
            bodyUiState = libraryCardBodyUiState.value
        )
    )

    private val addLibraryItemDialogUiState = combine(
        foldersWithItems,
        _addLibraryItemData
    ) { foldersWithItems, itemData ->
        if(itemData == null) return@combine null

        ActiveSessionAddLibraryItemDialogUiState(
            folders = foldersWithItems.map { it.folder },
            itemData = itemData,
            isConfirmButtonEnabled = itemData.name.isNotBlank()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val endSessionDialogUiState = _endDialogData.map { endDialogData ->
        if(endDialogData == null) return@map null
        ActiveSessionEndDialogUiState(
            rating = endDialogData.rating,
            comment = endDialogData.comment
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val dialogUiState = combine(
        _showDiscardSessionDialog,
        endSessionDialogUiState
    ) { showDiscardSessionDialog, endDialogUiState ->
        ActiveSessionDialogUiState(
            showDiscardSessionDialog = showDiscardSessionDialog,
            endDialogUiState = endDialogUiState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActiveSessionDialogUiState(
            showDiscardSessionDialog = _showDiscardSessionDialog.value,
            endDialogUiState = endSessionDialogUiState.value
        )
    )


    /** ############  The final public UI state ############ */

    val uiState = combine(
        libraryCardUiState,
        sessionState,
        addLibraryItemDialogUiState,
        dialogUiState,
    ) { libraryCardUiState, sessionState, addItemDialogUiState, dialogUiState ->
        ActiveSessionUiState(
            cardUiStates = listOf(
                libraryCardUiState,
                _recorderUiState,
                _metronomeUiState
            ),
            totalSessionDuration = sessionState?.sessionDuration ?: 0.seconds,
            ongoingPauseDuration = sessionState?.ongoingPauseDuration ?: 0.seconds,
            isPaused = sessionState?.isPaused ?: false,
            addItemDialogUiState = addItemDialogUiState,
            sections = sessionState?.sections?.reversed()?.map { section ->
                    ActiveSessionSectionListItemUiState(
                    id = section.id,
                    libraryItem = section.libraryItem,
                    duration = section.duration
                )
            } ?: emptyList(),
            runningSection = sessionState?.let {
                ActiveSessionSectionListItemUiState(
                    id = UUIDConverter.deadBeef, // doesn't matter, not used
                    libraryItem = sessionState.activeSection.first,
                    duration = sessionState.activeSection.second
                )
            },
            dialogUiState = dialogUiState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActiveSessionUiState(
            cardUiStates = emptyList(),
            totalSessionDuration = 0.milliseconds,
            ongoingPauseDuration = 0.seconds,
            sections = emptyList(),
            isPaused = false,
            runningSection = null,
            addItemDialogUiState = addLibraryItemDialogUiState.value,
            dialogUiState = dialogUiState.value
        )
    )


    /** UI Event handler */

    fun onUiEvent(event: ActiveSessionUiEvent) {
        when (event) {
            is ActiveSessionUiEvent.SelectFolder -> _selectedFolderId.update { event.folderId }
            is ActiveSessionUiEvent.SelectItem -> itemClicked(event.item)
            is ActiveSessionUiEvent.TogglePause -> togglePause()
            is ActiveSessionUiEvent.ShowFinishDialog -> _endDialogData.update { EndDialogData(
                rating = 3,
                comment = ""
            ) }
            is ActiveSessionUiEvent.DeleteSection -> removeSection(event.sectionId)
            is ActiveSessionUiEvent.CreateNewLibraryItem -> createLibraryItemDialog()
            is ActiveSessionUiEvent.ItemDialogUiEvent -> {
                when(val dialogEvent = event.dialogEvent) {
                    is LibraryItemDialogUiEvent.NameChanged ->
                        _addLibraryItemData.update { it?.copy(name = dialogEvent.name) }
                    is LibraryItemDialogUiEvent.ColorIndexChanged ->
                        _addLibraryItemData.update { it?.copy(colorIndex = dialogEvent.colorIndex) }
                    is LibraryItemDialogUiEvent.FolderIdChanged ->
                        _addLibraryItemData.update { it?.copy(folderId = dialogEvent.folderId) }
                    is LibraryItemDialogUiEvent.Confirmed ->
                        viewModelScope.launch {
                            _addLibraryItemData.value?.let { itemData ->
                                libraryUseCases.addItem(
                                    LibraryItemCreationAttributes(
                                        name = itemData.name,
                                        colorIndex = itemData.colorIndex,
                                        libraryFolderId = Nullable(itemData.folderId)
                                    )
                                )
                                _selectedFolderId.update { itemData.folderId }
                            }
                            _addLibraryItemData.update { null }
                        }
                    is LibraryItemDialogUiEvent.Dismissed -> _addLibraryItemData.update { null }
                }
            }
            is ActiveSessionUiEvent.EndDialogRatingChanged -> _endDialogData.update {
                it?.copy(rating = event.rating)
            }
            is ActiveSessionUiEvent.EndDialogCommentChanged -> _endDialogData.update {
                it?.copy(comment = event.comment)
            }
            is ActiveSessionUiEvent.EndDialogDismissed -> _endDialogData.update { null }
            is ActiveSessionUiEvent.EndDialogConfirmed -> stopSession()
            is ActiveSessionUiEvent.ShowDiscardSessionDialog -> _showDiscardSessionDialog.update { true }
            is ActiveSessionUiEvent.DiscardSessionDialogConfirmed -> {
                discardSession()
                _showDiscardSessionDialog.update { false }
            }
            is ActiveSessionUiEvent.DiscardSessionDialogDismissed -> _showDiscardSessionDialog.update { false }
            is ActiveSessionUiEvent.BackPressed -> {
                _timer?.cancel()
                _timer = null
            }
        }
    }



    /** ---------------------------------- private methods ----------------------------------- */

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

    private fun itemClicked(item: LibraryItem) {
        if (!bound) return
        sessionServiceEventHandler?.invoke(SessionServiceEvent.StartNewSection(item))
        startService()
    }

    private fun togglePause() {
        sessionServiceEventHandler?.invoke(SessionServiceEvent.TogglePause)
    }

    private fun stopSession() {
        val endDialogData = _endDialogData.value ?: return
        applicationScope.launch {
            val savableState = activeSessionUseCases.getFinalizedSession()   // complete running section

            // ignore empty sections (e.g. when paused and then stopped immediately))
            val sections = savableState.completedSections.filter { it.duration > 0.seconds }

            // store in database
            sessionUseCases.add(
                sessionCreationAttributes = SessionCreationAttributes(
                    breakDuration = sections.fold(0.seconds) { acc, section ->
                        acc + section.pauseDuration
                    },
                    comment = endDialogData.comment,
                    rating = endDialogData.rating
                ),
                sectionCreationAttributes = sections.map { section ->
                    SectionCreationAttributes(
                        libraryItemId = section.libraryItem.id,
                        duration = section.duration,
                        startTimestamp = savableState.startTimestamp
                    )
                }
            )
            activeSessionUseCases.reset()   // reset the active session state
            _endDialogData.update { null }
            sessionServiceEventHandler?.invoke(SessionServiceEvent.StopService)
            unbindService()
        }
    }

    private fun discardSession() {
        activeSessionUseCases.reset()
        sessionServiceEventHandler?.invoke(SessionServiceEvent.StopService)
    }

    private fun removeSection(itemId: UUID) {
        sessionServiceEventHandler?.invoke(SessionServiceEvent.DeleteSection(itemId))
    }

    private fun createLibraryItemDialog() {
        _addLibraryItemData.update {
            LibraryItemEditData(
                name = "",
                colorIndex = (Math.random() * 10).toInt(),
                folderId = _selectedFolderId.value
            )
        }
    }

    // ################################### Service ############################################

    private fun startService() {
        val intent = Intent(application, SessionService::class.java) // Build the intent for the service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            application.startForegroundService(intent)
        } else {
            application.startService(intent)
        }
    }

    private fun bindService() {
        val intent = Intent(application, SessionService::class.java) // Build the intent for the service
        application.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onCleared() {
        unbindService()
        _timer?.cancel()
        super.onCleared()
    }

    private fun unbindService() {
        if (bound) {
            application.unbindService(connection)
            bound = false
        }
    }
}