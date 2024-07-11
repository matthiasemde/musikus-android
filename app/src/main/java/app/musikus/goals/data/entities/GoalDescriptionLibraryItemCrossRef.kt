/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.goals.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import app.musikus.library.data.entities.LibraryItemModel
import java.util.UUID

@Entity(
    tableName = "goal_description_library_item_cross_ref",
    primaryKeys = ["goal_description_id", "library_item_id"],
    foreignKeys = [
        ForeignKey(
            entity = GoalDescriptionModel::class,
            parentColumns = ["id"],
            childColumns = ["goal_description_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LibraryItemModel::class,
            parentColumns = ["id"],
            childColumns = ["library_item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GoalDescriptionLibraryItemCrossRefModel (
    @ColumnInfo(name = "goal_description_id", index = true) val goalDescriptionId: UUID,
    @ColumnInfo(name = "library_item_id", index = true) val libraryItemId: UUID,
)