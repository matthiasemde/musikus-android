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
import app.musikus.database.LibraryFolderWithItems
import app.musikus.database.Nullable
import app.musikus.database.daos.LibraryItem
import app.musikus.database.entities.SectionCreationAttributes
import app.musikus.database.entities.SessionCreationAttributes
import app.musikus.services.SessionEvent
import app.musikus.services.SessionService
import app.musikus.services.SessionState
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

const val TAG = "ActiveSessionViewModel"


sealed class ActiveSessionUIEvent {
    data class StartNewSection(val libraryItem: LibraryItem): ActiveSessionUIEvent()
    data object TogglePause: ActiveSessionUIEvent()
    data object StopSession: ActiveSessionUIEvent()
}

data class ActiveSessionUiState(
    val libraryUiState: LibraryCardUiState,
    val totalSessionDuration: Duration,
    val totalBreakDuration: Duration,
    val sections: List<Pair<String, Duration>>,
    val isPaused: Boolean
)

data class LibraryCardUiState(
    val rootItems: List<LibraryItem>,
    val foldersWithItems: List<LibraryFolderWithItems>
)
data class PracticeSection(
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
    
    // ################## mirrors of session state

    private val sessionStateWrapper = MutableStateFlow<StateFlow<SessionState>?>(null)
    private val sessionState = sessionStateWrapper.flatMapLatest { it ?: flowOf(SessionState()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
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

    private val rootItems = libraryUseCases.getSortedItems(Nullable(null)).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = listOf()
    )

    private val folderWithItems = libraryUseCases.getSortedFolders().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = listOf()
    )

    private val libraryUiState = combine(
        rootItems,
        folderWithItems
    ) { rootItems, folderWithItems ->
        LibraryCardUiState(
            rootItems = rootItems,
            foldersWithItems = folderWithItems
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = LibraryCardUiState(
            rootItems = rootItems.value,
            foldersWithItems = folderWithItems.value
        )
    )

    val uiState = combine(
        libraryUiState,
        sessionState
    ) { libraryUiState, sessionState ->
        ActiveSessionUiState(
            libraryUiState = libraryUiState,
            totalSessionDuration = sessionState.sections.sumOf {
                (it.duration ?: sessionState.currentSectionDuration).inWholeMilliseconds }.milliseconds,
            totalBreakDuration = sessionState.pauseDuration,
            isPaused = sessionState.isPaused,
            sections = sessionState.sections.reversed().map { section ->
                Pair(
                    section.libraryItem.name,
                    section.duration ?: sessionState.currentSectionDuration
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = ActiveSessionUiState(
            libraryUiState = libraryUiState.value,
            totalSessionDuration = 0.milliseconds,
            totalBreakDuration = sessionState.value.pauseDuration,
            sections = emptyList(),
            isPaused = sessionState.value.isPaused
        )
    )


    /** UI Event handler */
    fun onEvent(event: ActiveSessionUIEvent) = when (event) {
        is ActiveSessionUIEvent.StartNewSection -> itemClicked(event.libraryItem)
        is ActiveSessionUIEvent.TogglePause -> togglePause()
        is ActiveSessionUIEvent.StopSession -> stopSession()
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