package de.practicetime.practicetime.entities

// import androidx.room.Entity
import androidx.room.Relation
import androidx.room.Embedded
import androidx.room.Junction


data class SessionWithSections(
    @Embedded val session: PracticeSession,
    @Relation(
        parentColumn = "id",
        entityColumn = "practice_session_id"
    )
    val sections: List<PracticeSection>
)

data class SectionWithCategory(
    @Embedded val section: PracticeSection,
    @Relation(
        parentColumn = "category_id",
        entityColumn = "id"
    )
    val category: Category
)

data class SessionWithSectionsWithCategories(
    @Embedded val session: PracticeSession,
    @Relation(
        entity = PracticeSection::class,
        parentColumn = "id",
        entityColumn = "practice_session_id"
    )
    val sections: List<SectionWithCategory>
)

data class GoalWithCategories(
    @Embedded val goal: Goal,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            GoalCategoryCrossRef::class,
            parentColumn = "goalId",
            entityColumn = "categoryId"
        )
    )
    val categories: List<Category>
)

data class CategoryWithGoals(
    @Embedded val category: Category,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            GoalCategoryCrossRef::class,
            parentColumn = "categoryId",
            entityColumn = "goalId"
        )
    )
    val goals: List<Goal>
)
