package de.practicetime.practicetime.entities

// import androidx.room.Entity
import androidx.room.Relation
import androidx.room.Embedded


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
