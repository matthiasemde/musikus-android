/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde
 */

package app.musikus.core.domain

import app.musikus.core.presentation.utils.UiText

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
    val label: UiText
    val comparator: Comparator<T>
    val name: String

    val isDefault: Boolean
}

