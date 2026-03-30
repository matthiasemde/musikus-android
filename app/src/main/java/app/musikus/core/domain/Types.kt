/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.core.domain

import app.musikus.goals.data.GoalSortInfo
import app.musikus.goals.data.GoalsSortMode
import app.musikus.library.data.LibraryFolderSortMode
import app.musikus.library.data.LibraryItemSortMode
import app.musikus.library.data.daos.LibraryFolder
import app.musikus.library.data.daos.LibraryItem
import app.musikus.menu.domain.ColorSchemeSelections
import app.musikus.menu.domain.ThemeSelections
import app.musikus.metronome.presentation.MetronomeSettings
import kotlinx.coroutines.flow.Flow

const val USER_PREFERENCES_NAME = "user_preferences"

data class UserPreferences(
    val theme: ThemeSelections,
    val colorScheme: ColorSchemeSelections,

    val appIntroDone: Boolean,

    // Sorting
    val libraryFolderSortMode: LibraryFolderSortMode,
    val libraryFolderSortDirection: SortDirection,

    val libraryItemSortMode: LibraryItemSortMode,
    val libraryItemSortDirection: SortDirection,

    val goalsSortMode: GoalsSortMode,
    val goalsSortDirection: SortDirection,

    // Show paused goals
    val showPausedGoals: Boolean,

    // Metronome
    val metronomeSettings: MetronomeSettings,

    // Announcement message
    val idOfLastAnnouncementSeen: Int,

    // App intro dialogs
    val appIntroDialogIndices: Map<AppIntroDialogScreens, Int>
)

interface UserPreferencesRepository {

    val theme: Flow<ThemeSelections>
    val colorScheme: Flow<ColorSchemeSelections>

    val itemSortInfo: Flow<SortInfo<LibraryItem>>
    val folderSortInfo: Flow<SortInfo<LibraryFolder>>
    val goalSortInfo: Flow<GoalSortInfo>

    val metronomeSettings: Flow<MetronomeSettings>

    val idOfLastAnnouncementSeen: Flow<Int>

    val appIntroDialogIndices: Flow<Map<AppIntroDialogScreens, Int>>

    /** Mutators */
    suspend fun updateTheme(theme: ThemeSelections)
    suspend fun updateColorScheme(colorScheme: ColorSchemeSelections)

    suspend fun updateLibraryItemSortInfo(sortInfo: SortInfo<LibraryItem>)
    suspend fun updateLibraryFolderSortInfo(sortInfo: SortInfo<LibraryFolder>)

    suspend fun updateGoalSortInfo(sortInfo: GoalSortInfo)
    suspend fun updateShowPausedGoals(value: Boolean)

    suspend fun updateAppIntroDone(value: Boolean)

    suspend fun updateMetronomeSettings(settings: MetronomeSettings)

    suspend fun updateIdOfLastAnnouncementSeen(id: Int)

    suspend fun updateAppIntroDialogIndex(screen: AppIntroDialogScreens, index: Int)
}
