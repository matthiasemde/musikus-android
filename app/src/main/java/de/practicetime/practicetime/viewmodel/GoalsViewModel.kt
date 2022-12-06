/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.practicetime.practicetime.dataStore
import de.practicetime.practicetime.database.GoalInstanceWithDescriptionWithLibraryItems
import de.practicetime.practicetime.database.PTDatabase
import de.practicetime.practicetime.repository.GoalsRepository
import de.practicetime.practicetime.repository.LibraryRepository
import de.practicetime.practicetime.repository.UserPreferencesRepository
import de.practicetime.practicetime.shared.MultiFABState
import de.practicetime.practicetime.ui.goals.updateGoals
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.*


class GoalsViewModel(
    application: Application
) : AndroidViewModel(application) {

    /** Database */
    private val database = PTDatabase.getInstance(application)

    /** Repositories */
    private val libraryRepository = LibraryRepository("GoalsViewModel", database)
    private val goalsRepository = GoalsRepository(database)
    private val userPreferencesRepository = UserPreferencesRepository(application.dataStore, application)

    init {
        viewModelScope.launch {
            updateGoals(application)
        }
    }

    private val userPreferences = userPreferencesRepository.userPreferences

    val goals = goalsRepository.goals

    val sortedGoals = goalsRepository.goals.combine(userPreferences) { goals, preferences ->
        goalsRepository.sortGoals(
            goals,
            preferences.goalsSortMode,
            preferences.goalsSortDirection
        )
    }

    val libraryItems = libraryRepository.items

    val sortMode = userPreferences.map { it.goalsSortMode }
    val sortDirection = userPreferences.map { it.goalsSortDirection }

    // Menu
    var showSortModeMenu = mutableStateOf(false)

    // Goal dialog
    val showGoalDialog = mutableStateOf(false)
    val goalDialogRepeat = mutableStateOf(true)

    val showEditGoalDialog = mutableStateOf(false)
    val editableGoal = mutableStateOf<GoalInstanceWithDescriptionWithLibraryItems?>(null)

    // MultiFAB
    var multiFABState = mutableStateOf(MultiFABState.COLLAPSED)

    // Action mode
    var actionMode = mutableStateOf(false)

    val selectedGoalIds = mutableStateListOf<UUID>()

    fun clearActionMode() {
        selectedGoalIds.clear()
        actionMode.value = false
    }
}