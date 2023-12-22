/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.repository

import app.musikus.database.daos.LibraryFolder
import app.musikus.database.daos.LibraryItem
import app.musikus.datastore.ThemeSelections
import app.musikus.datastore.UserPreferences
import app.musikus.utils.GoalsSortMode
import app.musikus.utils.LibraryFolderSortMode
import app.musikus.utils.LibraryItemSortMode
import app.musikus.utils.SortDirection
import app.musikus.utils.SortInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeUserPreferencesRepository : UserPreferencesRepository {

    private val _preferences = MutableStateFlow(UserPreferences(
        theme = ThemeSelections.DEFAULT,
        appIntroDone = false,
        libraryFolderSortMode = LibraryFolderSortMode.DEFAULT,
        libraryFolderSortDirection = SortDirection.DEFAULT,
        libraryItemSortMode = LibraryItemSortMode.DEFAULT,
        libraryItemSortDirection = SortDirection.DEFAULT,
        goalsSortMode = GoalsSortMode.DEFAULT,
        goalsSortDirection = SortDirection.DEFAULT,
        showPausedGoals = true,
    ))
    override val userPreferences: Flow<UserPreferences>
        get() = _preferences
    override val itemSortInfo: Flow<SortInfo<LibraryItem>>
        get() = _preferences.map { preferences ->
            SortInfo(
                mode = preferences.libraryItemSortMode,
                direction = preferences.libraryItemSortDirection,
            )
        }
    override val folderSortInfo: Flow<SortInfo<LibraryFolder>>
        get() = _preferences.map { preferences ->
            SortInfo(
                mode = preferences.libraryFolderSortMode,
                direction = preferences.libraryFolderSortDirection,
            )
        }

    override suspend fun updateTheme(theme: ThemeSelections) {
        _preferences.update {
            it.copy(theme = theme)
        }
    }

    override suspend fun updateLibraryItemSortInfo(sortInfo: SortInfo<LibraryItem>) {
        _preferences.update {
            it.copy(
                libraryItemSortMode = sortInfo.mode as LibraryItemSortMode,
                libraryItemSortDirection = sortInfo.direction,
            )
        }
    }

    override suspend fun updateLibraryFolderSortInfo(sortInfo: SortInfo<LibraryFolder>) {
        _preferences.update {
            it.copy(
                libraryFolderSortMode = sortInfo.mode as LibraryFolderSortMode,
                libraryFolderSortDirection = sortInfo.direction,
            )
        }
    }

    override suspend fun updateGoalsSortMode(mode: GoalsSortMode) {
        _preferences.update {
            it.copy(goalsSortMode = mode)
        }
    }

    override suspend fun updateShowPausedGoals(value: Boolean) {
        _preferences.update {
            it.copy(showPausedGoals = value)
        }
    }

    override suspend fun updateAppIntroDone(value: Boolean) {
        _preferences.update {
            it.copy(appIntroDone = value)
        }
    }
}