/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde
 */

package app.musikus.goals.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.core.domain.SortDirection
import app.musikus.core.domain.SortInfo
import app.musikus.goals.data.GoalsSortMode
import app.musikus.goals.data.entities.GoalDescriptionCreationAttributes
import app.musikus.goals.data.entities.GoalInstanceCreationAttributes
import app.musikus.goals.data.entities.GoalInstanceUpdateAttributes
import app.musikus.goals.data.entities.GoalPeriodUnit
import app.musikus.goals.data.entities.GoalType
import app.musikus.goals.domain.GoalInstanceWithProgressAndDescriptionWithLibraryItems
import app.musikus.goals.domain.usecase.GoalsUseCases
import app.musikus.library.data.daos.LibraryItem
import app.musikus.library.domain.usecase.LibraryUseCases
import app.musikus.library.presentation.DialogMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel state for the dialog which is displayed when adding or changing a goal.
 */

/**
 * ViewModel state container for the dialog
 */
data class GoalDialogData(
    val initialTarget: Duration = 0.seconds,
    val oneShot: Boolean = false,
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalsUseCases: GoalsUseCases,
    libraryUseCases: LibraryUseCases,
) : ViewModel() {

    private var _goalIdsCache: Set<UUID> = emptySet()

    init {
        viewModelScope.launch {
            goalsUseCases.update()
        }
    }

    /** Imported flows */

    private val goalsSortInfo = goalsUseCases.getGoalSortInfo().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SortInfo(
            mode = GoalsSortMode.DEFAULT,
            direction = SortDirection.DEFAULT
        )
    )

    private val currentGoals = goalsUseCases.getCurrent(excludePaused = false).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val items = libraryUseCases.getSortedItems().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /** Own state flows */

    // Menu
    private val _showSortModeMenu = MutableStateFlow(false)

    // Goal dialog
    private val _dialogData = MutableStateFlow<GoalDialogData?>(null)
    private val _goalToEditId = MutableStateFlow<UUID?>(null)

    // Action mode
    private val _selectedGoalIds = MutableStateFlow<Set<UUID>>(emptySet())

    // Delete or Archive dialog
    private val _showDeleteOrArchiveDialog = MutableStateFlow(false)
    private val _deleteOrArchiveDialogIsArchiveAction = MutableStateFlow(false)

    /**
     *  Composing the Ui state
     */
    private val sortMenuUiState = combine(
        goalsSortInfo,
        _showSortModeMenu
    ) { (mode, direction), show ->
        GoalsSortMenuUiState(
            show = show,
            mode = mode as GoalsSortMode,
            direction = direction,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsSortMenuUiState(
            show = false,
            mode = GoalsSortMode.DEFAULT,
            direction = SortDirection.DEFAULT,
        )
    )

    private val topBarUiState = sortMenuUiState.map { sortMenuUiState ->
        GoalsTopBarUiState(
            sortMenuUiState = sortMenuUiState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsTopBarUiState(
            sortMenuUiState = sortMenuUiState.value,
        )
    )

    private val actionModeUiState = _selectedGoalIds.map {
        GoalsActionModeUiState(
            isActionMode = it.isNotEmpty(),
            numberOfSelections = it.size,
            showEditAction = it.size == 1,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsActionModeUiState(
            isActionMode = false,
            numberOfSelections = 0,
            showEditAction = false,
        )
    )

    private val contentUiState = combine(
        currentGoals,
        _selectedGoalIds,
    ) { currentGoals, selectedGoals ->
        GoalsContentUiState(
            currentGoals = currentGoals,
            selectedGoalIds = selectedGoals,
            showHint = currentGoals.isEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsContentUiState(
            currentGoals = currentGoals.value,
            selectedGoalIds = _selectedGoalIds.value,
            showHint = true,
        )
    )

    private val addOrEditDialogUiState = combine(
        _dialogData,
        _goalToEditId,
        items,
    ) { dialogData, goalToEditId, items ->
        // if data == null, the ui state should be null ergo we don't show the dialog
        if (dialogData == null) return@combine null

        GoalsAddOrEditDialogUiState(
            mode = if (goalToEditId == null) DialogMode.ADD else DialogMode.EDIT,
            goalToEditId = goalToEditId,
            libraryItems = items,
            initialTargetHours = dialogData.initialTarget.inWholeHours.toInt(),
            initialTargetMinutes = dialogData.initialTarget.inWholeMinutes.toInt() % 60,
            oneShotGoal = dialogData.oneShot,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val deleteOrArchiveDialogUiState = combine(
        _showDeleteOrArchiveDialog,
        _deleteOrArchiveDialogIsArchiveAction,
        _selectedGoalIds,
    ) { show, isArchiveAction, selectedGoalIds ->
        if (!show) return@combine null

        GoalsDeleteOrArchiveDialogUiState(
            isArchiveAction = isArchiveAction,
            numberOfSelections = selectedGoalIds.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val dialogUiState = combine(
        addOrEditDialogUiState,
        deleteOrArchiveDialogUiState,
    ) { addOrEditDialogUiState, deleteOrArchiveDialogUiState ->
        GoalsDialogUiState(
            addOrEditDialogUiState = addOrEditDialogUiState,
            deleteOrArchiveDialogUiState = deleteOrArchiveDialogUiState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsDialogUiState(
            addOrEditDialogUiState = addOrEditDialogUiState.value,
            deleteOrArchiveDialogUiState = deleteOrArchiveDialogUiState.value,
        )
    )

    val uiState = combine(
        topBarUiState,
        actionModeUiState,
        contentUiState,
        dialogUiState,
    ) { topBarUiState, actionModeUiState, contentUiState, dialogUiState ->
        GoalsUiState(
            topBarUiState = topBarUiState,
            actionModeUiState = actionModeUiState,
            contentUiState = contentUiState,
            dialogUiState = dialogUiState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsUiState(
            topBarUiState = topBarUiState.value,
            actionModeUiState = actionModeUiState.value,
            contentUiState = contentUiState.value,
            dialogUiState = dialogUiState.value,
        )
    )

    /**
     * Ui event handler
     */

    fun onUiEvent(event: GoalsUiEvent): Boolean {
        when (event) {
            is GoalsUiEvent.BackButtonPressed -> clearActionMode()

            is GoalsUiEvent.GoalPressed -> onGoalClicked(event.goal, event.longClick)

            is GoalsUiEvent.GoalSortMenuPressed -> onSortMenuShowChanged(_showSortModeMenu.value.not())
            is GoalsUiEvent.GoalSortModeSelected -> onSortModeSelected(event.mode)

            is GoalsUiEvent.ArchiveButtonPressed -> {
                _showDeleteOrArchiveDialog.update { true }
                _deleteOrArchiveDialogIsArchiveAction.update { true }
            }
            is GoalsUiEvent.DeleteButtonPressed -> {
                _showDeleteOrArchiveDialog.update { false }
                _showDeleteOrArchiveDialog.update { true }
            }
            is GoalsUiEvent.DeleteOrArchiveDialogDismissed -> {
                _showDeleteOrArchiveDialog.update { false }
                clearActionMode()
            }
            is GoalsUiEvent.DeleteOrArchiveDialogConfirmed -> {
                _showDeleteOrArchiveDialog.update { false }
                if (_deleteOrArchiveDialogIsArchiveAction.value) {
                    onArchiveAction()
                } else {
                    onDeleteAction()
                }
            }
            is GoalsUiEvent.UndoButtonPressed -> {
                if (_deleteOrArchiveDialogIsArchiveAction.value) {
                    onUndoArchiveAction()
                } else {
                    onRestoreAction()
                }
            }
            is GoalsUiEvent.EditButtonPressed -> onEditAction()

            is GoalsUiEvent.AddGoalButtonPressed -> showDialog(event.oneShot)

            is GoalsUiEvent.ClearActionMode -> clearActionMode()

            /**
             * Event handler for changes within the dialogs
             */
            is GoalsUiEvent.DialogUiEvent -> {
                when (event.dialogEvent) {
                    is GoalDialogUiEvent.Confirm -> onDialogConfirm(
                        target = event.dialogEvent.target,
                        period = event.dialogEvent.period,
                        periodUnit = event.dialogEvent.periodUnit,
                        goalType = event.dialogEvent.goalType,
                        selectedLibraryItems = event.dialogEvent.selectedLibraryItems
                    )
                    is GoalDialogUiEvent.Dismiss -> clearDialog()
                }
            }
        }

        // events are consumed by default
        return true
    }

    /**
     * Private state modifiers
     */

    private fun showDialog(oneShot: Boolean) {
        _dialogData.update {
            GoalDialogData(oneShot = oneShot)
        }
    }

    private fun onSortMenuShowChanged(show: Boolean) {
        _showSortModeMenu.update { show }
    }

    private fun onSortModeSelected(selection: GoalsSortMode) {
        _showSortModeMenu.update { false }
        viewModelScope.launch {
            goalsUseCases.selectGoalSortMode(selection)
        }
    }

    private fun onEditAction() {
        val selectedGoalId = _selectedGoalIds.value.single()
        val goalToEdit = currentGoals.value.single {
            it.description.description.id == selectedGoalId
        }
        _goalToEditId.update { goalToEdit.description.description.id }
        _dialogData.update {
            GoalDialogData(
                initialTarget = goalToEdit.instance.target,
            )
        }
        clearActionMode()
    }

    private fun onArchiveAction() {
        viewModelScope.launch {
            _goalIdsCache = _selectedGoalIds.value
            goalsUseCases.archive(_selectedGoalIds.value.toList())
            clearActionMode()
        }
    }

    private fun onUndoArchiveAction() {
        viewModelScope.launch {
            goalsUseCases.unarchive(_goalIdsCache.toList())
        }
    }

    private fun onDeleteAction() {
        viewModelScope.launch {
            _goalIdsCache = _selectedGoalIds.value
            goalsUseCases.delete(_selectedGoalIds.value.toList())
            clearActionMode()
        }
    }

    private fun onRestoreAction() {
        viewModelScope.launch {
            goalsUseCases.restore(_goalIdsCache.toList())
        }
    }

    private fun clearActionMode() {
        _selectedGoalIds.update { emptySet() }
    }

    private fun onGoalClicked(
        goal: GoalInstanceWithProgressAndDescriptionWithLibraryItems,
        longClick: Boolean = false
    ) {
        val descriptionId = goal.description.description.id

        if (longClick) {
            _selectedGoalIds.update { it + descriptionId }
            return
        }

        // Short Click
        if (!actionModeUiState.value.isActionMode) {
            if (!goal.description.description.paused) {
                _goalToEditId.update { descriptionId }
                _dialogData.update {
                    GoalDialogData(
                        initialTarget = goal.instance.target,
                    )
                }
            }
        } else {
            if (descriptionId in _selectedGoalIds.value) {
                _selectedGoalIds.update { it - descriptionId }
            } else {
                _selectedGoalIds.update { it + descriptionId }
            }
        }
    }

    private fun clearDialog() {
        _dialogData.update { null }
        _goalToEditId.update { null }
    }

    private fun onDialogConfirm(
        target: Duration,
        period: Int,
        periodUnit: GoalPeriodUnit,
        goalType: GoalType,
        selectedLibraryItems: List<LibraryItem>
    ) {
        val dialogData = _dialogData.value ?: return
        viewModelScope.launch {
            _goalToEditId.value?.let { goalToEditId ->
                goalsUseCases.edit(
                    descriptionId = goalToEditId,
                    instanceUpdateAttributes = GoalInstanceUpdateAttributes(
                        target = target,
                    ),
                )
            } ?: goalsUseCases.add(
                GoalDescriptionCreationAttributes(
                    type = goalType,
                    repeat = !dialogData.oneShot,
                    periodInPeriodUnits = period,
                    periodUnit = periodUnit,
                ),
                instanceCreationAttributes = GoalInstanceCreationAttributes(
                    target = target,
                ),
                libraryItemIds = if (goalType == GoalType.ITEM_SPECIFIC) {
                    selectedLibraryItems.map { it.id }
                } else {
                    emptyList()
                }
            )
            clearDialog()
        }
    }
}
