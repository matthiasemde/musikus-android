/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.settings.data

import app.musikus.core.domain.GoalSortInfo
import app.musikus.core.domain.SortInfo
import app.musikus.library.data.daos.LibraryFolder
import app.musikus.library.data.daos.LibraryItem
import app.musikus.settings.domain.ColorSchemeSelections
import app.musikus.settings.domain.ThemeSelections
import app.musikus.ui.activesession.metronome.MetronomeSettings
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {

    val theme: Flow<ThemeSelections>
    val colorScheme: Flow<ColorSchemeSelections>

    val itemSortInfo: Flow<SortInfo<LibraryItem>>
    val folderSortInfo: Flow<SortInfo<LibraryFolder>>
    val goalSortInfo: Flow<GoalSortInfo>

    val metronomeSettings: Flow<MetronomeSettings>

    /** Mutators */
    suspend fun updateTheme(theme: ThemeSelections)
    suspend fun updateColorScheme(colorScheme: ColorSchemeSelections)

    suspend fun updateLibraryItemSortInfo(sortInfo: SortInfo<LibraryItem>)
    suspend fun updateLibraryFolderSortInfo(sortInfo: SortInfo<LibraryFolder>)

    suspend fun updateGoalSortInfo(sortInfo: GoalSortInfo)
    suspend fun updateShowPausedGoals(value: Boolean)

    suspend fun updateAppIntroDone(value: Boolean)

    suspend fun updateMetronomeSettings(settings: MetronomeSettings)
}