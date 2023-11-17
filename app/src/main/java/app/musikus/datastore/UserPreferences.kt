/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.datastore

import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.daos.LibraryFolder
import app.musikus.database.daos.LibraryItem

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

fun List<GoalInstanceWithDescriptionWithLibraryItems>.sorted(
    mode: GoalsSortMode,
    direction: SortDirection
) = when (direction) {
    SortDirection.ASCENDING -> {
        when (mode) {
            GoalsSortMode.DATE_ADDED -> this.sortedBy { it.description.description.createdAt }
            GoalsSortMode.TARGET -> this.sortedBy { it.instance.target }
            GoalsSortMode.PERIOD -> this.sortedBy { it.instance.periodInSeconds }
            GoalsSortMode.CUSTOM -> this // TODO
        }
    }
    SortDirection.DESCENDING -> {
        when (mode) {
            GoalsSortMode.DATE_ADDED -> this.sortedByDescending { it.description.description.createdAt }
            GoalsSortMode.TARGET -> this.sortedByDescending { it.instance.target }
            GoalsSortMode.PERIOD -> this.sortedByDescending { it.instance.periodInSeconds }
            GoalsSortMode.CUSTOM -> this // TODO()
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

        // TODO move to override (check other enums as well)
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

fun List<LibraryItem>.sorted(
    mode: LibraryItemSortMode,
    direction: SortDirection
) = when(direction) {
    SortDirection.ASCENDING -> {
        when (mode) {
            LibraryItemSortMode.DATE_ADDED -> this.sortedBy { it.createdAt }
            LibraryItemSortMode.LAST_MODIFIED -> this.sortedBy { it.modifiedAt }
            LibraryItemSortMode.NAME -> this.sortedBy { it.name }
            LibraryItemSortMode.COLOR -> this.sortedBy { it.colorIndex }
            LibraryItemSortMode.CUSTOM -> this // TODO
        }
    }
    SortDirection.DESCENDING -> {
        when (mode) {
            LibraryItemSortMode.DATE_ADDED -> this.sortedByDescending { it.createdAt }
            LibraryItemSortMode.LAST_MODIFIED -> this.sortedByDescending { it.modifiedAt }
            LibraryItemSortMode.NAME -> this.sortedByDescending { it.name }
            LibraryItemSortMode.COLOR -> this.sortedByDescending { it.colorIndex }
            LibraryItemSortMode.CUSTOM -> this // TODO
        }
    }
}

fun MutableList<LibraryItem>.sort(
    mode: LibraryItemSortMode,
    direction: SortDirection
) {
    when(direction) {
        SortDirection.ASCENDING -> {
            when (mode) {
                LibraryItemSortMode.DATE_ADDED -> this.sortBy { it.createdAt }
                LibraryItemSortMode.LAST_MODIFIED -> this.sortBy { it.modifiedAt }
                LibraryItemSortMode.NAME -> this.sortBy { it.name }
                LibraryItemSortMode.COLOR -> this.sortBy { it.colorIndex }
                LibraryItemSortMode.CUSTOM -> {} // TODO
            }
        }
        SortDirection.DESCENDING -> {
            when (mode) {
                LibraryItemSortMode.DATE_ADDED -> this.sortByDescending { it.createdAt }
                LibraryItemSortMode.LAST_MODIFIED -> this.sortByDescending { it.modifiedAt }
                LibraryItemSortMode.NAME -> this.sortByDescending { it.name }
                LibraryItemSortMode.COLOR -> this.sortByDescending { it.colorIndex }
                LibraryItemSortMode.CUSTOM -> {} // TODO
            }
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

fun List<LibraryFolder>.sorted(
    mode: LibraryFolderSortMode,
    direction: SortDirection,
) = when (direction) {
    SortDirection.ASCENDING -> {
        when (mode) {
            LibraryFolderSortMode.DATE_ADDED -> this.sortedBy { it.createdAt }
            LibraryFolderSortMode.LAST_MODIFIED -> this.sortedBy { it.modifiedAt }
            LibraryFolderSortMode.NAME -> this.sortedBy { it.name }
            LibraryFolderSortMode.CUSTOM -> this // TODO
        }
    }
    SortDirection.DESCENDING -> {
        when (mode) {
            LibraryFolderSortMode.DATE_ADDED -> this.sortedByDescending { it.createdAt }
            LibraryFolderSortMode.LAST_MODIFIED -> this.sortedByDescending { it.modifiedAt }
            LibraryFolderSortMode.NAME -> this.sortedByDescending { it.name }
            LibraryFolderSortMode.CUSTOM -> this // TODO
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

    // Show paused goals
    val showPausedGoals: Boolean,
)