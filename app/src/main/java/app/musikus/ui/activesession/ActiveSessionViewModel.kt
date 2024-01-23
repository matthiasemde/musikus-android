package app.musikus.ui.activesession

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.database.LibraryFolderWithItems
import app.musikus.database.Nullable
import app.musikus.database.daos.LibraryItem
import app.musikus.database.entities.SectionCreationAttributes
import app.musikus.database.entities.SessionCreationAttributes
import app.musikus.usecase.library.LibraryUseCases
import app.musikus.usecase.sessions.SessionsUseCases
import app.musikus.utils.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
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
data class PracticeSection(
    val libraryItem: LibraryItem,
    val startTimestamp: ZonedDateTime,
    var duration: Duration?
)

@HiltViewModel
class ActiveSessionViewModel @Inject constructor(
    private val libraryUseCases: LibraryUseCases,
    private val sessionUseCases: SessionsUseCases,
    private val timeProvider: TimeProvider
): ViewModel() {

    val testTime = MutableStateFlow(0.seconds)

    private var _timer: java.util.Timer? = null

    private val timerInterval = 100.milliseconds

    private val _currentSectionDuration = MutableStateFlow(0.seconds)
    private val _pauseDuration = MutableStateFlow(0.seconds)
    private val _isPaused = MutableStateFlow(false)

    private val _sections = MutableStateFlow<List<PracticeSection>>(listOf())


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
        _currentSectionDuration,
        _isPaused,
        _sections,
        _pauseDuration
    ) { libraryUiState, currentSectionDuration, isPaused, sections, pauseDuration ->
        ActiveSessionUiState(
            libraryUiState = libraryUiState,
            totalSessionDuration = sections.sumOf { (it.duration ?: currentSectionDuration).inWholeMilliseconds }.milliseconds,
            totalBreakDuration = pauseDuration,
            sections = sections.map { section ->
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
            totalBreakDuration = _pauseDuration.value,
            sections = emptyList()
        )
    )


    fun itemClicked(item: LibraryItem) {
        startTimer()
        // end current section and store its duration
        var currentSectionDuration = 0.seconds
        _currentSectionDuration.update {
            currentSectionDuration = it
            0.seconds
        }

        _sections.update {
            // update duration in newest section
            val updatedLastSection =
                    it.lastOrNull()?.copy(duration = currentSectionDuration)
            // create new section
            val newItem = PracticeSection(
                libraryItem = item,
                duration = null,
                startTimestamp = timeProvider.now()
            )

            if(updatedLastSection != null) {
                // return new list with updated item
                it.dropLast(1) + updatedLastSection + newItem
            } else {
                listOf(newItem)
            }
        }
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
                _currentSectionDuration.update { it + timerInterval }
            }
            testTime.update { it + timerInterval }
        }
    }

    fun stopSession() {
        viewModelScope.launch {
            sessionUseCases.add(
                sessionCreationAttributes = SessionCreationAttributes(
                    breakDuration = _pauseDuration.value,
                    comment = "New Session from new ActiveSession Screen!",
                    rating = 5
                ),
                sectionCreationAttributes = _sections.value.map {
                    SectionCreationAttributes(
                        libraryItemId = it.libraryItem.id,
                        duration = it.duration ?: _currentSectionDuration.value,
                        startTimestamp = it.startTimestamp
                    )
                }
            )
        }
        _timer?.cancel()
    }

}