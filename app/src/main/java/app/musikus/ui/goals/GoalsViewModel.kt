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
import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.daos.GoalDescription
import app.musikus.database.daos.LibraryItem
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalInstanceCreationAttributes
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.database.entities.GoalType
import app.musikus.repository.GoalRepository
import app.musikus.repository.LibraryRepository
import app.musikus.repository.SessionRepository
import app.musikus.repository.UserPreferencesRepository
import app.musikus.shared.TopBarUiState
import app.musikus.ui.library.DialogMode
import app.musikus.usecase.goals.GoalsUseCases
import app.musikus.utils.GoalsSortMode
import app.musikus.utils.LibraryItemSortMode
import app.musikus.utils.SortDirection
import app.musikus.utils.TimeProvider
import app.musikus.utils.sorted
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class GoalWithProgress(
    val goal: GoalInstanceWithDescriptionWithLibraryItems,
    val progress: Duration,
)

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

data class GoalsOverflowMenuUiState(
    val showPausedGoals: Boolean,
)

data class GoalsTopBarUiState(
    override val title: String,
    override val showBackButton: Boolean,
    val sortMenuUiState: GoalsSortMenuUiState,
    val overflowMenuUiState: GoalsOverflowMenuUiState,
) : TopBarUiState

data class GoalsActionModeUiState(
    val isActionMode: Boolean,
    val numberOfSelections: Int,
    val showEditAction: Boolean,
    val showPauseAction: Boolean,
    val showUnpauseAction: Boolean,
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
    private val userPreferencesRepository : UserPreferencesRepository,
    libraryRepository: LibraryRepository,
    private val goalRepository : GoalRepository,
    private val goalsUseCases: GoalsUseCases,
    sessionRepository : SessionRepository,
) : ViewModel() {

    private var _goalsCache: List<GoalDescription> = emptyList()


    init {
        viewModelScope.launch {
            goalsUseCases.update()
        }
    }

    /** Imported flows */

    private val userPreferences = userPreferencesRepository.userPreferences

    private val showPausedGoals = userPreferences.map {
        it.showPausedGoals
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    private val itemsSortInfo = userPreferences.map {
        it.libraryItemSortMode to it.libraryItemSortDirection
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryItemSortMode.DEFAULT to SortDirection.DEFAULT
    )

    private val goalsSortInfo = userPreferences.map {
        it.goalsSortMode to it.goalsSortDirection
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsSortMode.DEFAULT to SortDirection.DEFAULT
    )

    private val currentGoals = goalRepository.currentGoals.stateIn(
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
        itemsSortInfo
    ) { items, (sortMode, sortDirection) ->
        items.sorted(
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
    private val _dialogData = MutableStateFlow<GoalDialogData?>(null)
    private val _goalToEdit = MutableStateFlow<GoalInstanceWithDescriptionWithLibraryItems?>(null)

    // Action mode
    private val _selectedGoals = MutableStateFlow<Set<GoalInstanceWithDescriptionWithLibraryItems>>(emptySet())

    /** Combining imported and own state flows */
    private val filteredGoals = combine(
        currentGoals,
        showPausedGoals
    ) { goals, showPausedGoals ->
        goals.filter { goal ->
            showPausedGoals || !goal.description.description.paused
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val sortedGoals = combine(
        filteredGoals,
        goalsSortInfo
    ) { goals, (sortMode, sortDirection) ->
        goals.sorted(
            sortMode,
            sortDirection
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val goalProgress = filteredGoals.flatMapLatest { goals ->
        val sections = goals.map { goal ->
            sessionRepository.sectionsForGoal(goal).map { sections ->
                goal to sections
            }
        }

        combine(sections) { combinedGoalsWithSections ->
            combinedGoalsWithSections.associate { (goal, sections) ->
                goal.instance.id to sections.sumOf { section ->
                    section.duration.inWholeSeconds
                }.seconds
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )


    /**
     *  Composing the Ui state
     */
    private val sortMenuUiState = combine(
        goalsSortInfo,
        _showSortModeMenu
    ) { (mode, direction), show ->
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

    private val overflowMenuUiState = showPausedGoals.map {
        GoalsOverflowMenuUiState(
            showPausedGoals = it
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsOverflowMenuUiState(
            showPausedGoals = showPausedGoals.value
        )
    )

    private val topBarUiState = combine(
        sortMenuUiState,
        overflowMenuUiState,
    ) { sortMenuUiState, overflowMenuUiState ->
        GoalsTopBarUiState(
            title = "Goals",
            showBackButton = false,
            sortMenuUiState = sortMenuUiState,
            overflowMenuUiState = overflowMenuUiState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsTopBarUiState(
            title = "Goals",
            showBackButton = false,
            sortMenuUiState = sortMenuUiState.value,
            overflowMenuUiState = overflowMenuUiState.value,
        )
    )

    private val actionModeUiState = _selectedGoals.map {
        GoalsActionModeUiState(
            isActionMode = it.isNotEmpty(),
            numberOfSelections = it.size,
            showEditAction = it.size == 1 && it.none { goal -> goal.description.description.paused },
            showPauseAction = it.any { goal -> !goal.description.description.paused },
            showUnpauseAction = it.any { goal -> goal.description.description.paused },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsActionModeUiState(
            isActionMode = false,
            numberOfSelections = 0,
            showEditAction = false,
            showPauseAction = false,
            showUnpauseAction = false,
        )
    )

    private val contentUiState = combine(
        sortedGoals,
        goalProgress,
        _selectedGoals,
    ) { sortedGoals, goalProgress, selectedGoals ->
        val goalsWithProgress = sortedGoals.map { goal ->
            GoalWithProgress(
                goal = goal,
                progress = if (goal.description.description.paused) 0.seconds else
                    goalProgress[goal.instance.id] ?: 0.seconds,
            )
        }
        GoalsContentUiState(
            goalsWithProgress = goalsWithProgress,
            selectedGoals = selectedGoals,
            showHint = goalsWithProgress.isEmpty(),
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
        _dialogData,
        _goalToEdit,
        sortedItems,
    ) { dialogData, goalToEdit, sortedItems ->
        if(dialogData == null) return@combine null // if data == null, the ui state should be null ergo we don't show the dialog

        GoalsDialogUiState(
            mode = if (goalToEdit == null) DialogMode.ADD else DialogMode.EDIT,
            dialogData = dialogData,
            goalToEdit = goalToEdit,
            libraryItems = sortedItems,
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
                selectedLibraryItems = sortedItems.value.firstOrNull()?.let {
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
            goalsUseCases.selectSortMode(selection)
        }
    }

    fun onEditAction() {
        _selectedGoals.value.first().let {goalToEdit ->
            _goalToEdit.update { goalToEdit }
            _dialogData.update {
                GoalDialogData(
                    target = goalToEdit.instance.target,
                )
            }
        }
        clearActionMode()
    }

    fun onPauseAction() {
        viewModelScope.launch {
            goalsUseCases.pause(_selectedGoals.value.toList().map {
                it.description.description.id
            })
            clearActionMode()
        }
    }

    fun onUnpauseAction() {
        viewModelScope.launch {
            goalsUseCases.unpause(_selectedGoals.value.toList().map {
                it.description.description.id
            })
            clearActionMode()
        }
    }
    fun onPausedGoalsChanged(newValue: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateShowPausedGoals(newValue)
        }
    }

    fun onArchiveAction() {
        viewModelScope.launch {
            _goalsCache = _selectedGoals.value.map { it.description.description }
            goalRepository.archive(_goalsCache)
            clearActionMode()
        }
    }

    fun onUndoArchiveAction() {
        viewModelScope.launch {
            goalRepository.unarchive(_goalsCache)
        }
    }

    fun onDeleteAction() {
        viewModelScope.launch {
            _goalsCache = _selectedGoals.value.map { it.description.description }
            goalRepository.delete(_goalsCache.map { it.id })
            clearActionMode()
        }
    }

    fun onRestoreAction() {
        viewModelScope.launch {
            goalRepository.restore(_goalsCache.map { it.id })
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
            if(!goal.description.description.paused) {
                _goalToEdit.update { goal }
                _dialogData.update {
                    GoalDialogData(
                        target = goal.instance.target,
                    )
                }
            }
        } else {
            if(_selectedGoals.value.contains(goal)) {
                _selectedGoals.update { it - goal }
            } else {
                _selectedGoals.update { it + goal }
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
        _goalToEdit.update { null }
    }

    fun onDialogConfirm() {
        dialogUiState.value?.let { uiState ->
            val dialogData = uiState.dialogData
            viewModelScope.launch {
                if (uiState.goalToEdit == null) {
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
                    goalRepository.editGoalTarget(
                        goal = uiState.goalToEdit.instance,
                        newTarget = dialogData.target
                    )
                }
                clearDialog()
            }
        }
    }
}
