/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.settings.domain

import app.musikus.ui.activesession.metronome.MetronomeSettings
import app.musikus.core.domain.GoalsSortMode
import app.musikus.core.domain.LibraryFolderSortMode
import app.musikus.core.domain.LibraryItemSortMode
import app.musikus.core.domain.SortDirection

interface EnumWithLabel {
    val label: String
}

interface EnumWithDescription {
    val description: String
}

enum class ThemeSelections : EnumWithLabel {
    SYSTEM {
       override val label = "System default"
    },
    DAY {
        override val label = "Light"
    },
    NIGHT {
        override val label = "Dark"
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
        override val label = "Musikus"
        override val description = "A fresh new look"
    },
    LEGACY {
        override val label = "PracticeTime"
        override val description = "Reminds you of an old friend"
    },
    DYNAMIC {
        override val label = "Dynamic"
        override val description = "The color scheme follows your system theme. If it looks bad, it's on you"
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