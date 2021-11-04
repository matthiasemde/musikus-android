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
