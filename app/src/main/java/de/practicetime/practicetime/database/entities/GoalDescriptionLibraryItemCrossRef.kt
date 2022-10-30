/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "goal_description_library_item_cross_ref",
    primaryKeys = ["goal_description_id", "library_item_id"]
)
data class GoalDescriptionLibraryItemCrossRef (
    @ColumnInfo(name = "goal_description_id", index = true) val goalDescriptionId: Long,
    @ColumnInfo(name = "library_item_id", index = true) val libraryItemId: Long,
)
