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
import app.musikus.services.SessionService
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
import kotlin.time.Duration.Companion.seconds

const val TAG = "ActiveSessionViewModel"

data class ActiveSessionUiState(
    val libraryUiState: LibraryCardUiState,
    val totalSessionDuration: Duration,
    val totalBreakDuration: Duration,
    val sections: List<Pair<String, Duration>>,
    val isPaused: Boolean
)

data class LibraryCardUiState(
    val items: List<LibraryItem>,
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
    
    private val sectionsWrapper = MutableStateFlow<StateFlow<List<PracticeSection>>?>(null)
    private val currentSectionDurationWrapper = MutableStateFlow<StateFlow<Duration>?>(null)
    private val pauseDurationWrapper =  MutableStateFlow<StateFlow<Duration>?>(null)
    private val isPausedWrapper =  MutableStateFlow<StateFlow<Boolean>?>(null)

    private val sections = sectionsWrapper.flatMapLatest { it ?: flowOf(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )
    private val currentSectionDuration = currentSectionDurationWrapper.flatMapLatest { it ?: flowOf(0.seconds) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = 0.seconds
        )
    private val pauseDuration = pauseDurationWrapper.flatMapLatest { it ?: flowOf(0.seconds) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = 0.seconds
        )
    private val isPaused = isPausedWrapper.flatMapLatest { it ?: flowOf(false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false
        )

    // #############################################    

    private lateinit var sessionService: SessionService
    private var bound = false

    private val connection = object : ServiceConnection {
        /** called by service when we have connection to the service => we have mService reference */
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to SessionForegroundService, cast the IBinder and get SessionForegroundService instance
            Log.d("TAG", "onServiceConnected")
            val binder = service as SessionService.LocalBinder
            sessionService = binder.getService()

            sectionsWrapper.update { sessionService.sections }
            currentSectionDurationWrapper.update { sessionService.currentSectionDuration }
            pauseDurationWrapper.update { sessionService.pauseDuration }
            isPausedWrapper.update { sessionService.isPaused }

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

    private val rootItems = libraryUseCases.getItems(Nullable(null)).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = listOf()
    )

    private val folderWithItems = libraryUseCases.getFolders().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = listOf()
    )

    private val libraryUiState = combine(
        rootItems,
        folderWithItems
    ) { rootItems, folderWithItems ->
        LibraryCardUiState(
            items = rootItems,
            foldersWithItems = folderWithItems
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = LibraryCardUiState(
            items = rootItems.value,
            foldersWithItems = folderWithItems.value
        )
    )

    val uiState = combine(
        libraryUiState,
        currentSectionDuration,
        isPaused,
        sections,
        pauseDuration
    ) { libraryUiState, currentSectionDuration, isPaused, sections, pauseDuration ->
        ActiveSessionUiState(
            libraryUiState = libraryUiState,
            totalSessionDuration = sections.sumOf { (it.duration ?: currentSectionDuration).inWholeMilliseconds }.milliseconds,
            totalBreakDuration = pauseDuration,
            isPaused = isPaused,
            sections = sections.reversed().map { section ->
                Pair(
                    section.libraryItem.name,
                    section.duration ?: currentSectionDuration
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = ActiveSessionUiState(
            libraryUiState = libraryUiState.value,
            totalSessionDuration = 0.milliseconds,
            totalBreakDuration = pauseDuration.value,
            sections = emptyList(),
            isPaused = isPaused.value
        )
    )

    fun itemClicked(item: LibraryItem) {
        if (!bound) return
        sessionService.newSection(item)
        startService()
    }

    fun togglePause() {
        sessionService.togglePause()
    }

    fun stopSession() {
        viewModelScope.launch {
            sessionUseCases.add(
                sessionCreationAttributes = SessionCreationAttributes(
                    breakDuration = pauseDuration.value,
                    comment = "New Session from new ActiveSession Screen!",
                    rating = 5
                ),
                sectionCreationAttributes = sections.value.map {
                    SectionCreationAttributes(
                        libraryItemId = it.libraryItem.id,
                        duration = it.duration ?: currentSectionDuration.value,
                        startTimestamp = it.startTimestamp
                    )
                }
            )
        }
        sessionService.stopTimer()
    }

    // ################################### Service ############################################

    fun startService() {
        Log.d("TAG", "startService Button pressed")
        Log.d("TAG", "start Foreground Service ")
        val intent = Intent(application, SessionService::class.java) // Build the intent for the service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            application.startForegroundService(intent)
        } else {
            application.startService(intent)
        }
    }

    fun bindService() {
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