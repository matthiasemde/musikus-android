package app.musikus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.database.MusikusDatabase
import app.musikus.database.SectionWithLibraryItem
import app.musikus.repository.LibraryRepository
import app.musikus.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.UUID

data class SessionEditData(
    val rating: Int,
    val comment: String,
    val sections: List<SectionWithLibraryItem>,
)

/** Ui state data classes */

data class EditSessionContentUiState (
    val sessionEditData: SessionEditData,
)

data class EditSessionUiState(
    val contentUiState: EditSessionContentUiState,
    val showConfirmationDialog: Boolean,
)

class EditSessionViewModel(
    application: Application,
) : AndroidViewModel(application) {

    /** Database */
    private val database = MusikusDatabase.getInstance(application)

    /** Repositories */
    private val libraryRepository = LibraryRepository(database)
    private val sessionRepository = SessionRepository(database)

    /** Own state flows */
    private val _sessionToEditId: MutableStateFlow<UUID?> = MutableStateFlow(null)
    private val _showConfirmationDialog = MutableStateFlow(false)
    private val _sessionEditData = MutableStateFlow(
        SessionEditData(
            rating = 3,
            comment = "Initial comment",
            sections = emptyList()
        )
    )

    /** Combining imported and own flows */
    private val sessionToEdit = _sessionToEditId.map { sessionId ->
        if (sessionId != null) {
            sessionRepository.sessionWithSectionsWithLibraryItems(sessionId)
        } else {
            null
        }
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = null
    )

    /**
     * Composing the Ui state
     */
    private val contentUiState = _sessionEditData.map { sessionEditData ->
        EditSessionContentUiState(sessionEditData)
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = EditSessionContentUiState(
            sessionEditData = _sessionEditData.value,
        )
    )

    val editSessionUiState = combine(
        contentUiState,
        _showConfirmationDialog,
    ) { contentUiState, showConfirmationDialog ->
        EditSessionUiState(
            contentUiState,
            showConfirmationDialog,
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = EditSessionUiState(
            contentUiState = contentUiState.value,
            _showConfirmationDialog.value,
        )
    )

    /** State modifiers */
    fun setSessionToEditId(id: UUID) {
        _sessionToEditId.update { id }
        _sessionEditData.update { sessionEditData ->
            sessionEditData.copy(
                comment = id.toString()
            )
        }
    }

    fun onShowConfirmationDialog() {
        _showConfirmationDialog.update { true }
    }

    fun onRatingChanged(newValue: Int) {
        _sessionEditData.update { sessionEditData ->
            sessionEditData.copy(
                rating = newValue
            )
        }
    }
    fun onCommentChanged(newValue: String) {
        _sessionEditData.update { sessionEditData ->
            sessionEditData.copy(
                comment = newValue
            )
        }
    }

    fun onConfirmHandler() {
        _showConfirmationDialog.update { true }
    }

    fun onCancelHandler() {
        _showConfirmationDialog.update { true }
    }
}