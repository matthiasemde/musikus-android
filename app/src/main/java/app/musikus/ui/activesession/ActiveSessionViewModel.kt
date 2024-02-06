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
import app.musikus.database.entities.SectionCreationAttributes
import app.musikus.database.entities.SessionCreationAttributes
import app.musikus.services.SessionEvent
import app.musikus.services.SessionService
import app.musikus.services.SessionState
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

const val TAG = "ActiveSessionViewModel"

data class PracticeSection(
    val id: Int,
    val libraryItem: LibraryItem,
    val startTimestamp: ZonedDateTime,
    var duration: Duration?
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ActiveSessionViewModel @Inject constructor(
    private val libraryUseCases: LibraryUseCases,
    private val sessionUseCases: SessionsUseCases,
    private val application: Application
): AndroidViewModel(application) {


    /** own stateFlows */

    private val _selectedFolderId = MutableStateFlow<UUID?>(null)
    private val _addLibraryItemData = MutableStateFlow<LibraryItemEditData?>(null)

    // ################## mirrors of session state

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
            fabAction = { createLibraryItemDialog(headUiState.selectedFolderId) },
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
            fabAction = { createLibraryItemDialog(null) },
            headerUiState = libraryCardHeaderUiState.value,
            bodyUiState = libraryCardBodyUiState.value
        )
    )

    val uiState = combine(
        libraryCardUiState,
        sessionState,
        _addLibraryItemData
    ) { libraryCardUiState, sessionState, newLibraryItemData ->
        val totalDuration = sessionState.sections.sumOf {
            (it.duration ?: sessionState.currentSectionDuration).inWholeMilliseconds }.milliseconds
        ActiveSessionUiState(
            libraryCardUiState = libraryCardUiState,
            totalSessionDuration = totalDuration,
            totalBreakDuration = sessionState.pauseDuration,
            isPaused = sessionState.isPaused,
            newLibraryItemData = newLibraryItemData,
            sections = sessionState.sections.reversed().map { section ->
                SectionListItemUiState(
                    id = section.id,    // also used as id
                    libraryItem = section.libraryItem,
                    duration = section.duration ?: sessionState.currentSectionDuration
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActiveSessionUiState(
            libraryCardUiState = libraryCardUiState.value,
            totalSessionDuration = 0.milliseconds,
            totalBreakDuration = sessionState.value.pauseDuration,
            sections = emptyList(),
            isPaused = sessionState.value.isPaused,
            newLibraryItemData = _addLibraryItemData.value
        )
    )


    /** UI Event handler */

    fun onUiEvent(event: ActiveSessionUiEvent) = when (event) {
        is ActiveSessionUiEvent.SelectFolder -> _selectedFolderId.update { event.folderId }
        is ActiveSessionUiEvent.SelectItem -> itemClicked(event.item)
        is ActiveSessionUiEvent.TogglePause -> togglePause()
        is ActiveSessionUiEvent.StopSession -> stopSession()
//        is ActiveSessionUIEvent.ChangeFolderDisplayed -> _selectedFolder.update { event.folder }
        is ActiveSessionUiEvent.DeleteSection -> removeSection(event.sectionId)
//        is ActiveSessionUIEvent.CreateNewLibraryItem -> createLibraryItemDialog(event.folderId)
//        is ActiveSessionUIEvent.NewLibraryItemNameChanged -> _addLibraryItemData.update { it?.copy(name = event.newName) }
//        is ActiveSessionUIEvent.NewLibraryItemColorChanged -> _addLibraryItemData.update { it?.copy(colorIndex = event.newColorIndex) }
//        is ActiveSessionUIEvent.NewLibraryItemFolderChanged -> _addLibraryItemData.update { it?.copy(folderId = event.newFolderId) }
//        is ActiveSessionUIEvent.NewLibraryItemDialogDismissed -> _addLibraryItemData.update { null }
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
        }
        sessionEvent(SessionEvent.StopTimer)
    }

    private fun removeSection(itemId: Int) {
        sessionEvent(SessionEvent.DeleteSection(itemId))
    }

    private fun createLibraryItemDialog(folderId: UUID? = null) {
        _addLibraryItemData.update {
            LibraryItemEditData(
                name = "",
                colorIndex = (Math.random() * 10).toInt(),
                folderId = folderId
            )
        }
    }

    // ################################### Service ############################################

    private fun startService() {
        Log.d("TAG", "startService Button pressed")
        Log.d("TAG", "start Foreground Service ")
        val intent = Intent(application, SessionService::class.java) // Build the intent for the service
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