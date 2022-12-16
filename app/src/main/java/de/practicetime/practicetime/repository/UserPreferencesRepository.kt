/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import de.practicetime.practicetime.datastore.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object PreferenceKeys {
    val THEME = stringPreferencesKey("theme")
    val APPINTRO_DONE = booleanPreferencesKey("appintro_done")
    val LIBRARY_FOLDER_SORT_MODE = stringPreferencesKey("library_folder_sort_mode")
    val LIBRARY_FOLDER_SORT_DIRECTION = stringPreferencesKey("library_folder_sort_direction")
    val LIBRARY_ITEM_SORT_MODE = stringPreferencesKey("library_item_sort_mode")
    val LIBRARY_ITEM_SORT_DIRECTION = stringPreferencesKey("library_item_sort_direction")
    val GOALS_SORT_MODE = stringPreferencesKey("goals_sort_mode")
    val GOALS_SORT_DIRECTION = stringPreferencesKey("goals_sort_direction")
}

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val userPreferences = dataStore.data.map { preferences ->
        UserPreferences(
            theme = ThemeSelections.valueOrDefault(preferences[PreferenceKeys.THEME]),

            appIntroDone = preferences[PreferenceKeys.APPINTRO_DONE] ?: false,

            libraryFolderSortMode = LibraryFolderSortMode.valueOrDefault(preferences[PreferenceKeys.LIBRARY_FOLDER_SORT_MODE]),
            libraryFolderSortDirection = SortDirection.valueOrDefault(preferences[PreferenceKeys.LIBRARY_FOLDER_SORT_DIRECTION]),

            libraryItemSortMode = LibraryItemSortMode.valueOrDefault(preferences[PreferenceKeys.LIBRARY_ITEM_SORT_MODE]),
            libraryItemSortDirection = SortDirection.valueOrDefault(preferences[PreferenceKeys.LIBRARY_ITEM_SORT_DIRECTION]),

            goalsSortMode = GoalsSortMode.valueOrDefault(preferences[PreferenceKeys.GOALS_SORT_MODE]),
            goalsSortDirection = SortDirection.valueOrDefault(preferences[PreferenceKeys.GOALS_SORT_DIRECTION])
        )
    }

    suspend fun updateTheme(theme: ThemeSelections) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME] = theme.name
        }
    }

    suspend fun updateLibraryFolderSortMode(mode: LibraryFolderSortMode) {
        userPreferences.map { preferences ->
            Pair(preferences.libraryFolderSortMode, preferences.libraryFolderSortDirection)
        }.first().let { (currentMode, currentDirection) ->
            if (currentMode != mode) {
                dataStore.edit { preferences ->
                    preferences[PreferenceKeys.LIBRARY_FOLDER_SORT_MODE] = mode.name
                    preferences[PreferenceKeys.LIBRARY_FOLDER_SORT_DIRECTION] =
                        SortDirection.ASCENDING.name
                }
            } else {
                dataStore.edit { preferences ->
                    preferences[PreferenceKeys.LIBRARY_FOLDER_SORT_DIRECTION] =
                        currentDirection.toggle().name
                }
            }
        }
    }

    suspend fun updateLibraryItemSortMode(mode: LibraryItemSortMode) {
        userPreferences.map { preferences ->
            Pair(preferences.libraryItemSortMode, preferences.libraryItemSortDirection)
        }.first().let { (currentMode, currentDirection) ->
            if (currentMode != mode) {
                dataStore.edit { preferences ->
                    preferences[PreferenceKeys.LIBRARY_ITEM_SORT_MODE] = mode.name
                    preferences[PreferenceKeys.LIBRARY_ITEM_SORT_DIRECTION] =
                        SortDirection.ASCENDING.name
                }
            } else {
                dataStore.edit { preferences ->
                    preferences[PreferenceKeys.LIBRARY_ITEM_SORT_DIRECTION] =
                        currentDirection.toggle().name
                }
            }
        }
    }

    suspend fun updateGoalsSortMode(mode: GoalsSortMode) {
        userPreferences.map { preferences ->
            Pair(preferences.goalsSortMode, preferences.goalsSortDirection)
        }.first().let { (currentMode, currentDirection) ->
            if (currentMode != mode) {
                dataStore.edit { preferences ->
                    preferences[PreferenceKeys.GOALS_SORT_MODE] = mode.name
                    preferences[PreferenceKeys.GOALS_SORT_DIRECTION] =
                        SortDirection.ASCENDING.name
                }
            } else {
                dataStore.edit { preferences ->
                    preferences[PreferenceKeys.GOALS_SORT_DIRECTION] =
                        currentDirection.toggle().name
                }
            }
        }
    }
}