/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.repository

import app.musikus.library.data.daos.LibraryFolder
import app.musikus.library.data.daos.LibraryItem
import app.musikus.settings.data.UserPreferencesRepository
import app.musikus.settings.domain.ColorSchemeSelections
import app.musikus.settings.domain.ThemeSelections
import app.musikus.settings.domain.UserPreferences
import app.musikus.ui.activesession.metronome.MetronomeSettings
import app.musikus.core.domain.GoalSortInfo
import app.musikus.core.domain.GoalsSortMode
import app.musikus.core.domain.LibraryFolderSortMode
import app.musikus.core.domain.LibraryItemSortMode
import app.musikus.core.domain.SortDirection
import app.musikus.core.domain.SortInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeUserPreferencesRepository : UserPreferencesRepository {

    private val _preferences = MutableStateFlow(
        UserPreferences(
        theme = ThemeSelections.DEFAULT,
        colorScheme = ColorSchemeSelections.DEFAULT,
        appIntroDone = false,
        libraryFolderSortMode = LibraryFolderSortMode.DEFAULT,
        libraryFolderSortDirection = SortDirection.DEFAULT,
        libraryItemSortMode = LibraryItemSortMode.DEFAULT,
        libraryItemSortDirection = SortDirection.DEFAULT,
        goalsSortMode = GoalsSortMode.DEFAULT,
        goalsSortDirection = SortDirection.DEFAULT,
        showPausedGoals = true,
        metronomeSettings = MetronomeSettings.DEFAULT
    )
    )
    override val theme: Flow<ThemeSelections>
        get() = _preferences.map { it.theme }

    override val colorScheme: Flow<ColorSchemeSelections>
        get() = _preferences.map { it.colorScheme }

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

    override val goalSortInfo: Flow<GoalSortInfo>
        get() = _preferences.map { preferences ->
            SortInfo(
                mode = preferences.goalsSortMode,
                direction = preferences.goalsSortDirection,
            )
        }
    override val metronomeSettings: Flow<MetronomeSettings>
        get() = _preferences.map { it.metronomeSettings }

    override suspend fun updateTheme(theme: ThemeSelections) {
        _preferences.update {
            it.copy(theme = theme)
        }
    }

    override suspend fun updateColorScheme(colorScheme: ColorSchemeSelections) {
        _preferences.update {
            it.copy(colorScheme = colorScheme)
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

    override suspend fun updateGoalSortInfo(sortInfo: GoalSortInfo) {
        _preferences.update {
            it.copy(
                goalsSortMode = sortInfo.mode as GoalsSortMode,
                goalsSortDirection = sortInfo.direction,
            )
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

    override suspend fun updateMetronomeSettings(settings: MetronomeSettings) {
        _preferences.update {
            it.copy(metronomeSettings = settings)
        }
    }
}