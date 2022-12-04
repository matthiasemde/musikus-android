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

package de.practicetime.practicetime.database

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import de.practicetime.practicetime.database.entities.*


data class SessionWithSections(
    @Embedded val session: Session,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val sections: List<Section>
)

data class SectionWithLibraryItem(
    @Embedded val section: Section,
    @Relation(
        parentColumn = "library_item_id",
        entityColumn = "id"
    )
    val libraryItem: LibraryItem
)

data class SessionWithSectionsWithLibraryItems(
    @Embedded val session: Session,
    @Relation(
        entity = Section::class,
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val sections: List<SectionWithLibraryItem>
)

data class GoalDescriptionWithLibraryItems(
    @Embedded val description: GoalDescription,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            GoalDescriptionLibraryItemCrossRef::class,
            parentColumn = "goal_description_id",
            entityColumn = "library_item_id"
        )
    )
    val libraryItems: List<LibraryItem>
)

data class LibraryFolderWithItems(
    @Embedded val folder: LibraryFolder,
    @Relation(
        parentColumn = "id",
        entityColumn = "library_folder_id"
    )
    val items: List<LibraryItem>
)

data class LibraryItemWithGoalDescriptions(
    @Embedded val libraryItem: LibraryItem,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            GoalDescriptionLibraryItemCrossRef::class,
            parentColumn = "library_item_id",
            entityColumn = "goal_description_id"
        )
    )
    val descriptions: List<GoalDescription>
)

data class LibraryItemWithGoalDescriptionsWithLibraryItems(
    @Embedded val libraryItem: LibraryItem,
    @Relation(
        entity = GoalDescription::class,
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            GoalDescriptionLibraryItemCrossRef::class,
            parentColumn = "library_item_id",
            entityColumn = "goal_description_id"
        )
    )
    val descriptions: List<GoalDescriptionWithLibraryItems>
)

data class SectionWithLibraryItemWithGoalDescriptions(
    @Embedded val section: Section,
    @Relation(
        entity = LibraryItem::class,
        parentColumn = "library_item_id",
        entityColumn = "id"
    )
    val libraryItem: LibraryItemWithGoalDescriptions
)

data class SessionWithSectionsWithLibraryItemsWithGoalDescriptions(
    @Embedded val session: Session,
    @Relation(
        entity = Section::class,
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val sections: List<SectionWithLibraryItemWithGoalDescriptions>
)

data class GoalInstanceWithDescription(
    @Embedded val instance: GoalInstance,
    @Relation(
        parentColumn = "goal_description_id",
        entityColumn = "id"
    )
    val description: GoalDescription
)

data class GoalDescriptionWithInstances(
    @Embedded val description: GoalDescription,
    @Relation(
        parentColumn = "id",
        entityColumn = "goalDescriptionId"
    )
    val instances: List<GoalInstance>
)

data class GoalInstanceWithDescriptionWithLibraryItems(
    @Embedded val instance: GoalInstance,
    @Relation(
        entity = GoalDescription::class,
        parentColumn = "goal_description_id",
        entityColumn = "id"
    )
    val description: GoalDescriptionWithLibraryItems
)
