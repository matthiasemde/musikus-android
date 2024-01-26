/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.database.daos.LibraryItem
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalInstanceCreationAttributes
import app.musikus.database.entities.GoalInstanceUpdateAttributes
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.database.entities.GoalType
import app.musikus.shared.TopBarUiState
import app.musikus.ui.library.DialogMode
import app.musikus.usecase.goals.GoalInstanceWithProgressAndDescriptionWithLibraryItems
import app.musikus.usecase.goals.GoalsUseCases
import app.musikus.usecase.library.LibraryUseCases
import app.musikus.usecase.userpreferences.UserPreferencesUseCases
import app.musikus.utils.GoalsSortMode
import app.musikus.utils.SortDirection
import app.musikus.utils.SortInfo
import app.musikus.utils.TimeProvider
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


data class GoalDialogData(
    val target: Duration = 0.seconds,
    val periodInPeriodUnits: Int = 0,
    val periodUnit: GoalPeriodUnit = GoalPeriodUnit.DEFAULT,
    val goalType: GoalType = GoalType.DEFAULT,
    val oneShot: Boolean = false,
    val selectedLibraryItems: List<LibraryItem> = emptyList(),
)

/**
 * Ui state data classes
 */
data class GoalsSortMenuUiState(
    val show: Boolean,

    val mode: GoalsSortMode,
    val direction: SortDirection,
)

data class GoalsTopBarUiState(
    override val title: String,
    override val showBackButton: Boolean,
    val sortMenuUiState: GoalsSortMenuUiState,
) : TopBarUiState

data class GoalsActionModeUiState(
    val isActionMode: Boolean,
    val numberOfSelections: Int,
    val showEditAction: Boolean,
)

data class GoalsContentUiState(
    val currentGoals: List<GoalInstanceWithProgressAndDescriptionWithLibraryItems>,
    val selectedGoalIds: Set<UUID>,

    val showHint: Boolean,
)

data class GoalsDialogUiState(
    val mode: DialogMode,
    val goalToEditId: UUID?,
    val dialogData: GoalDialogData,
    val libraryItems: List<LibraryItem>,
)

data class GoalsUiState (
    val topBarUiState: GoalsTopBarUiState,
    val actionModeUiState: GoalsActionModeUiState,
    val contentUiState: GoalsContentUiState,
    val dialogUiState: GoalsDialogUiState?,
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val timeProvider: TimeProvider,
    private val userPreferencesUseCases: UserPreferencesUseCases,
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

    private val goalsSortInfo = userPreferencesUseCases.getGoalSortInfo().stateIn(
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

    private val items = libraryUseCases.getItems().stateIn(
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
            title = "Goals",
            showBackButton = false,
            sortMenuUiState = sortMenuUiState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsTopBarUiState(
            title = "Goals",
            showBackButton = false,
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

    private val dialogUiState = combine(
        _dialogData,
        _goalToEditId,
        items,
    ) { dialogData, goalToEditId, items ->
        // if data == null, the ui state should be null ergo we don't show the dialog
        if(dialogData == null) return@combine null

        GoalsDialogUiState(
            mode = if (goalToEditId == null) DialogMode.ADD else DialogMode.EDIT,
            dialogData = dialogData,
            goalToEditId = goalToEditId,
            libraryItems = items,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
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

    /** State modifiers */

    fun showDialog(oneShot: Boolean) {
        _dialogData.update {
            GoalDialogData(
                target = 0.seconds,
                periodInPeriodUnits = 1,
                periodUnit = GoalPeriodUnit.DAY,
                goalType = GoalType.NON_SPECIFIC,
                oneShot = oneShot,
                selectedLibraryItems = items.value.firstOrNull()?.let {
                    listOf(it)
                } ?: emptyList(),
            )
        }
    }

    fun onSortMenuShowChanged(show: Boolean) {
        _showSortModeMenu.update { show }
    }

    fun onSortModeSelected(selection: GoalsSortMode) {
        _showSortModeMenu.update { false }
        viewModelScope.launch {
            userPreferencesUseCases.selectGoalSortMode(selection)
        }
    }

    fun onEditAction() {
        val selectedGoalId = _selectedGoalIds.value.single()
        val goalToEdit = currentGoals.value.single {
            it.description.description.id == selectedGoalId
        }
        _goalToEditId.update { goalToEdit.description.description.id }
        _dialogData.update {
            GoalDialogData(
                target = goalToEdit.instance.target,
            )
        }
        clearActionMode()
    }

    fun onArchiveAction() {
        viewModelScope.launch {
            _goalIdsCache = _selectedGoalIds.value
            goalsUseCases.archive(_selectedGoalIds.value.toList())
            clearActionMode()
        }
    }

    fun onUndoArchiveAction() {
        viewModelScope.launch {
            goalsUseCases.unarchive(_goalIdsCache.toList())
        }
    }

    fun onDeleteAction() {
        viewModelScope.launch {
            _goalIdsCache = _selectedGoalIds.value
            goalsUseCases.delete(_selectedGoalIds.value.toList())
            clearActionMode()
        }
    }

    fun onRestoreAction() {
        viewModelScope.launch {
            goalsUseCases.restore(_goalIdsCache.toList())
        }
    }

    fun clearActionMode() {
        _selectedGoalIds.update{ emptySet() }
    }

    fun onGoalClicked(
        goal: GoalInstanceWithProgressAndDescriptionWithLibraryItems,
        longClick: Boolean = false
    ) {
        val descriptionId = goal.description.description.id

        if (longClick) {
            _selectedGoalIds.update { it + descriptionId }
            return
        }

        // Short Click
        if(!actionModeUiState.value.isActionMode) {
            if(!goal.description.description.paused) {
                _goalToEditId.update { descriptionId }
                _dialogData.update {
                    GoalDialogData(
                        target = goal.instance.target,
                    )
                }
            }
        } else {
            if(descriptionId in _selectedGoalIds.value) {
                _selectedGoalIds.update { it - descriptionId }
            } else {
                _selectedGoalIds.update { it + descriptionId }
            }
        }
    }

    fun onTargetChanged(target: Duration) {
        _dialogData.update { it?.copy(target = target) }
    }

    fun onPeriodChanged(period: Int) {
        _dialogData.update { it?.copy(periodInPeriodUnits = period) }
    }

    fun onPeriodUnitChanged(unit: GoalPeriodUnit) {
        _dialogData.update { it?.copy(periodUnit = unit) }
    }

    fun onGoalTypeChanged(type: GoalType) {
        _dialogData.update { it?.copy(goalType = type) }
    }

    fun onLibraryItemsChanged(items: List<LibraryItem>) {
        _dialogData.update { it?.copy(selectedLibraryItems = items) }
    }

    fun clearDialog() {
        _dialogData.update { null }
        _goalToEditId.update { null }
    }

    fun onDialogConfirm() {
        dialogUiState.value?.let { uiState ->
            val dialogData = uiState.dialogData
            viewModelScope.launch {
                if (uiState.goalToEditId == null) {
                    goalsUseCases.add(
                        GoalDescriptionCreationAttributes(
                            type = dialogData.goalType,
                            repeat = !dialogData.oneShot,
                            periodInPeriodUnits = dialogData.periodInPeriodUnits,
                            periodUnit = dialogData.periodUnit,
                        ),
                        instanceCreationAttributes = GoalInstanceCreationAttributes(
                            startTimestamp = when(dialogData.periodUnit) {
                                GoalPeriodUnit.DAY -> timeProvider.getStartOfDay()
                                GoalPeriodUnit.WEEK -> timeProvider.getStartOfWeek()
                                GoalPeriodUnit.MONTH -> timeProvider.getStartOfMonth()
                             },
                            target = dialogData.target,
                        ),
                        libraryItemIds = dialogData.selectedLibraryItems.map { it.id },
                    )
                } else {
                    goalsUseCases.edit(
                        descriptionId = uiState.goalToEditId,
                        instanceUpdateAttributes = GoalInstanceUpdateAttributes(
                            target = dialogData.target,
                        ),
                    )
                }
                clearDialog()
            }
        }
    }
}
