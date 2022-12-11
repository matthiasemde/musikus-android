/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.datastore

enum class SortDirection {
    ASCENDING,
    DESCENDING;

    fun toggle() = when (this) {
        ASCENDING -> DESCENDING
        DESCENDING -> ASCENDING
    }

    companion object {
        val DEFAULT = ASCENDING

        fun fromBoolean(boolean: Boolean) = if (boolean) ASCENDING else DESCENDING

        fun valueOrDefault(string: String?) = try {
            valueOf(string ?: "")
        } catch (e: Exception) {
            DEFAULT
        }
    }
}

enum class ThemeSelections {
    SYSTEM,
    DAY,
    NIGHT;

    companion object {
        val DEFAULT = SYSTEM

        fun valueOrDefault(string: String?) = try {
            valueOf(string ?: "")
        } catch (e: Exception) {
            DEFAULT
        }
    }
}

enum class GoalsSortMode {
    DATE_ADDED,
    TARGET,
    PERIOD,
    CUSTOM;

    companion object {
        val DEFAULT = DATE_ADDED

        fun toString(sortMode: GoalsSortMode): String {
            return when (sortMode) {
                DATE_ADDED -> "Date added"
                TARGET -> "Target"
                PERIOD -> "Period"
                CUSTOM -> "Custom"
            }
        }

        fun valueOrDefault(string: String?) = try {
            valueOf(string ?: "")
        } catch (e: Exception) {
            DEFAULT
        }
    }
}

enum class LibraryItemSortMode {
    DATE_ADDED,
    LAST_MODIFIED,
    NAME,
    COLOR,
    CUSTOM;

    companion object {
        val DEFAULT = DATE_ADDED

        fun toString(sortMode: LibraryItemSortMode) = when (sortMode) {
            DATE_ADDED -> "Date added"
            LAST_MODIFIED -> "Last modified"
            NAME -> "Name"
            COLOR -> "Color"
            CUSTOM -> "Custom"
        }

        fun valueOrDefault(string: String?) = try {
            valueOf(string ?: "")
        } catch (e: Exception) {
            DEFAULT
        }
    }
}

enum class LibraryFolderSortMode {
    DATE_ADDED,
    LAST_MODIFIED,
    NAME,
    CUSTOM;

    companion object {
        val DEFAULT = DATE_ADDED

        fun toString(sortMode: LibraryFolderSortMode) = when (sortMode) {
            DATE_ADDED -> "Date added"
            LAST_MODIFIED -> "Last modified"
            NAME -> "Name"
            CUSTOM -> "Custom"
        }

        fun valueOrDefault(string: String?) = try {
            valueOf(string ?: "")
        } catch (e: Exception) {
            DEFAULT
        }
    }
}

data class UserPreferences (
    val theme: ThemeSelections,

    val appIntroDone: Boolean,

    // Sorting
    val libraryFolderSortMode: LibraryFolderSortMode,
    val libraryFolderSortDirection: SortDirection,

    val libraryItemSortMode: LibraryItemSortMode,
    val libraryItemSortDirection: SortDirection,

    val goalsSortMode: GoalsSortMode,
    val goalsSortDirection: SortDirection,
)