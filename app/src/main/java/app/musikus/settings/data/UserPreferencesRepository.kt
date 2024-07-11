/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.musikus.database.daos.LibraryFolder
import app.musikus.database.daos.LibraryItem
import app.musikus.settings.domain.ColorSchemeSelections
import app.musikus.settings.domain.ThemeSelections
import app.musikus.settings.domain.UserPreferences
import app.musikus.ui.activesession.metronome.MetronomeSettings
import app.musikus.utils.GoalSortInfo
import app.musikus.utils.GoalsSortMode
import app.musikus.utils.LibraryFolderSortMode
import app.musikus.utils.LibraryItemSortMode
import app.musikus.utils.SortDirection
import app.musikus.utils.SortInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object PreferenceKeys {
    val THEME = stringPreferencesKey("theme")
    val COLOR_SCHEME = stringPreferencesKey("color_scheme")
    val APPINTRO_DONE = booleanPreferencesKey("appintro_done")

    val LIBRARY_FOLDER_SORT_MODE = stringPreferencesKey("library_folder_sort_mode")
    val LIBRARY_FOLDER_SORT_DIRECTION = stringPreferencesKey("library_folder_sort_direction")

    val LIBRARY_ITEM_SORT_MODE = stringPreferencesKey("library_item_sort_mode")
    val LIBRARY_ITEM_SORT_DIRECTION = stringPreferencesKey("library_item_sort_direction")

    val GOALS_SORT_MODE = stringPreferencesKey("goals_sort_mode")
    val GOALS_SORT_DIRECTION = stringPreferencesKey("goals_sort_direction")

    val SHOW_PAUSED_GOALS = booleanPreferencesKey("show_paused_goals")

    val METRONOME_BPM = intPreferencesKey("metronome_bpm")
    val METRONOME_BEATS_PER_BAR = intPreferencesKey("metronome_beats_per_bar")
    val METRONOME_CLICKS_PER_BEAT = intPreferencesKey("metronome_clicks_per_beat")
}

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

class UserPreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {
    private val userPreferences = dataStore.data.map { preferences ->
        UserPreferences(
            theme = ThemeSelections.valueOrDefault(preferences[PreferenceKeys.THEME]),
            colorScheme = ColorSchemeSelections.valueOrDefault(preferences[PreferenceKeys.COLOR_SCHEME]),

            appIntroDone = preferences[PreferenceKeys.APPINTRO_DONE] ?: false,

            libraryFolderSortMode = LibraryFolderSortMode.valueOrDefault(preferences[PreferenceKeys.LIBRARY_FOLDER_SORT_MODE]),
            libraryFolderSortDirection = SortDirection.valueOrDefault(preferences[PreferenceKeys.LIBRARY_FOLDER_SORT_DIRECTION]),

            libraryItemSortMode = LibraryItemSortMode.valueOrDefault(preferences[PreferenceKeys.LIBRARY_ITEM_SORT_MODE]),
            libraryItemSortDirection = SortDirection.valueOrDefault(preferences[PreferenceKeys.LIBRARY_ITEM_SORT_DIRECTION]),

            goalsSortMode = GoalsSortMode.valueOrDefault(preferences[PreferenceKeys.GOALS_SORT_MODE]),
            goalsSortDirection = SortDirection.valueOrDefault(preferences[PreferenceKeys.GOALS_SORT_DIRECTION]),

            showPausedGoals =  preferences[PreferenceKeys.SHOW_PAUSED_GOALS] ?: true,

            metronomeSettings = MetronomeSettings(
                bpm = preferences[PreferenceKeys.METRONOME_BPM] ?: MetronomeSettings.DEFAULT.bpm,
                beatsPerBar = preferences[PreferenceKeys.METRONOME_BEATS_PER_BAR] ?: MetronomeSettings.DEFAULT.beatsPerBar,
                clicksPerBeat = preferences[PreferenceKeys.METRONOME_CLICKS_PER_BEAT] ?: MetronomeSettings.DEFAULT.clicksPerBeat
            )
        )
    }

    override val theme: Flow<ThemeSelections> = userPreferences.map { preferences ->
        preferences.theme
    }

    override val colorScheme: Flow<ColorSchemeSelections> = userPreferences.map { preferences ->
        preferences.colorScheme
    }

    override val itemSortInfo : Flow<SortInfo<LibraryItem>> =
        userPreferences.map { preferences ->
            SortInfo(
                mode = preferences.libraryItemSortMode,
                direction = preferences.libraryItemSortDirection
            )
        }

    override val folderSortInfo : Flow<SortInfo<LibraryFolder>> =
        userPreferences.map { preferences ->
            SortInfo(
                mode = preferences.libraryFolderSortMode,
                direction = preferences.libraryFolderSortDirection
            )
        }

    override val goalSortInfo : Flow<GoalSortInfo> =
        userPreferences.map { preferences ->
            SortInfo(
                mode = preferences.goalsSortMode,
                direction = preferences.goalsSortDirection
            )
        }

    override val metronomeSettings: Flow<MetronomeSettings> = userPreferences.map { preferences ->
        preferences.metronomeSettings
    }

    /** Mutators */
    override suspend fun updateTheme(theme: ThemeSelections) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME] = theme.name
        }
    }

    override suspend fun updateColorScheme(colorScheme: ColorSchemeSelections) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.COLOR_SCHEME] = colorScheme.name
        }
    }

    override suspend fun updateLibraryItemSortInfo(sortInfo: SortInfo<LibraryItem>) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.LIBRARY_ITEM_SORT_MODE] = sortInfo.mode.name
            preferences[PreferenceKeys.LIBRARY_ITEM_SORT_DIRECTION] = sortInfo.direction.name
        }
    }

    override suspend fun updateLibraryFolderSortInfo(sortInfo: SortInfo<LibraryFolder>) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.LIBRARY_FOLDER_SORT_MODE] = sortInfo.mode.name
            preferences[PreferenceKeys.LIBRARY_FOLDER_SORT_DIRECTION] = sortInfo.direction.name
        }
    }


    override suspend fun updateGoalSortInfo(sortInfo: GoalSortInfo) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.GOALS_SORT_MODE] = sortInfo.mode.name
            preferences[PreferenceKeys.GOALS_SORT_DIRECTION] = sortInfo.direction.name
        }
    }

    override suspend fun updateShowPausedGoals(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SHOW_PAUSED_GOALS] = value
        }
    }

    override suspend fun updateAppIntroDone(value: Boolean) {
        TODO("Not yet implemented")
    }

    override suspend fun updateMetronomeSettings(settings: MetronomeSettings) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.METRONOME_BPM] = settings.bpm
            preferences[PreferenceKeys.METRONOME_BEATS_PER_BAR] = settings.beatsPerBar
            preferences[PreferenceKeys.METRONOME_CLICKS_PER_BEAT] = settings.clicksPerBeat
        }
    }
}