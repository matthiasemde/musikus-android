/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.utils

import app.musikus.database.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.LibraryFolderWithItems
import app.musikus.database.daos.GoalDescription
import app.musikus.database.daos.GoalInstance
import app.musikus.database.daos.LibraryFolder
import app.musikus.database.daos.LibraryItem

data class SortInfo<T>(
    val mode: SortMode<T>,
    val direction: SortDirection
)

enum class SortDirection {
    ASCENDING,
    DESCENDING;

    fun invert() = when (this) {
        ASCENDING -> DESCENDING
        DESCENDING -> ASCENDING
    }

    companion object {
        val DEFAULT = DESCENDING

        fun valueOrDefault(string: String?) = try {
            valueOf(string ?: "")
        } catch (e: Exception) {
            DEFAULT
        }
    }
}

interface SortMode<T> {
    val label: String
    val comparator: Comparator<T>
    val name: String

    val isDefault: Boolean
}

typealias GoalSortInfo = SortInfo<Pair<GoalDescription, GoalInstance>>
enum class GoalsSortMode : SortMode<Pair<GoalDescription, GoalInstance>> {
    DATE_ADDED {
        override val label = "Date added"
        override val comparator = compareBy<Pair<GoalDescription, GoalInstance>> { (description, _) ->
            description.createdAt
        }
    },
    TARGET {
        override val label = "Target"
        override val comparator = compareBy<Pair<GoalDescription, GoalInstance>> { (_, instance) ->
            instance.target
        }
    },
    PERIOD {
        override val label = "Period"
        override val comparator = compareBy<Pair<GoalDescription, GoalInstance>> { (description, _) ->
            description.periodUnit
        }.thenBy { (description, _) ->
            description.periodInPeriodUnits
        }
    },
//    CUSTOM {
//        override val label = "Custom"
//        override val comparator = compareBy<Pair<GoalDescription, GoalInstance>> { TODO }
//    }
    ;

    override val isDefault: Boolean
        get() = this == DEFAULT

    companion object {
        val DEFAULT = DATE_ADDED

        fun valueOrDefault(string: String?) = try {
            valueOf(string ?: "")
        } catch (e: Exception) {
            DEFAULT
        }
    }
}

@JvmName("sortedGoalInstanceWithDescriptionWithLibraryItems")
fun List<GoalInstanceWithDescriptionWithLibraryItems>.sorted(
    mode: GoalsSortMode,
    direction: SortDirection
) : List<GoalInstanceWithDescriptionWithLibraryItems> = this.sortedWith(
    when(direction) {
        SortDirection.ASCENDING ->
            compareBy (mode.comparator) { it.description.description to it.instance}
        SortDirection.DESCENDING ->
            compareByDescending(mode.comparator) { it.description.description to it.instance }
    }
)

@JvmName("sortedGoalDescriptionWithInstancesAndLibraryItems")
fun List<GoalDescriptionWithInstancesAndLibraryItems>.sorted(
    mode: GoalsSortMode,
    direction: SortDirection
) : List<GoalDescriptionWithInstancesAndLibraryItems> = this.sortedWith(
    when(direction) {
        SortDirection.ASCENDING ->
            compareBy (mode.comparator) { it.description to it.instances.last() }
        SortDirection.DESCENDING ->
            compareByDescending(mode.comparator) { it.description to it.instances.last() }
    }
)

enum class LibraryItemSortMode : SortMode<LibraryItem> {
    DATE_ADDED {
        override val label = "Date added"
        override val comparator = compareBy<LibraryItem> { it.createdAt }
    },
    LAST_MODIFIED {
        override val label = "Last modified"
        override val comparator = compareBy<LibraryItem> { it.modifiedAt }
    },
    NAME {
        override val label = "Name"
        override val comparator = compareBy<LibraryItem> { it.name }
    },
    COLOR {
        override val label = "Color"
        override val comparator = compareBy<LibraryItem> { it.colorIndex }
    },
//    CUSTOM {
//        override val label = "Custom"
//        override val comparator = compareBy<LibraryItem> { TODO }
//    }
    ;

    override val isDefault: Boolean
        get() = this == DEFAULT

    companion object {
        val DEFAULT = DATE_ADDED

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
) = this.sortedWith (
    when(direction) {
        SortDirection.ASCENDING ->
            compareBy (mode.comparator) { it }
        SortDirection.DESCENDING ->
            compareByDescending(mode.comparator) { it }
    }
)

enum class LibraryFolderSortMode : SortMode<LibraryFolder> {
    DATE_ADDED {
        override val label = "Date added"
        override val comparator = compareBy<LibraryFolder> { it.createdAt }
    },
    LAST_MODIFIED {
        override val label = "Last modified"
        override val comparator = compareBy<LibraryFolder> { it.modifiedAt }
    },
    NAME {
        override val label = "Name"
        override val comparator = compareBy<LibraryFolder> { it.name }
    },
//    CUSTOM {
//        override val label = "Custom"
//        override val comparator = compareBy<LibraryFolder> { TODO }
//    }
    ;

    override val isDefault: Boolean
        get() = this == DEFAULT

    companion object {
        val DEFAULT = DATE_ADDED

        fun valueOrDefault(string: String?) = try {
            valueOf(string ?: "")
        } catch (e: Exception) {
            DEFAULT
        }
    }
}

fun List<LibraryFolderWithItems>.sorted(
    mode: LibraryFolderSortMode,
    direction: SortDirection,
) = this.sortedWith(
    when (direction) {
        SortDirection.ASCENDING ->
            compareBy(mode.comparator) { it.folder }
        SortDirection.DESCENDING ->
            compareByDescending(mode.comparator) { it.folder }
    }
)