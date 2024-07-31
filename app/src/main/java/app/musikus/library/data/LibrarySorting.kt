/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.library.data

import app.musikus.R
import app.musikus.core.data.LibraryFolderWithItems
import app.musikus.core.domain.SortDirection
import app.musikus.core.domain.SortMode
import app.musikus.core.presentation.utils.UiText
import app.musikus.library.data.daos.LibraryFolder
import app.musikus.library.data.daos.LibraryItem


enum class LibraryItemSortMode : SortMode<LibraryItem> {
    DATE_ADDED {
        override val label = UiText.StringResource(R.string.library_item_sort_mode_date_added)
        override val comparator = compareBy<LibraryItem> { it.createdAt }
    },
    LAST_MODIFIED {
        override val label = UiText.StringResource(R.string.library_item_sort_mode_last_modified)
        override val comparator = compareBy<LibraryItem> { it.modifiedAt }
    },
    NAME {
        override val label = UiText.StringResource(R.string.library_item_sort_mode_name)
        override val comparator = compareBy<LibraryItem> { it.name }
    },
    COLOR {
        override val label = UiText.StringResource(R.string.library_item_sort_mode_color)
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
        override val label = UiText.StringResource(R.string.library_folder_sort_mode_date_added)
        override val comparator = compareBy<LibraryFolder> { it.createdAt }
    },
    LAST_MODIFIED {
        override val label = UiText.StringResource(R.string.library_folder_sort_mode_last_modified)
        override val comparator = compareBy<LibraryFolder> { it.modifiedAt }
    },
    NAME {
        override val label = UiText.StringResource(R.string.library_folder_sort_mode_name)
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