package app.musikus.ui.activesession

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.database.LibraryFolderWithItems
import app.musikus.database.Nullable
import app.musikus.database.daos.LibraryItem
import app.musikus.database.entities.SectionCreationAttributes
import app.musikus.repository.SessionRepository
import app.musikus.usecase.library.LibraryUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.concurrent.timer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

const val TAG = "ActiveSessionViewModel"

data class ActiveSessionUiState(
    val libraryUiState: LibraryCardUiState,
    val totalSessionDuration: Duration,
    val totalBreakDuration: Duration,
    val sections: List<Pair<String, Duration>>
)

data class LibraryCardUiState(
    val items: List<LibraryItem>,
    val foldersWithItems: List<LibraryFolderWithItems>
)

@HiltViewModel
class ActiveSessionViewModel @Inject constructor(
    private val libraryUseCases: LibraryUseCases,
    private val sessionRepository: SessionRepository
): ViewModel() {

    private var _timer: java.util.Timer? = null

    private val timerInterval = 100.milliseconds

    private val _sectionDuration = MutableStateFlow(0.seconds)
    private val _pauseDuration = MutableStateFlow(0.seconds)
    private val _isPaused = MutableStateFlow(false)

    private val _sections = MutableStateFlow<List<SectionCreationAttributes>>(listOf())


    val endpoint = libraryUseCases.getFolders().map {
        Log.d("TAG", it.toString())
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

    private val allLibraryItems = combine(
        rootItems,
        folderWithItems
    ) { rootItems, folderWithItems ->
        rootItems + folderWithItems.flatMap { it.items }
    }.stateIn(
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
        _sectionDuration,
        _isPaused,
        _sections,
        _pauseDuration
    ) { libraryUiState, sectionDuration, isPaused, sections, pauseDuration ->
        ActiveSessionUiState(
            libraryUiState = libraryUiState,
            totalSessionDuration = sectionDuration + sections.sumOf { it.duration.inWholeMilliseconds }.milliseconds,
            totalBreakDuration = pauseDuration,
            sections = sections.map { sectionAttrib ->
                Pair(
                    allLibraryItems.value.find {
                        it.id == sectionAttrib.libraryItemId }?.name ?: "Unknown",
                    sectionAttrib.duration
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = ActiveSessionUiState(
            libraryUiState = libraryUiState.value,
            totalSessionDuration = 0.milliseconds,
            totalBreakDuration = _pauseDuration.value,
            sections = emptyList()
        )
    )

//    fun addSection(LibraryItem) {
//
//    }

    fun folderClicked(folder: LibraryItem) {
//        _sections.update { it + folder.name }
    }

    fun pauseTimer() {
        _isPaused.update { !it }
    }

    fun startTimer() {
        if (_timer != null) {
            return
        }
        _timer = timer(
            name = "Timer",
            initialDelay = timerInterval.inWholeMilliseconds,
            period = timerInterval.inWholeMilliseconds
        ) {
            if (_isPaused.value) {
                _pauseDuration.update { it + timerInterval }
            } else {
                _sectionDuration.update { it + timerInterval }
            }
        }
    }

}