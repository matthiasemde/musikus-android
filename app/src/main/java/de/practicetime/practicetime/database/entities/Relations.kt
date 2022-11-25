/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package de.practicetime.practicetime.database.entities

import androidx.room.Relation
import androidx.room.Embedded
import androidx.room.Junction


data class SessionWithSections(
    @Embedded val session: Session,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val sections: List<Section>
)

data class SectionWithCategory(
    @Embedded val section: Section,
    @Relation(
        parentColumn = "category_id",
        entityColumn = "id"
    )
    val category: Category
)

data class SessionWithSectionsWithCategories(
    @Embedded val session: Session,
    @Relation(
        entity = Section::class,
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val sections: List<SectionWithCategory>
)

data class GoalDescriptionWithCategories(
    @Embedded val description: GoalDescription,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            GoalDescriptionCategoryCrossRef::class,
            parentColumn = "goal_description_id",
            entityColumn = "category_id"
        )
    )
    val categories: List<Category>
)

data class CategoryWithGoalDescriptions(
    @Embedded val category: Category,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            GoalDescriptionCategoryCrossRef::class,
            parentColumn = "category_id",
            entityColumn = "goal_description_id"
        )
    )
    val descriptions: List<GoalDescription>
)

data class CategoryWithGoalDescriptionsWithCategories(
    @Embedded val category: Category,
    @Relation(
        entity = GoalDescription::class,
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            GoalDescriptionCategoryCrossRef::class,
            parentColumn = "category_id",
            entityColumn = "goal_description_id"
        )
    )
    val descriptions: List<GoalDescriptionWithCategories>
)

data class SectionWithCategoryWithGoalDescriptions(
    @Embedded val section: Section,
    @Relation(
        entity = Category::class,
        parentColumn = "category_id",
        entityColumn = "id"
    )
    val category: CategoryWithGoalDescriptions
)

data class SessionWithSectionsWithCategoriesWithGoalDescriptions(
    @Embedded val session: Session,
    @Relation(
        entity = Section::class,
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val sections: List<SectionWithCategoryWithGoalDescriptions>
)

data class GoalInstanceWithDescription(
    @Embedded val instance: GoalInstance,
    @Relation(
        parentColumn = "goal_description_id",
        entityColumn = "id"
    )
    val description: GoalDescription
)

//data class GoalDescriptionWithInstances(
//    @Embedded val description: GoalDescription,
//    @Relation(
//        parentColumn = "id",
//        entityColumn = "goalDescriptionId"
//    )
//    val instances: List<GoalInstance>
//)

data class GoalInstanceWithDescriptionWithCategories(
    @Embedded val instance: GoalInstance,
    @Relation(
        entity = GoalDescription::class,
        parentColumn = "goal_description_id",
        entityColumn = "id"
    )
    val description: GoalDescriptionWithCategories
)
