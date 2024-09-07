/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.settings.domain

import app.musikus.R
import app.musikus.core.data.EnumWithDescription
import app.musikus.core.data.EnumWithLabel
import app.musikus.core.domain.SortDirection
import app.musikus.core.domain.SortInfo
import app.musikus.core.presentation.utils.UiText
import app.musikus.goals.data.GoalSortInfo
import app.musikus.goals.data.GoalsSortMode
import app.musikus.library.data.LibraryFolderSortMode
import app.musikus.library.data.LibraryItemSortMode
import app.musikus.library.data.daos.LibraryFolder
import app.musikus.library.data.daos.LibraryItem
import app.musikus.metronome.presentation.MetronomeSettings
import kotlinx.coroutines.flow.Flow

const val USER_PREFERENCES_NAME = "user_preferences"

enum class ThemeSelections : EnumWithLabel {
    SYSTEM {
       override val label = UiText.StringResource(R.string.settings_appearance_theme_options_system)
    },
    DAY {
        override val label = UiText.StringResource(R.string.settings_appearance_theme_options_day)
    },
    NIGHT {
        override val label = UiText.StringResource(R.string.settings_appearance_theme_options_night)
    };

    companion object {
        val DEFAULT = SYSTEM

        fun valueOrDefault(string: String?) = try {
            valueOf(string ?: "")
        } catch (e: Exception) {
            DEFAULT
        }
    }
}

enum class ColorSchemeSelections : EnumWithLabel, EnumWithDescription {
    MUSIKUS {
        override val label = UiText.StringResource(R.string.settings_appearance_color_scheme_options_musikus_title)
        override val description = UiText.StringResource(R.string.settings_appearance_color_scheme_options_musikus_text)
    },
    LEGACY {
        override val label = UiText.StringResource(R.string.settings_appearance_color_scheme_options_legacy_title)
        override val description = UiText.StringResource(R.string.settings_appearance_color_scheme_options_legacy_text)
    },
    DYNAMIC {
        override val label = UiText.StringResource(R.string.settings_appearance_color_scheme_options_dynamic_title)
        override val description = UiText.StringResource(R.string.settings_appearance_color_scheme_options_dynamic_text)
    };

    companion object {
        val DEFAULT = MUSIKUS

        fun valueOrDefault(string: String?) = try {
            valueOf(string ?: "")
        } catch (e: Exception) {
            DEFAULT
        }
    }
}


data class UserPreferences (
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
    val metronomeSettings: MetronomeSettings
)


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