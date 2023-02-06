/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.practicetime.practicetime.dataStore
import de.practicetime.practicetime.database.GoalInstanceWithDescriptionWithLibraryItems
import de.practicetime.practicetime.database.PTDatabase
import de.practicetime.practicetime.database.entities.GoalPeriodUnit
import de.practicetime.practicetime.database.entities.GoalType
import de.practicetime.practicetime.database.entities.LibraryItem
import de.practicetime.practicetime.datastore.GoalsSortMode
import de.practicetime.practicetime.datastore.LibraryItemSortMode
import de.practicetime.practicetime.datastore.SortDirection
import de.practicetime.practicetime.repository.GoalRepository
import de.practicetime.practicetime.repository.LibraryRepository
import de.practicetime.practicetime.repository.SessionRepository
import de.practicetime.practicetime.repository.UserPreferencesRepository
import de.practicetime.practicetime.shared.MultiFABState
import de.practicetime.practicetime.shared.TopBarUiState
import de.practicetime.practicetime.ui.goals.updateGoals
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GoalWithProgress(
    val goal: GoalInstanceWithDescriptionWithLibraryItems,
    val progress: Int,
)

data class GoalDialogData(
    val target: Int,
    val periodInPeriodUnits: Int? = null,
    val periodUnit: GoalPeriodUnit? = null,
    val goalType: GoalType? = null,
    val oneShot: Boolean? = null,
    val libraryItems: List<LibraryItem>? = null,
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
    val sortMenuUiState: GoalsSortMenuUiState
) : TopBarUiState

data class GoalsActionModeUiState(
    val isActionMode: Boolean,
    val numberOfSelections: Int,
)

data class GoalsContentUiState(
    val goalsWithProgress: List<GoalWithProgress>,
    val selectedGoals: Set<GoalInstanceWithDescriptionWithLibraryItems>,

    val showHint: Boolean,
)

data class GoalsDialogUiState(
    val mode: DialogMode,
    val goalToEdit: GoalInstanceWithDescriptionWithLibraryItems?,
    val dialogData: GoalDialogData,
    val isPeriodUnitSelectorExpanded: Boolean,
    val items: List<LibraryItem>,
    val isItemSelectorExpanded: Boolean,
    val confirmButtonEnabled: Boolean,
)

data class GoalsUiState (
    val topBarUiState: GoalsTopBarUiState,
    val actionModeUiState: GoalsActionModeUiState,
    val contentUiState: GoalsContentUiState,
    val dialogUiState: GoalsDialogUiState?,
//    val fabUiState: GoalsFabUiState,
)

class GoalsViewModel(
    application: Application
) : AndroidViewModel(application) {

    /** Database */
    private val database = PTDatabase.getInstance(application)

    /** Repositories */
    private val libraryRepository = LibraryRepository(database)
    private val goalRepository = GoalRepository(database)
    private val sessionRepository = SessionRepository(database)
    private val userPreferencesRepository = UserPreferencesRepository(application.dataStore)

    init {
        viewModelScope.launch {
            updateGoals(application)
        }
    }

    /** Imported flows */

    private val userPreferences = userPreferencesRepository.userPreferences

    private val itemsSortMode = userPreferences.map {
        it.libraryItemSortMode
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryItemSortMode.DEFAULT
    )

    private val itemsSortDirection = userPreferences.map {
        it.libraryItemSortDirection
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SortDirection.DEFAULT
    )

    private val goalsSortMode = userPreferences.map {
        it.goalsSortMode
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsSortMode.DEFAULT
    )

    private val goalsSortDirection = userPreferences.map {
        it.goalsSortDirection
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SortDirection.DEFAULT
    )

    private val currentGoals = goalRepository.currentGoals.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val sortedGoalsWithProgress = combine(
        currentGoals,
        userPreferences
    ) { goals, preferences ->
        goalRepository.sortGoals(
            goals,
            preferences.goalsSortMode,
            preferences.goalsSortDirection
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val currentGoalsWithProgress = sortedGoalsWithProgress.map { currentGoals ->
        currentGoals.map { goal ->

            val progress = sessionRepository.sectionsForGoal(goal).stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            ).value.sumOf { section ->
                section.duration ?: 0
            }
            GoalWithProgress(goal, progress)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val items = libraryRepository.items.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val sortedItems = combine(
        items,
        itemsSortMode,
        itemsSortDirection
    ) { items, sortMode, sortDirection ->
        libraryRepository.sortItems(
            items = items,
            mode = sortMode,
            direction = sortDirection
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /** Own state flows */

    // Menu
    private val _showSortModeMenu = MutableStateFlow(false)

    // Goal dialog
    private val _goalDialogData = MutableStateFlow<GoalDialogData?>(null)
    private val _goalToEdit = MutableStateFlow<GoalInstanceWithDescriptionWithLibraryItems?>(null)

    private val _isPeriodUnitSelectorExpanded = MutableStateFlow(false)

    private val _isItemSelectorExpanded = MutableStateFlow(false)

    // MultiFAB TODO
    var multiFABState = mutableStateOf(MultiFABState.COLLAPSED)

    // Action mode
    private val _selectedGoals = MutableStateFlow<Set<GoalInstanceWithDescriptionWithLibraryItems>>(emptySet())


    /**
     *  Composing the Ui state
     */
    private val sortMenuUiState = combine(
        goalsSortMode,
        goalsSortDirection,
        _showSortModeMenu
    ) { mode, direction, show ->
        GoalsSortMenuUiState(
            show = show,
            mode = mode,
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

    private val topBarUiState = sortMenuUiState.map {
        GoalsTopBarUiState(
            title = "Goals",
            showBackButton = false,
            sortMenuUiState = it
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsTopBarUiState(
            title = "Goals",
            showBackButton = false,
            sortMenuUiState = sortMenuUiState.value
        )
    )

    private val actionModeUiState = _selectedGoals.map {
        GoalsActionModeUiState(
            isActionMode = it.isNotEmpty(),
            numberOfSelections = it.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsActionModeUiState(
            isActionMode = false,
            numberOfSelections = 0
        )
    )

    private val contentUiState = combine(
        currentGoalsWithProgress,
        _selectedGoals,
    ) { goals, selectedGoals ->
        GoalsContentUiState(
            goalsWithProgress = goals,
            selectedGoals = selectedGoals,
            showHint = goals.isEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsContentUiState(
            goalsWithProgress = emptyList(),
            selectedGoals = emptySet(),
            showHint = true,
        )
    )

    private val dialogUiState = combine(
        _goalDialogData,
        _goalToEdit,
        sortedItems,
        _isPeriodUnitSelectorExpanded,
        _isItemSelectorExpanded
    ) { dialogData, goalToEdit, sortedItems, isPeriodUnitSelectorExpanded, isItemSelectorExpanded ->
        if(dialogData == null) return@combine null // if data == null, the ui state should be null ergo we don't show the dialog
        val confirmButtonEnabled = true //TODO

        GoalsDialogUiState(
            mode = if (goalToEdit == null) DialogMode.ADD else DialogMode.EDIT,
            dialogData = dialogData,
            goalToEdit = goalToEdit,
            isPeriodUnitSelectorExpanded = isPeriodUnitSelectorExpanded,
            items = sortedItems,
            isItemSelectorExpanded = isItemSelectorExpanded,
            confirmButtonEnabled = confirmButtonEnabled,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val goalsUiState = combine(
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
        _goalDialogData.update {
            GoalDialogData(
                target = 0,
                periodInPeriodUnits = 0,
                periodUnit = GoalPeriodUnit.DAY,
                goalType = GoalType.NON_SPECIFIC,
                oneShot = oneShot,
                libraryItems = emptyList(),
            )
        }
        _isPeriodUnitSelectorExpanded.update { false }
        _isItemSelectorExpanded.update { false }
    }

    fun onSortMenuShowChanged(show: Boolean) {
        _showSortModeMenu.update { show }
    }

    fun onSortModeSelected(selection: GoalsSortMode) {
        _showSortModeMenu.update { false }
        viewModelScope.launch {
            userPreferencesRepository.updateGoalsSortMode(selection)
        }
    }

    fun onEditAction() {
        _selectedGoals.value.first().let {goalToEdit ->
            _goalToEdit.update { goalToEdit }
            _goalDialogData.update {
                GoalDialogData(
                    target = goalToEdit.instance.target,
                )
            }
        }
        clearActionMode()
    }

//    fun onDeactivateAction() {
//        viewModelScope.launch {
//            goalRepository.deactivate(_selectedGoals.value.map { it.description.description })
//            clearActionMode()
//        }
//    }
    fun onDeleteAction() {
        viewModelScope.launch {
            goalRepository.archive(_selectedGoals.value.map { it.description.description })
            clearActionMode()
        }
    }

    fun clearActionMode() {
        _selectedGoals.update{ emptySet() }
    }

    fun onGoalClicked(
        goal: GoalInstanceWithDescriptionWithLibraryItems,
        longClick: Boolean = false
    ) {
        if (longClick) {
            _selectedGoals.update { it + goal }
            return
        }

        // Short Click
        if(!actionModeUiState.value.isActionMode) {
            _goalToEdit.update { goal }
            _goalDialogData.update {
                GoalDialogData(
                    target = goal.instance.target,
                )
            }
        } else {
            if(_selectedGoals.value.contains(goal)) {
                _selectedGoals.update { it - goal }
            } else {
                _selectedGoals.update { it + goal }
            }
        }
    }

    fun onTargetChanged(target: Int) {
        _goalDialogData.update { it?.copy(target = target) }
    }

    fun onPeriodChanged(period: Int) {
        _goalDialogData.update { it?.copy(periodInPeriodUnits = period) }
    }

    fun onPeriodUnitChanged(unit: GoalPeriodUnit) {
        _goalDialogData.update { it?.copy(periodUnit = unit) }
    }

    fun onPeriodUnitSelectionExpandedChanged(expanded: Boolean) {
        _isPeriodUnitSelectorExpanded.update { expanded }
    }
    fun onGoalTypeChanged(type: GoalType) {
        _goalDialogData.update { it?.copy(goalType = type) }
    }

    fun onLibraryItemsChanged(items: List<LibraryItem>) {
        _goalDialogData.update { it?.copy(libraryItems = items) }
    }

    fun clearDialog() {
        _goalDialogData.update { null }
        _goalToEdit.update { null }
        _isPeriodUnitSelectorExpanded.update { false }
        _isItemSelectorExpanded.update { false }
    }

    fun onDialogConfirm() {
//        when(dialogUiState.value?.mode) {
//            DialogMode.ADD -> onAddDialogConfirm()
//            DialogMode.EDIT -> onEditDialogConfirm()
//            null -> {}
//        }
        clearDialog()
    }
    fun onEditDialogConfirm() { //TODO
//        viewModelScope.launch {
//            val dialogData = _goalDialogData.value ?: return@launch
//            val goalToEdit = _goalToEdit.value
//            val target = dialogData.targetHours * 60 + dialogData.targetMinutes
//            val period = dialogData.periodInPeriodUnits * dialogData.periodUnit.toMinutes()
//            val goalType = dialogData.goalType
//            val libraryItems = dialogData.libraryItems
//
//            if(goalToEdit == null) {
//                goalRepository.add(
//                    target = target,
//                    period = period,
//                    goalType = goalType,
//                    libraryItems = libraryItems,
//                    oneShot = dialogData.oneShot,
//                )
//            } else {
//                goalRepository.update(
//                    goalToEdit.instance.id,
//                    target = target,
//                    period = period,
//                    goalType = goalType,
//                    libraryItems = libraryItems,
//                    oneShot = dialogData.oneShot,
//                )
//            }
            clearDialog()
//        }
    }

}