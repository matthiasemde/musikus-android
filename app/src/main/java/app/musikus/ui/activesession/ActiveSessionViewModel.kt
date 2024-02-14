package app.musikus.ui.activesession

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.database.Nullable
import app.musikus.database.daos.LibraryItem
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.database.entities.SectionCreationAttributes
import app.musikus.database.entities.SessionCreationAttributes
import app.musikus.services.SessionEvent
import app.musikus.services.SessionService
import app.musikus.services.SessionServiceAction
import app.musikus.services.SessionState
import app.musikus.ui.library.LibraryItemDialogUiEvent
import app.musikus.ui.library.LibraryItemEditData
import app.musikus.usecase.library.LibraryUseCases
import app.musikus.usecase.sessions.SessionsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

const val TAG = "ActiveSessionViewModel"

data class PracticeSection(
    val id: Int,
    val libraryItem: LibraryItem,
    val startTimestamp: ZonedDateTime,
    var duration: Duration?
)

data class EndDialogData(
    val rating: Int,
    val comment: String
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ActiveSessionViewModel @Inject constructor(
    private val libraryUseCases: LibraryUseCases,
    private val sessionUseCases: SessionsUseCases,
    private val application: Application
): AndroidViewModel(application) {

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

    private val _selectedFolderId = MutableStateFlow<UUID?>(null)
    private val _addLibraryItemData = MutableStateFlow<LibraryItemEditData?>(null)
    private val _endDialogData = MutableStateFlow<EndDialogData?>(null)

    /**
     *  --------------------- Session service  ---------------------
     */

    private val sessionStateWrapper = MutableStateFlow<StateFlow<SessionState>?>(null)
    private val sessionState = sessionStateWrapper.flatMapLatest {
        it ?: flowOf(SessionState())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SessionState()
    )

    // #############################################    

    private var sessionEvent: (SessionEvent) -> Unit = { Log.d("TAG", "not implemented") }
    private var bound = false

    private val connection = object : ServiceConnection {
        /** called by service when we have connection to the service => we have mService reference */
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to SessionForegroundService, cast the Binder and get SessionService instance
            val binder = service as SessionService.LocalBinder
            sessionEvent = binder.getOnEvent()
            sessionStateWrapper.update { binder.getSessionStateFlow() }

            bound = true
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
        }
    }

    init {
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

    private val currentlyPracticedSection = sessionState.map {
        it.sections.lastOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )


    private val totalDurationRoundedDownToNextSecond = sessionState.map { sessionState ->
        sessionState.sections.sumOf {
            (it.duration ?: sessionState.currentSectionDuration).inWholeMilliseconds
        }.milliseconds.inWholeSeconds.seconds
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.seconds
    )

    /**
     *  ---------------- Composing the Ui state --------------------
     */

    private val libraryCardHeaderUiState = combine(
        foldersWithItems,
        _selectedFolderId,
        currentlyPracticedSection
    ) { foldersWithItems, selectedFolderId, currentlyPracticedSection ->
        ActiveSessionDraggableCardHeaderUiState.LibraryCardHeaderUiState(
            folders = listOf(null) + foldersWithItems.map { it.folder },
            selectedFolderId = selectedFolderId,
            activeFolderId = currentlyPracticedSection?.let {
                Nullable(it.libraryItem.libraryFolderId)
            }
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
        currentlyPracticedSection
    ) { itemsInSelectedFolder, currentlyPracticedSection ->
        ActiveSessionDraggableCardBodyUiState.LibraryCardBodyUiState(
            items = itemsInSelectedFolder,
            activeItemId = currentlyPracticedSection?.libraryItem?.id
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


    val uiState = combine(
        libraryCardUiState,
        sessionState,
        addLibraryItemDialogUiState,
        totalDurationRoundedDownToNextSecond,
        endSessionDialogUiState
    ) { libraryCardUiState, sessionState, addItemDialogUiState, totalDuration, endSessionDialogUiState ->
        ActiveSessionUiState(
            cardUiStates = listOf(
                libraryCardUiState,
                _recorderUiState,
                _metronomeUiState
            ),
            totalSessionDuration = totalDuration,
            totalBreakDuration = sessionState.pauseDuration,
            isPaused = sessionState.isPaused,
            addItemDialogUiState = addItemDialogUiState,
            sections = sessionState.sections.reversed().map { section ->
                ActiveSessionSectionListItemUiState(
                    id = section.id,    // also used as id
                    libraryItem = section.libraryItem,
                    duration = section.duration ?: sessionState.currentSectionDuration
                )
            },
            endDialogUiState = endSessionDialogUiState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActiveSessionUiState(
            cardUiStates = emptyList(),
            totalSessionDuration = 0.milliseconds,
            totalBreakDuration = sessionState.value.pauseDuration,
            sections = emptyList(),
            isPaused = sessionState.value.isPaused,
            addItemDialogUiState = addLibraryItemDialogUiState.value,
            endDialogUiState = endSessionDialogUiState.value
        )
    )


    /** UI Event handler */

    fun onUiEvent(event: ActiveSessionUiEvent) {
        when (event) {
            is ActiveSessionUiEvent.SelectFolder -> _selectedFolderId.update { event.folderId }
            is ActiveSessionUiEvent.SelectItem -> itemClicked(event.item)
            is ActiveSessionUiEvent.TogglePause -> togglePause()
            is ActiveSessionUiEvent.StopSession -> _endDialogData.update { EndDialogData(
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
        }
    }



    /** ---------------------------------- private methods ----------------------------------- */

    private fun itemClicked(item: LibraryItem) {
        if (!bound) return
        sessionEvent(SessionEvent.StartNewSection(item))
        startService()
    }

    private fun togglePause() {
        sessionEvent(SessionEvent.TogglePause)
    }

    private fun stopSession() {
        viewModelScope.launch {
            sessionUseCases.add(
                sessionCreationAttributes = SessionCreationAttributes(
                    breakDuration = sessionState.value.pauseDuration,
                    comment = "New Session from new ActiveSession Screen!",
                    rating = 5
                ),
                sectionCreationAttributes = sessionState.value.sections.map {
                    SectionCreationAttributes(
                        libraryItemId = it.libraryItem.id,
                        duration = it.duration ?: sessionState.value.currentSectionDuration,
                        startTimestamp = it.startTimestamp
                    )
                }
            )
            stopService()
            _endDialogData.update { null }
        }
    }

    private fun removeSection(itemId: Int) {
        sessionEvent(SessionEvent.DeleteSection(itemId))
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
        Log.d("TAG", "startService Button pressed")
        Log.d("TAG", "start Foreground Service ")
        val intent = Intent(application, SessionService::class.java) // Build the intent for the service
        intent.action = SessionServiceAction.START.name
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            application.startForegroundService(intent)
        } else {
            application.startService(intent)
        }
    }

    private fun stopService() {
        Log.d("TAG", "stopService Button pressed")
        val intent = Intent(application, SessionService::class.java) // Build the intent for the service
        intent.action = SessionServiceAction.STOP.name
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            application.startForegroundService(intent)
        } else {
            application.startService(intent)
        }
    }

    private fun bindService() {
        Log.d("TAG", "bindService Button pressed")
        val intent = Intent(application, SessionService::class.java) // Build the intent for the service
        application.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onCleared() {
        Log.d("TAG", "onCleared")
        if (bound) {
            application.unbindService(connection)
            bound = false
        }
        super.onCleared()
    }
}