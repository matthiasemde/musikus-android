/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package app.musikus.database

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import app.musikus.database.daos.GoalDescription
import app.musikus.database.daos.GoalInstance
import app.musikus.database.daos.LibraryFolder
import app.musikus.database.daos.LibraryItem
import app.musikus.database.daos.Section
import app.musikus.database.daos.Session
import app.musikus.database.entities.GoalDescriptionLibraryItemCrossRefModel
import app.musikus.database.entities.GoalDescriptionModel
import app.musikus.database.entities.GoalInstanceModel
import app.musikus.database.entities.LibraryItemModel
import app.musikus.database.entities.SectionModel


data class SessionWithSections(
    @Embedded val session: Session,
    @Relation(
        entity = SectionModel::class,
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val sections: List<Section>
)

data class SectionWithLibraryItem(
    @Embedded val section: Section,
    @Relation(
        entity = LibraryItemModel::class,
        parentColumn = "library_item_id",
        entityColumn = "id"
    )
    val libraryItem: LibraryItem
)

data class SessionWithSectionsWithLibraryItems(
    @Embedded val session: Session,
    @Relation(
        entity = SectionModel::class,
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val sections: List<SectionWithLibraryItem>
)

data class GoalDescriptionWithLibraryItems(
    @Embedded val description: GoalDescription,
    @Relation(
        entity = LibraryItemModel::class,
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            GoalDescriptionLibraryItemCrossRefModel::class,
            parentColumn = "goal_description_id",
            entityColumn = "library_item_id"
        )
    )
    val libraryItems: List<LibraryItem>
)

data class LibraryFolderWithItems(
    @Embedded val folder: LibraryFolder,
    @Relation(
        entity = LibraryItemModel::class,
        parentColumn = "id",
        entityColumn = "library_folder_id"
    )
    val items: List<LibraryItem>
)

data class LibraryItemWithGoalDescriptions(
    @Embedded val libraryItem: LibraryItem,
    @Relation(
        entity = GoalDescriptionModel::class,
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            GoalDescriptionLibraryItemCrossRefModel::class,
            parentColumn = "library_item_id",
            entityColumn = "goal_description_id"
        )
    )
    val descriptions: List<GoalDescription>
)

data class LibraryItemWithGoalDescriptionsWithLibraryItems(
    @Embedded val libraryItem: LibraryItem,
    @Relation(
        entity = GoalDescriptionModel::class,
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            GoalDescriptionLibraryItemCrossRefModel::class,
            parentColumn = "library_item_id",
            entityColumn = "goal_description_id"
        )
    )
    val descriptions: List<GoalDescriptionWithLibraryItems>
)

data class SectionWithLibraryItemWithGoalDescriptions(
    @Embedded val section: Section,
    @Relation(
        entity = LibraryItemModel::class,
        parentColumn = "library_item_id",
        entityColumn = "id"
    )
    val libraryItem: LibraryItemWithGoalDescriptions
)

data class SessionWithSectionsWithLibraryItemsWithGoalDescriptions(
    @Embedded val session: Session,
    @Relation(
        entity = SectionModel::class,
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val sections: List<SectionWithLibraryItemWithGoalDescriptions>
)

data class GoalInstanceWithDescription(
    @Embedded val instance: GoalInstance,
    @Relation(
        entity = GoalDescriptionModel::class,
        parentColumn = "goal_description_id",
        entityColumn = "id"
    )
    val description: GoalDescription
)

data class GoalDescriptionWithInstances(
    @Embedded val description: GoalDescription,
    @Relation(
        entity = GoalInstanceModel::class,
        parentColumn = "id",
        entityColumn = "goal_description_id"
    )
    val instances: List<GoalInstance>
)

data class GoalInstanceWithDescriptionWithLibraryItems(
    @Embedded val instance: GoalInstance,
    @Relation(
        entity = GoalDescriptionModel::class,
        parentColumn = "goal_description_id",
        entityColumn = "id"
    )
    val description: GoalDescriptionWithLibraryItems
)
