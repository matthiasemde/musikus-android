/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package app.musikus.core.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import app.musikus.core.domain.TimeProvider
import app.musikus.goals.data.daos.GoalDescription
import app.musikus.goals.data.daos.GoalInstance
import app.musikus.goals.data.entities.GoalDescriptionLibraryItemCrossRefModel
import app.musikus.goals.data.entities.GoalDescriptionModel
import app.musikus.goals.data.entities.GoalInstanceModel
import app.musikus.library.data.daos.LibraryFolder
import app.musikus.library.data.daos.LibraryItem
import app.musikus.library.data.entities.LibraryItemModel
import app.musikus.sessions.data.daos.Section
import app.musikus.sessions.data.daos.Session
import app.musikus.sessions.data.entities.SectionModel

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
) {
    val startTimestamp by lazy {
        sections.minOf { it.section.startTimestamp }
    }
}

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
) {
    val title by lazy { description.title(libraryItems.firstOrNull()) }

    fun subtitle(instance: GoalInstance) = description.subtitle(instance)

    fun endOfInstanceInLocalTimezone(instance: GoalInstance, timeProvider: TimeProvider) =
        description.endOfInstanceInLocalTimezone(instance, timeProvider)
}

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
) {
    fun endOfInstanceInLocalTimezone(timeProvider: TimeProvider) =
        description.endOfInstanceInLocalTimezone(instance, timeProvider)
}

data class GoalDescriptionWithInstancesAndLibraryItems(
    @Embedded val description: GoalDescription,
    @Relation(
        entity = GoalInstanceModel::class,
        parentColumn = "id",
        entityColumn = "goal_description_id"
    )
    val instances: List<GoalInstance>,
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
) {
    val title by lazy { description.title(libraryItems.firstOrNull()) }

    val latestInstance by lazy {
        instances.singleOrNull { it.endTimestamp == null }
            ?: instances.maxWith(compareBy { it.startTimestamp })
    }

    val subtitle by lazy { latestInstance.let { description.subtitle(it) } }

    val startTime by lazy { instances.minOf { it.startTimestamp } }

    fun endTime(timeProvider: TimeProvider) = instances.maxOf {
        description.endOfInstanceInLocalTimezone(it, timeProvider)
    }

    val goalDescriptionWithLibraryItems by lazy {
        GoalDescriptionWithLibraryItems(
            description = description,
            libraryItems = libraryItems
        )
    }
}

data class GoalInstanceWithDescriptionWithLibraryItems(
    @Embedded val instance: GoalInstance,
    @Relation(
        entity = GoalDescriptionModel::class,
        parentColumn = "goal_description_id",
        entityColumn = "id"
    )
    val description: GoalDescriptionWithLibraryItems
) {

    val title
        get() = description.title

    val subtitle by lazy { description.description.subtitle(instance) }

    fun endTimestampInLocalTimezone(timeProvider: TimeProvider) =
        description.endOfInstanceInLocalTimezone(instance, timeProvider)
}
